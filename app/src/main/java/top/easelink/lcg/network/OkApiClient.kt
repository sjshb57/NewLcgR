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
        TODO()
    }

    override fun sendGetRequestWithUrl(url: String): Document? {
        val request = Request.Builder().get().url(url).build()
        val response = mClient.newCall(request).execute()
        return when (response.code) {
            in 200..299 -> Jsoup.parse(response.body?.string() ?: "")
            302 -> throw RequestTooOftenException()
            else -> null
        }
    }

    override fun sendPostRequestWithUrl(
        url: String,
        form: MutableMap<String, String>?
    ): Connection.Response {
        TODO("not implemented")
    }
}
