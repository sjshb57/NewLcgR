package top.easelink.lcg.ui.webview.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.net.http.SslError
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import timber.log.Timber
import top.easelink.framework.customview.webview.HorizontalScrollDisableWebView
import top.easelink.framework.threadpool.CalcPool
import top.easelink.framework.threadpool.IOPool
import top.easelink.framework.threadpool.Main
import top.easelink.lcg.R
import top.easelink.lcg.account.AccountManager.isLoggedIn
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.account.UserDataRepo.updateUserInfo
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.network.LCGCookieJar
import top.easelink.lcg.service.web.HookInterface
import top.easelink.lcg.ui.main.me.source.UserInfoRepo.requestUserInfo
import top.easelink.lcg.ui.main.MainActivity
import top.easelink.lcg.ui.webview.FORCE_ENABLE_JS_KEY
import top.easelink.lcg.ui.webview.OPEN_LOGIN_PAGE
import top.easelink.lcg.ui.webview.TITLE_KEY
import top.easelink.lcg.utils.WebsiteConstant.EXTRA_TABLE_HTML
import top.easelink.lcg.utils.WebsiteConstant.LOGIN_QUERY
import top.easelink.lcg.utils.WebsiteConstant.QQ_LOGIN_URL
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.WebsiteConstant.URL_KEY
import top.easelink.lcg.utils.showMessage
import top.easelink.lcg.utils.setStatusBarPadding

class WebViewActivity : AppCompatActivity() {

    private lateinit var mWebView: WebView
    private lateinit var animationView: LottieAnimationView
    private lateinit var videoLayout: FrameLayout

    private var mUrl: String? = null
    private var mHtml: String? = null
    private var mForceEnableJs = true
    private var isOpenLoginEvent = false
    private val coroutineScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, e ->
        Timber.e(e)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
        initContentView()
        initActionBar()
        initWebView()
        initBackPressHandler()
    }

    private fun initBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mWebView.canGoBack()) {
                    mWebView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.toString()?.let { url ->
                mWebView.loadUrl(url)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        (mWebView.parent as? ViewGroup)?.removeView(mWebView)
        mWebView.destroy()
    }

    private fun initContentView() {
        setContentView(R.layout.activity_web_view)
        mWebView = findViewById(R.id.web_view)
        animationView = findViewById(R.id.searching_file)
        videoLayout = findViewById(R.id.container)
    }

    private fun openInSystemBrowser(url: String) {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(this)
        }
    }

    private fun initWebView() {
        mWebView.webViewClient = webViewClient
        mWebView.webChromeClient = InnerChromeClient()
        mWebView.setDownloadListener { url, _, _, _, _ ->
            openInSystemBrowser(url)
        }

        // CookieManager 默认接受 first-party cookie，但 third-party 默认是 false。
        // QQ 登录会跨域跳转到 connect.qq.com，需要 third-party cookie 才能完成；
        // WAF cookie wzws_cid 是 first-party HttpOnly，依赖 acceptCookie=true。
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(mWebView, true)
        }

        isOpenLoginEvent = intent.getBooleanExtra(OPEN_LOGIN_PAGE, false)
        mForceEnableJs = intent.getBooleanExtra(FORCE_ENABLE_JS_KEY, false)

        when {
            !mUrl.isNullOrEmpty() -> {
                updateWebViewSettingsRemote()
                if (isOpenLoginEvent) {
                    mWebView.removeJavascriptInterface(HOOK_NAME)
                    mWebView.addJavascriptInterface(WebViewHook(), HOOK_NAME)
                }
                mWebView.loadUrl(mUrl!!)
            }
            !mHtml.isNullOrEmpty() -> {
                updateWebViewSettingsLocal()
                mWebView.loadDataWithBaseURL("", mHtml!!, "text/html", "UTF-8", "")
            }
        }
    }

    inner class WebViewHook : HookInterface {
        @JavascriptInterface
        override fun processHtml(html: String?) {
            if (html.isNullOrBlank()) {
                Timber.d("processHtml: html is null/blank, skip")
                return
            }
            coroutineScope.launch(CalcPool) {
                val doc = Jsoup.parse(html)
                val currentUrl = withContext(Main) { mWebView.url } ?: return@launch
                val success = isLoginSuccess(doc, currentUrl)
                Timber.d("processHtml: url=%s isLoginSuccess=%s", currentUrl, success)
                if (!success) return@launch
                // 把刚登录拿到的 cookie 灌入统一 jar，OkHttp/Jsoup/Coil 立刻共享同一个会话。
                LCGCookieJar.syncFromWebView(currentUrl)
                UserDataRepo.isLoggedIn = true
                isLoggedIn.postValue(true)
                doc.getElementById("messagetext")?.text()?.let {
                    showMessage(it)
                } ?: showMessage(R.string.login_successfully)
                delay(1000)
                withContext(IOPool) {
                    requestUserInfo()?.let(::updateUserInfo)
                }
                withContext(Main) {
                    startActivity(Intent(mWebView.context, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    /**
     * Discuz 论坛登录成功的标志：页面里出现用户头像 div.avt。
     *
     * 之前曾尝试加更严格的"不含 #messagelogin / 不含'您需要先登录'文案"双重校验，
     * 但 52pojie 登录后的首页可能在其它模板片段（侧边栏快速登录、未读弹窗、片段
     * 引用等）里包含同名元素，导致 isLoginSuccess 永远 false → 用户登录后死循环
     * 在 WebView 里。回归原作者只看 div.avt 的简单规则。
     */
    private fun isLoginSuccess(doc: org.jsoup.nodes.Document, url: String): Boolean {
        val hasAvatar = doc.selectFirst("div.avt") != null
        Timber.d("isLoginSuccess: url=%s hasAvatar=%s", url, hasAvatar)
        return hasAvatar
    }

    private fun initData() {
        intent.data?.let { uri ->
            if (uri.scheme == "lcg") {
                mUrl = uri.toString().replace("lcg:", SERVER_BASE_URL)
                return
            }
            mUrl = uri.toString()
        }
        mUrl = mUrl ?: intent.getStringExtra(URL_KEY)
        mHtml = intent.getStringExtra(EXTRA_TABLE_HTML)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        (item.icon as? Animatable)?.start()
        return when (item.itemId) {
            R.id.action_share -> {
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
                true
            }
            R.id.action_open_in_webview -> {
                mWebView.url?.let(::openInSystemBrowser) ?: showMessage(R.string.general_error)
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val shareIntent: Intent
        get() = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_template, mWebView.title, mWebView.url))
            putExtra(Intent.EXTRA_SUBJECT, mWebView.title)
        }

    private fun initActionBar() {
        findViewById<Toolbar>(R.id.web_view_toolbar)?.let { toolbar ->
            toolbar.setStatusBarPadding()
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
                setDisplayShowHomeEnabled(true)
                intent.getIntExtra(TITLE_KEY, 0).takeIf { it != 0 }?.let(::setTitle)
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            else -> {
                // 处理其他方向情况
            }
        }
    }

    private val webViewClient: WebViewClient
        get() = InnerWebViewClient()

    private fun setLoading(isLoading: Boolean) {
        animationView.visibility = if (isLoading) View.VISIBLE else View.GONE
        mWebView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun updateWebViewSettingsLocal() {
        mWebView.settings.apply {
            if (mWebView is HorizontalScrollDisableWebView) {
                (mWebView as HorizontalScrollDisableWebView).setScrollEnable(true)
            }
            javaScriptEnabled = mForceEnableJs
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            defaultTextEncodingName = "UTF-8"
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun updateWebViewSettingsRemote() {
        mWebView.settings.apply {
            javaScriptEnabled = mForceEnableJs
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            defaultTextEncodingName = "UTF-8"
            builtInZoomControls = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            blockNetworkImage = true
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private inner class InnerChromeClient : WebChromeClient() {
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mCustomView: View? = null

        // 把 WebView 内 JS 的 console 输出接到 logcat。WAF 滑块页跑重度混淆 JS，
        // 出错时如果不接 console，整个页面就停在"正在加载中..."而没有任何线索。
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Timber.tag("WebViewJS").d(
                "[%s] %s @ %s:%d",
                consoleMessage.messageLevel(),
                consoleMessage.message(),
                consoleMessage.sourceId(),
                consoleMessage.lineNumber()
            )
            return true
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (mCustomView != null) {
                callback.onCustomViewHidden()
                return
            }
            mCustomView = view.apply { visibility = View.VISIBLE }
            mCustomViewCallback = callback
            videoLayout.addView(view)
            videoLayout.bringToFront()

            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        override fun onHideCustomView() {
            mCustomView?.let {
                it.visibility = View.GONE
                videoLayout.removeView(it)
                mCustomView = null
                mCustomViewCallback?.onCustomViewHidden()

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private inner class InnerWebViewClient : WebViewClient() {
        override fun onPageCommitVisible(view: WebView, url: String) {
            setLoading(false)
            view.settings.blockNetworkImage = false
            // 等 WAF 验证码子资源加载后再同步 cookie：commit 瞬间 wzws_cid 可能
            // 还没到 CookieManager（它是通过 /waf_*_captcha 响应的 Set-Cookie 下发的）。
            // 文字版只需要等一张 ~8KB 的 JPEG，300ms 够；滑块版要等 234KB base64 AJAX，
            // 弱网下经常超过 300ms，延长到 2000ms。shouldInterceptRequest 里另有按子资源
            // 粒度的即时同步兜底。
            val delay = if (url.contains("/waf_slider_verify.html")) 2000L else 300L
            view.postDelayed({
                runCatching { android.webkit.CookieManager.getInstance().flush() }
                LCGCookieJar.syncFromWebView(url)
            }, delay)
            if (isOpenLoginEvent) {
                view.loadUrl("javascript:$HOOK_NAME.processHtml(document.documentElement.outerHTML);")
            }
        }

        // 子资源粒度的 cookie 同步：/waf_*_captcha 响应会下发新的 wzws_cid，
        // 必须在它返回后立刻 flush + sync，否则后续 OkHttp 请求带的是过期 cookie。
        // 同时给 WAF/wzws-* 子资源打日志，方便定位"卡在正在加载中"的环节。
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val u = request.url.toString()
            if (u.contains("/waf_") || u.contains("/wzws-")) {
                Timber.tag("WAF").d("→ %s %s", request.method, u)
            }
            if (u.contains("/waf_slider_captcha") || u.contains("/waf_text_captcha")) {
                view.post {
                    runCatching { android.webkit.CookieManager.getInstance().flush() }
                    LCGCookieJar.syncFromWebView(u)
                }
            }
            return null
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            Timber.tag("WAF").w(
                "HTTP %d on %s (%s)",
                errorResponse.statusCode,
                request.url,
                errorResponse.reasonPhrase
            )
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            Timber.tag("WAF").w(
                "err %d %s on %s",
                error.errorCode,
                error.description,
                request.url
            )
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            // 关键修复：WAF 验证页 (/waf_text_verify.html / /waf_slider_verify.html) 必须
            // 立刻加载图片——验证码图 (/waf_text_captcha) 的响应头才是下发 wzws_cid
            // cookie 的地方。普通页面才屏蔽图片以加快首屏。
            val isWaf = isWafChallengeUrl(url)
            view.settings.blockNetworkImage = !isWaf
            // 验证页要让用户看到，不能用 Lottie 占位盖住
            setLoading(!isWaf)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            // 安全策略：任何 SSL 错误一律 cancel，不提供"继续"绕过 UI（MITM 风险）。
            handler.cancel()
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            request.url.toString().let { url ->
                when {
                    url.startsWith("wtloginmqq://ptlogin/qlogin") -> {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                        return true
                    }
                    url.startsWith("bdnetdisk") -> {
                        showMessage(R.string.baidu_net_disk_not_support)
                        return true
                    }
                }
            }
            return false
        }
    }

    private fun isWafChallengeUrl(url: String?): Boolean {
        if (url == null) return false
        return url.contains("/waf_text_verify.html") ||
                url.contains("/waf_slider_verify.html") ||
                url.contains("/waf_text_captcha") ||
                url.contains("/waf_slider_captcha")
    }

    companion object {
        private const val HOOK_NAME = "hook"

        fun startWebViewWith(url: String, context: Context?) {
            Intent(context ?: LCGApp.context, WebViewActivity::class.java).apply {
                putExtra(URL_KEY, url)
                putExtra(FORCE_ENABLE_JS_KEY, true)
            }.also { (context ?: LCGApp.context).startActivity(it) }
        }

        fun openLoginPage(context: Context) {
            Intent(context, WebViewActivity::class.java).apply {
                putExtra(URL_KEY, SERVER_BASE_URL + LOGIN_QUERY)
                putExtra(FORCE_ENABLE_JS_KEY, true)
                putExtra(OPEN_LOGIN_PAGE, true)
            }.also { context.startActivity(it) }
        }

        fun openQQLoginPage(context: Context) {
            Intent(context, WebViewActivity::class.java).apply {
                putExtra(URL_KEY, QQ_LOGIN_URL)
                putExtra(FORCE_ENABLE_JS_KEY, true)
                putExtra(OPEN_LOGIN_PAGE, true)
            }.also { context.startActivity(it) }
        }
    }
}