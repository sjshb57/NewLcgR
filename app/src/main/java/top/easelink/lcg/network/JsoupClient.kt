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
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-")
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
     * 探测响应里是否携带 Discuz 的登录页特征。
     * 命中且本地仍持有"已登录"状态 -> 视为会话失效，清账号并通知 UI。
     *
     * 为何不再"unstable"：原实现注释掉的 checkLoginState 用字符串匹配 userName，
     * 容易随论坛文案变动而误判；这里改用更稳定的"登录表单存在 + 没有 formhash 指向的私有页"组合。
     */
    private fun checkLoginState(doc: Document) {
        if (!UserDataRepo.isLoggedIn) return
        val hasLoginForm = doc.selectFirst("form[name=login], div#loginform, table#loginform") != null
        val hasFormHash = doc.selectFirst("input[name=formhash]") != null
        val messageText = doc.getElementById("messagetext")?.text().orEmpty()
        val mentionsLogin = messageText.contains("您还未登录") ||
                messageText.contains("请先登录") ||
                messageText.contains("还没有登录")

        // 看到"健康"页面（有 formhash、没登录表单、没 messagetext 错误）→ 会话有效，重置 one-shot。
        if (hasFormHash && !hasLoginForm && messageText.isEmpty()) {
            sessionExpiredFiredOnce.compareAndSet(true, false)
            return
        }

        // 命中登录页结构 或 Discuz 提示框文案 → 视为失效。
        if ((hasLoginForm && !hasFormHash) || mentionsLogin) {
            if (sessionExpiredFiredOnce.compareAndSet(false, true)) {
                Timber.w("session expired detected, clearing user data")
                UserDataRepo.clearAll()
                EventBus.getDefault().post(SessionExpiredEvent())
            }
        }
    }
}
