package top.easelink.lcg.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.ui.main.model.SessionExpiredEvent
import top.easelink.lcg.ui.main.source.checkMessages
import top.easelink.lcg.ui.main.source.extractFormHash
import top.easelink.lcg.utils.WebsiteConstant
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.getCookiesFor
import top.easelink.lcg.utils.getDeviceUserAgent
import top.easelink.lcg.utils.updateCookies
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

object JsoupClient : ApiRequest {
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val FOLLOW_REDIRECTS_ENABLE = true

    var formHash: String? = null
        set(value) {
            Timber.d("formHash = $value")
            field = value
        }

    private var lastTime = 0L
    private val CHECK_INTERVAL = if (BuildConfig.DEBUG) 60 * 1000 else 30 * 1000

    // 5 秒以上的页面在国内很常见，把整体调度上限提到 30s，配合 OkApiClient 一致。
    private const val TIME_OUT_LIMIT = 30 * 1000
    private const val BASE_URL = SERVER_BASE_URL
    private val USER_AGENT = getDeviceUserAgent(LCGApp.context)

    /**
     * 避免短时间内重复抛出 SessionExpiredEvent，让 UI 只弹一次"登录失效"。
     * 在 clearAll 时复位。
     */
    private val sessionExpiredFiredOnce = AtomicBoolean(false)

    @Throws(SocketTimeoutException::class, IOException::class)
    override fun sendGetRequestWithQuery(query: String): Document {
        val url = "$BASE_URL$query"
        return getConnection(url)
            .method(Connection.Method.GET)
            .followRedirects(FOLLOW_REDIRECTS_ENABLE)
            .execute()
            .let {
                updateCookies(it.cookies())
                it.parse().also { doc ->
                    checkResponse(doc)
                }
            }
    }

    fun sendAjaxRequest(query: String): String {
        val url = "$BASE_URL$query"
        return getConnection(url)
            .method(Connection.Method.GET)
            .followRedirects(false)
            .execute()
            .let {
                updateCookies(it.cookies())
                it.body()
            }
    }

    override fun sendGetRequestWithUrl(url: String): Document {
        return getConnection(url)
            .method(Connection.Method.GET)
            .followRedirects(FOLLOW_REDIRECTS_ENABLE)
            .execute()
            .let {
                updateCookies(it.cookies())
                it.parse().also { doc ->
                    checkResponse(doc)
                }
            }
    }

    override fun sendPostRequestWithUrl(
        url: String,
        form: MutableMap<String, String>?
    ): Connection.Response {
        return getConnection(url)
            .apply {
                if (form != null) {
                    data(form)
                }
            }
            .postDataCharset("gbk")
            .method(Connection.Method.POST)
            .execute()
            .also {
                updateCookies(it.cookies())
            }
    }

    /** 登出后由外部调用，让下次再次检测到 401 时仍能弹一次提示。 */
    fun resetSessionExpiredFlag() {
        sessionExpiredFiredOnce.set(false)
    }

    private fun getConnection(url: String): Connection {
        return Jsoup.connect(url)
            .timeout(TIME_OUT_LIMIT)
            .ignoreHttpErrors(true)
            .cookies(getCookiesFor(url))
            .userAgent(USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("Cache-Control", "max-age=0")
            .header("Connection", "keep-alive")
            .header("Host", WebsiteConstant.SERVER_HOST)
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("sec-ch-ua-mobile", "?1")
            .header("sec-ch-ua-platform", "Android")
    }

    private fun checkResponse(doc: Document) {
        clientScope.launch {
            if (formHash.isNullOrEmpty()) {
                formHash = extractFormHash(doc)
            }
            if (System.currentTimeMillis() - lastTime > CHECK_INTERVAL) {
                lastTime = System.currentTimeMillis()
                try {
                    checkLoginState(doc)
                    checkMessages(doc)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    /**
     * 探测响应里是否携带 Discuz 的"会话失效"特征。
     * 命中且本地仍持有"已登录"状态 -> 视为会话失效，清账号并通知 UI。
     *
     * 检测策略基于实地抓取 52pojie 真实响应（已登录帖子页 vs 未登录访问签到页）：
     *
     * 强信号 #1：<div id="messagelogin"> 元素存在
     *   这个元素只在 Discuz 的"需要登录才能继续此操作"提示页里出现，正常浏览页绝不会有。
     *   是最干净的判定依据。
     *
     * 强信号 #2：<div id="messagetext"> 文本含 "您需要先登录才能继续"
     *   Discuz 标准未登录提示文案，已在 52pojie 实际响应里验证过。
     *
     * 命中任一即清登录态。曾用过的"form[name=login] 存在"信号被舍弃 ——
     * 论坛侧边栏经常自带快速登录框，已登录用户也可能看到，会误报。
     *
     * 触发时打 Timber.w 记录上下文，方便用户在 logcat 里排查"莫名掉登录"。
     */
    private fun checkLoginState(doc: Document) {
        if (!UserDataRepo.isLoggedIn) return

        val hasMessageLogin = doc.getElementById("messagelogin") != null
        val messageText = doc.getElementById("messagetext")?.text().orEmpty()
        val explicitExpired = messageText.contains(SESSION_EXPIRED_MARKER)

        if (!hasMessageLogin && !explicitExpired) {
            // 与会话无关 / 健康响应 → 重置 one-shot，下次再失效仍能弹一次。
            sessionExpiredFiredOnce.compareAndSet(true, false)
            return
        }

        Timber.w(
            "session expired detected: hasMessageLogin=%s explicitExpired=%s messageText='%s'",
            hasMessageLogin,
            explicitExpired,
            messageText.take(120)
        )
        if (sessionExpiredFiredOnce.compareAndSet(false, true)) {
            UserDataRepo.clearAll()
            EventBus.getDefault().post(SessionExpiredEvent())
        }
    }

    private const val SESSION_EXPIRED_MARKER = "您需要先登录才能继续"
}
