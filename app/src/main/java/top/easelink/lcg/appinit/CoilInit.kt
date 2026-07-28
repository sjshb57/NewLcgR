package top.easelink.lcg.appinit

import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import top.easelink.lcg.utils.getCookiesFor
import top.easelink.lcg.utils.toHeaderString
import java.util.concurrent.TimeUnit

fun initCoil() {
    Coil.setImageLoader(LCGImageLoaderFactory)
}

object LCGImageLoaderFactory : ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader
            .Builder(LCGApp.context)
            .okHttpClient(buildClient())
            .build()
    }

    private fun buildClient(): OkHttpClient {
        // 论坛附件域 attach.52pojie.cn 需要带原站 Referer 和会话 cookie 才能访问。
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            if (original.url.host.contains("attach")) {
                val cookieHeader = getCookiesFor(original.url.toString()).toHeaderString()
                val newRequest = original.newBuilder()
                    .header("Referer", "https://www.52pojie.cn/")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("Sec-Fetch-Mode", "no-cors")
                    .header("Sec-Fetch-Dest", "image")
                    .header("X-Requested-With", "top.easelink.lcg")
                    .also { if (cookieHeader.isNotEmpty()) it.header("Cookie", cookieHeader) }
                    .header("Connection", "keep-alive")
                    .header(
                        "Accept",
                        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                    )
                    .header("Accept-Encoding", "gzip, deflate")
                    // 论坛附件 CDN 历史上对移动端 UA 偶有限制，沿用旧实现的桌面 UA 作为兼容。
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(original)
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
