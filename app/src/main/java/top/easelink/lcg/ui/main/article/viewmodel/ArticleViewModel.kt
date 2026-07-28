package top.easelink.lcg.ui.main.article.viewmodel

import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import top.easelink.framework.threadpool.IOPool
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.ui.main.article.viewmodel.ArticleAdapterListener.Companion.FETCH_POST_INIT
import top.easelink.lcg.ui.main.article.viewmodel.ArticleAdapterListener.Companion.FETCH_POST_MORE
import top.easelink.lcg.ui.main.model.BlockException
import top.easelink.lcg.ui.main.model.NetworkException
import top.easelink.lcg.ui.main.source.local.ArticlesLocalDataSource
import top.easelink.lcg.ui.main.source.model.ArticleAbstractResponse
import top.easelink.lcg.ui.main.source.model.ArticleEntity
import top.easelink.lcg.ui.main.source.model.Post
import top.easelink.lcg.ui.main.source.remote.ArticlesRemoteDataSource
import top.easelink.lcg.utils.RegexUtils
import top.easelink.lcg.utils.showMessage
import java.io.IOException
import java.util.*

class ArticleViewModel : ViewModel(), ArticleAdapterListener {

    val posts = MutableLiveData<MutableList<Post>>()
    val blockMessage = MutableLiveData<String>()
    val isNotFound = MutableLiveData<Boolean>()
    val shouldDisplayPosts = MutableLiveData<Boolean>()
    val articleTitle = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val isFavorited = MutableLiveData<Boolean>(false)

    /**
     * 是否还有下一页。Adapter 据此显示 LoadMore item，并据此判断"加载更多"是否
     * 应当成功消失（原实现用一次性回调，异步未返回值前默认 false，导致 LoadMore 永不消失）。
     */
    val hasMorePages = MutableLiveData(false)

    /** 翻页 append 事件：把新增加的页内容直接交给 Adapter，UI 用 notifyItemRangeInserted。 */
    val newPostsAppended = MutableLiveData<List<Post>>()

    /** 单条插入事件：position + post，UI 用 notifyItemInserted。 */
    val postInsertedAt = MutableLiveData<Pair<Int, Post>>()

    private var mUrl: String? = null
    private var nextPageUrl: String? = null
    private var mFormHash: String? = null
    private var articleAbstract: ArticleAbstractResponse? = null

    /** 防止 LoadMore 在 onBind 频繁触发时重复拉取，导致同一页被 append 多次。 */
    private var fetching = false

    fun setUrl(url: String) {
        mUrl = url
    }

    override fun fetchArticlePost(type: Int, callback: ((Boolean) -> Unit)?) {
        if (fetching) {
            callback?.invoke(false)
            return
        }
        safeUpdate(isLoading, true)

        val query: String? = when (type) {
            FETCH_POST_INIT -> mUrl
            FETCH_POST_MORE -> nextPageUrl
            else -> null
        }

        if (query.isNullOrBlank()) {
            safeUpdate(isLoading, false)
            callback?.invoke(false)
            return
        }

        fetching = true
        viewModelScope.launch(IOPool) {
            try {
                val response = ArticlesRemoteDataSource.getArticleDetail(
                    query, type == FETCH_POST_INIT
                )
                if (response == null) {
                    callback?.invoke(false)
                    return@launch
                }

                articleAbstract = response.articleAbstractResponse
                response.articleTitle.takeIf { it.isNotBlank() }?.let {
                    safeUpdate(articleTitle, it)
                }

                if (response.postList.isNotEmpty()) {
                    when (type) {
                        FETCH_POST_INIT -> {
                            // 首次/重新加载：全列表替换。posts 仍是真源（用于 extractDownloadUrl / addPostToTop 等）。
                            safeUpdate(posts, response.postList.toMutableList())
                        }
                        else -> {
                            // 翻页 append：把增量交给 Adapter 自己 add+notify，避免 setItems 全量重绑。
                            posts.value?.addAll(response.postList)
                            safeUpdate(newPostsAppended, response.postList)
                        }
                    }
                }

                nextPageUrl = response.nextPageUrl
                safeUpdate(hasMorePages, nextPageUrl?.isNotEmpty() == true)
                mFormHash = response.fromHash
                safeUpdate(shouldDisplayPosts, true)
                callback?.invoke(true)
            } catch (e: Exception) {
                handleFetchError(e)
                callback?.invoke(false)
            } finally {
                fetching = false
                safeUpdate(isLoading, false)
            }
        }
    }

    private fun handleFetchError(e: Exception) {
        when (e) {
            is BlockException -> {
                if (posts.value?.isNotEmpty() == true) {
                    showMessage(e.alertMessage)
                } else {
                    setArticleBlocked(e.alertMessage)
                }
            }
            is NetworkException -> setArticleNotFound()
            is IOException -> showMessage(R.string.io_error_mark_invalid)
            else -> showMessage(R.string.error)
        }
        Timber.e(e)
    }

    override fun replyAdd(url: String) {
        if (TextUtils.isEmpty(url)) {
            safeUpdate(isLoading, false)
            throw IllegalStateException()
        }
        viewModelScope.launch(IOPool) {
            ArticlesRemoteDataSource.replyAdd(url).also {
                showMessage(it)
            }
        }
    }

    fun extractDownloadUrl(): ArrayList<String>? {
        val patterns = listOf(
            "https://[a-zA-Z0-9.]{0,20}lanzou[a-z]{1}.com/[a-zA-Z0-9]{4,12}",
            "https://pan.baidu.com/s/.{23}",
            "http://t.cn/[a-zA-Z0-9]{8}",
            "https://cloud.189.cn/t/[a-zA-Z0-9]{4,12}",
            "https://pan.xunlei.com/s/[a-zA-Z0-9_]{1,20}"
        )

        return posts.value?.firstOrNull()?.content?.let { content ->
            patterns.flatMap { RegexUtils.extractInfoFrom(content, it) }
                .takeIf { it.isNotEmpty() }
                ?.let { ArrayList(it) }
        }
    }

    fun checkIsFavorited() {
        val url = mUrl ?: return
        viewModelScope.launch(IOPool) {
            val isFav = ArticlesLocalDataSource.isArticleInFavorites(url)
            safeUpdate(isFavorited, isFav)
        }
    }

    fun toggleFavorite() {
        val isCurrentlyFavorited = isFavorited.value ?: false
        if (isCurrentlyFavorited) {
            removeFromFavorite()
        } else {
            addToFavorite()
        }
    }

    fun addToFavorite() {
        val currentPosts = posts.value ?: run {
            showMessage(R.string.add_to_favorite_failed)
            return
        }

        viewModelScope.launch(IOPool) {
            try {
                val title = articleTitle.value ?: articleAbstract?.title ?: "未知标题"
                val author = currentPosts.firstOrNull()?.author ?: ""
                val content = articleAbstract?.description ?: ""
                val url = mUrl ?: throw IllegalStateException("URL is null")

                val articleEntity = ArticleEntity(
                    title = title,
                    author = author,
                    url = url,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )

                if (AppConfig.syncFavorites && UserDataRepo.isLoggedIn) {
                    val threadId = extractThreadId(mUrl)
                    if (threadId != null && mFormHash != null) {
                        val success = ArticlesRemoteDataSource.addFavorites(threadId, mFormHash!!)
                        showMessage(if (success) R.string.sync_favorite_successfully else R.string.sync_favorite_failed)
                    } else {
                        showMessage(R.string.sync_favorite_failed)
                    }
                }

                try {
                    val res = ArticlesLocalDataSource.addArticleToFavorite(articleEntity)
                    if (!AppConfig.syncFavorites) {
                        showMessage(if (res) R.string.add_to_favorite_successfully else R.string.add_to_favorite_failed)
                    }
                    safeUpdate(isFavorited, true)
                } catch (e: Exception) {
                    Timber.e(e)
                    showMessage(R.string.add_to_favorite_failed)
                }
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(R.string.add_to_favorite_failed)
            }
        }
    }

    fun removeFromFavorite() {
        val url = mUrl ?: return
        viewModelScope.launch(IOPool) {
            try {
                // 远端取消收藏：尽力同步，失败也不挡本地删除，避免 UI 进入 "本地已删/远端仍在" 死锁。
                if (AppConfig.syncFavorites && UserDataRepo.isLoggedIn) {
                    val threadId = extractThreadId(mUrl)
                    val formHash = mFormHash
                    if (threadId != null && !formHash.isNullOrEmpty()) {
                        val success = ArticlesRemoteDataSource.removeFavorites(threadId, formHash)
                        if (!success) {
                            Timber.w("remote unfavorite failed, falling back to local-only removal")
                        }
                    }
                }

                val res = ArticlesLocalDataSource.delArticleFromFavorite(url)
                showMessage(if (res) R.string.remove_all_favorites_successfully else R.string.add_to_favorite_failed)
                safeUpdate(isFavorited, false)
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(R.string.add_to_favorite_failed)
            }
        }
    }

    fun addPostToTop(post: Post) {
        posts.value?.let { currentList ->
            // 改原 list 并发单条增量事件，避免整列重绑。
            val insertPos = if (currentList.isEmpty()) 0 else 1
            currentList.add(insertPos, post)
            safeUpdate(postInsertedAt, insertPos to post)
        }
    }

    private fun setArticleNotFound() {
        safeUpdate(isNotFound, true)
        safeUpdate(shouldDisplayPosts, false)
    }

    private fun setArticleBlocked(message: String) {
        safeUpdate(blockMessage, message)
        safeUpdate(shouldDisplayPosts, false)
    }

    private fun extractThreadId(url: String?): String? {
        return url?.split("-")?.getOrNull(1)
    }

    private fun <T> safeUpdate(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.value = value
        } else {
            liveData.postValue(value)
        }
    }
}