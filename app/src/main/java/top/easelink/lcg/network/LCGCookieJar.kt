package top.easelink.lcg.network

import android.webkit.CookieManager
import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.utils.SharedPreferencesHelper
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import androidx.core.content.edit

/**
 * 单例 cookie 真源。OkHttp / Jsoup / Coil 全部走这一份 jar。
 * WebView 侧由系统 CookieManager 持久化，通过 syncFromWebView 单向同步进 jar；
 * 反向回推 WebView 不做，否则启动时容易把已失效会话灌进 WebView 触发 WAF 拒访问。
 */
object LCGCookieJar {

    private const val LEGACY_MIGRATED_KEY = "lcg_cookie_migrated_v1"

    val jar: ClearableCookieJar by lazy {
        val persistor = SharedPrefsCookiePersistor(LCGApp.context)
        PersistentCookieJar(SetCookieCache(), persistor).also {
            migrateLegacyCookiesIfNeeded(it)
        }
    }

    /** 取出一个 url 应该携带的 cookie 列表（key=value 形式，方便 Jsoup/Coil 拼请求头）。 */
    fun cookiesForUrl(url: String): Map<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return emptyMap()
        return jar.loadForRequest(httpUrl).associate { it.name to it.value }
    }

    /**
     * Jsoup 拿到响应后回写 cookie。Jsoup 的 cookies 没有 domain/path/expires 信息，
     * 这里按请求 URL 的 host 构造，并给一个 30 天过期，避免无限期残留。
     */
    fun saveCookiesFromJsoup(url: String, cookies: Map<String, String>) {
        if (cookies.isEmpty()) return
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val list = cookies.mapNotNull { (name, value) ->
            buildCookie(httpUrl, name, value)
        }
        if (list.isNotEmpty()) jar.saveFromResponse(httpUrl, list)
    }

    /**
     * 把 WebView 的 CookieManager 中关于 url 的 cookie 同步进 jar。
     * 在 onPageCommitVisible / onPageFinished 等时机调用。
     */
    fun syncFromWebView(url: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val raw = CookieManager.getInstance().getCookie(url) ?: return
        val list = raw.split(";")
            .mapNotNull { entry ->
                val pair = entry.trim().split("=", limit = 2)
                if (pair.size == 2 && pair[0].isNotBlank()) {
                    buildCookie(httpUrl, pair[0], pair[1])
                } else null
            }
        if (list.isNotEmpty()) jar.saveFromResponse(httpUrl, list)
    }

    /** 登出时调用：清空 jar 与 WebView CookieManager。 */
    fun clearAll() {
        jar.clear()
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
    }

    private fun buildCookie(httpUrl: HttpUrl, name: String, value: String): Cookie? = runCatching {
        // 短期会话类 cookie 必须用短 TTL，否则下次冷启动会把已过期的旧值推回 WebView，
        // 服务端看到过期会话直接拒绝（典型表现：waf_text_verify.html 无法访问）。
        // wzws_cid 是知道创宇 WAF 的挑战 cookie（Max-Age=1800s），PHPSESSID 同理。
        val ttl = if (name.startsWith("wzws_") || name == "PHPSESSID") {
            SHORT_SESSION_TTL_MS
        } else {
            DEFAULT_COOKIE_TTL_MS
        }
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain(httpUrl.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + ttl)
            .build()
    }.onFailure { Timber.w(it, "build cookie failed: %s=%s", name, value) }.getOrNull()

    /**
     * 一次性把老版本扁平 SP（sp_cookie）里残留的 cookie 拷到 jar，让现存用户升级后不被踢下线。
     * 完成后写一个标志位，下次启动不再重复。老 SP 留着不删，避免破坏其它可能的读取者。
     */
    private fun migrateLegacyCookiesIfNeeded(target: ClearableCookieJar) {
        val userSp = SharedPreferencesHelper.getUserSp()
        if (userSp.getBoolean(LEGACY_MIGRATED_KEY, false)) return
        val legacy = SharedPreferencesHelper.getCookieSp().all
        if (legacy.isNotEmpty()) {
            val httpUrl = SERVER_BASE_URL.toHttpUrlOrNull()
            if (httpUrl != null) {
                val list = legacy.mapNotNull { (k, v) ->
                    val str = v?.toString() ?: return@mapNotNull null
                    buildCookie(httpUrl, k, str)
                }
                if (list.isNotEmpty()) {
                    target.saveFromResponse(httpUrl, list)
                    Timber.d("migrated %d legacy cookies into jar", list.size)
                }
            }
        }
        userSp.edit { putBoolean(LEGACY_MIGRATED_KEY, true) }
    }

    private const val DEFAULT_COOKIE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    private const val SHORT_SESSION_TTL_MS = 30L * 60 * 1000  // 30 分钟，对齐 WAF challenge 默认 Max-Age
}
