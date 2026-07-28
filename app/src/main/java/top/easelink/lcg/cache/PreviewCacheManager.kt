package top.easelink.lcg.cache

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.lcg.appinit.LCGApp
import java.io.File

object PreviewCacheManager: ICacheManager {
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val PREVIEW_CACHE_FOLDER = "${LCGApp.context.cacheDir}/preview_articles"
    private const val CONFIG_FILE_NAME = "PreviewCacheConfig"

    /**
     * 将Url对应的内容存入磁盘缓存
     */
    fun saveToDisk(url: String, content: String) {
        cacheScope.launch {
            checkDirs()
            val file = File(PREVIEW_CACHE_FOLDER, getCacheFileName(url))
            try {
                file.writeText(content)
            } catch (e: Exception) {
                Timber.e(e)
                file.delete()
            }
        }
    }

    /**
     * 从磁盘中找到对应的数据内容
     */
    fun findDocOrNull(url: String): Document? {
        val file: File? = findInDisk("$PREVIEW_CACHE_FOLDER/${getCacheFileName(url)}")
        if (file != null && file.exists()) {
            try {
                return Jsoup.parse(file, "utf-8")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return null
    }

    /**
     * 清除磁盘缓存。每 24 小时最多清一次，清完写回 LastCleanTime。
     */
    override suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        if (checkShouldCache()) {
            File(PREVIEW_CACHE_FOLDER).deleteRecursively()
            getConfigSp().edit { putLong(KEY_LAST_CLEAN_TIME, System.currentTimeMillis()) }
        }
        Unit
    }

    private fun checkDirs() {
        val parent = File(PREVIEW_CACHE_FOLDER)
        if (!parent.exists()) parent.mkdirs()
    }

    private fun findInDisk(url: String): File? {
        return if (isLocalFile(url)) {
            File(url)
        } else {
            null
        }
    }

    private fun isLocalFile(url: String): Boolean {
        return url.startsWith("/")
    }

    private fun getCacheFileName(url: String): String {
        return url.hashCode().toString()
    }

    /**
     * 决定本次是否要执行一次清理。默认 LastCleanTime = 0L（远古时间），首次启动会清一次；
     * 之后每 24 小时清一次。原实现默认值用 now → 永远 false → 缓存目录无限膨胀。
     */
    private fun checkShouldCache(): Boolean {
        val last = getConfigSp().getLong(KEY_LAST_CLEAN_TIME, 0L)
        return System.currentTimeMillis() - last > CLEAN_INTERVAL_MS
    }

    private fun getConfigSp() =
        LCGApp.instance.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE)

    private const val KEY_LAST_CLEAN_TIME = "LastCleanTime"
    private const val CLEAN_INTERVAL_MS = 24L * 60 * 60 * 1000
}