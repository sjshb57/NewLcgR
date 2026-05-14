package top.easelink.lcg.utils

import top.easelink.lcg.network.LCGCookieJar
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL

/**
 * 全局 cookie 工具 —— 历史上多套实现的入口都在这里。
 * 现在所有调用都委托给 [LCGCookieJar]，保证整个 App 共用同一份 cookie 真源。
 *
 * 老的扁平 SharedPreferences 实现已废弃，迁移逻辑见 LCGCookieJar.migrateLegacyCookiesIfNeeded。
 */

/** 默认按论坛主域取 cookie，保持旧调用点的语义。 */
fun getCookies(): Map<String, String> = LCGCookieJar.cookiesForUrl(SERVER_BASE_URL)

/** 取一个具体 URL 应该携带的 cookie（用于跨域请求，如附件域名）。 */
fun getCookiesFor(url: String): Map<String, String> = LCGCookieJar.cookiesForUrl(url)

/** WebView 拿到的 "k1=v1; k2=v2" 字符串入库。commit 参数保留以兼容旧调用点。 */
@Suppress("UNUSED_PARAMETER")
fun updateCookies(cookieUrl: String, commit: Boolean) {
    val cookies = cookieUrl.split(";")
        .mapNotNull { entry ->
            val pair = entry.trim().split("=", limit = 2)
            if (pair.size == 2 && pair[0].isNotBlank()) pair[0] to pair[1] else null
        }
        .toMap()
    LCGCookieJar.saveCookiesFromJsoup(SERVER_BASE_URL, cookies)
}

/** Jsoup 响应回写 cookie。 */
fun updateCookies(cookies: Map<String, String>) {
    LCGCookieJar.saveCookiesFromJsoup(SERVER_BASE_URL, cookies)
}

/** 登出时调用：jar + WebView 一起清，OkApiClient 不再残留旧 cookie。 */
fun clearCookies() {
    LCGCookieJar.clearAll()
}

/** 把 cookie 拼成 HTTP 头格式（用于 Coil 等需要手动设置 Cookie 头的场景）。 */
fun Map<String, String>.toHeaderString(): String =
    entries.joinToString("; ") { "${it.key}=${it.value}" }
