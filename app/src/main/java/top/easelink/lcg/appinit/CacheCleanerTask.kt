package top.easelink.lcg.appinit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.easelink.lcg.cache.HotTopicCacheManager
import top.easelink.lcg.cache.PreviewCacheManager

object CacheCleanerTask {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun clearCachesIfNeeded() {
        scope.launch {
            delay(5000)
            PreviewCacheManager.clearAllCaches()
            HotTopicCacheManager.clearAllCaches()
        }
    }
}