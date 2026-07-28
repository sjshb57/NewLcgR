package top.easelink.lcg.network

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.ui.search.model.RequestTooOftenException
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * ⚠️ 现状：这个 object 全项目没有任何调用点，是死代码。
 * 实际所有网络请求都走 [JsoupClient]。
 * 留着是因为它是"迁移到 OkHttp"的半成品骨架 —— 要么补完，要么删掉，别放着不管。
 */
object OkApiClient : ApiRequest {

    private const val FOLLOW_REDIRECTS = true

    // 旧实现是 5s 全超时，国内访问 52pojie 经常 >5s -> 大量假性 SocketTimeoutException。
    // 调整到经验值：连接 10s，读写 15s，整体调用 30s。
    private const val CONNECT_TIMEOUT = 10L
    private const val READ_WRITE_TIMEOUT = 15L
    private const val CALL_TIMEOUT = 30L

    private val mClient: OkHttpClient by lazy {
        val cacheDirectory = File(LCGApp.context.cacheDir, "okhttp_cache")
        OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(CacheControlInterceptor)
            .cookieJar(LCGCookieJar.jar)
            .followRedirects(FOLLOW_REDIRECTS)
            .retryOnConnectionFailure(true)
            .cache(Cache(cacheDirectory, 10 * 1024 * 1024))
            .build()
    }

    override fun sendGetRequestWithQuery(query: String): Document? {
        // 不能用 TODO()：它抛 NotImplementedError(Error)，调用方 catch(Exception) 接不住
        throw UnsupportedOperationException("OkApiClient.sendGetRequestWithQuery not implemented")
    }

    override fun sendGetRequestWithUrl(url: String): Document? {
        val request = Request.Builder().get().url(url).build()
        val response = mClient.newCall(request).execute()
        return when (response.code) {
            in 200..299 -> Jsoup.parse(response.body.string())
            302 -> throw RequestTooOftenException()
            else -> null
        }
    }

    override fun sendPostRequestWithUrl(
        url: String,
        form: MutableMap<String, String>?
    ): Connection.Response {
        throw UnsupportedOperationException("OkApiClient.sendPostRequestWithUrl not implemented")
    }
}
