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
import top.easelink.lcg.account.UserDataRepo.updateUserInfo
import top.easelink.lcg.appinit.LCGApp
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
import top.easelink.lcg.utils.updateCookies

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
            if (html.isNullOrBlank()) return
            coroutineScope.launch(CalcPool) {
                val doc = Jsoup.parse(html)
                doc.selectFirst("div.avt") ?: return@launch
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
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
            CookieManager.getInstance().getCookie(url)?.let {
                updateCookies(it, isOpenLoginEvent)
            }
            if (isOpenLoginEvent) {
                view.loadUrl("javascript:$HOOK_NAME.processHtml(document.documentElement.outerHTML);")
            }
            view.settings.blockNetworkImage = false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            view.settings.blockNetworkImage = true
            setLoading(true)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            // 考虑显示警告对话框后再决定是否继续
            handler.cancel() // 改为 cancel() 提高安全性
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            request.url.toString().let { url ->
                when {
                    url.startsWith("wtloginmqq://ptlogin/qlogin") -> runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                    url.startsWith("bdnetdisk") -> showMessage(R.string.baidu_net_disk_not_support)
                }
            }
            return false
        }
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