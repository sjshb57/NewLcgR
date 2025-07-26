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

    private var mUrl: String? = null
    private var nextPageUrl: String? = null
    private var mFormHash: String? = null
    private var articleAbstract: ArticleAbstractResponse? = null

    fun setUrl(url: String) {
        mUrl = url
    }

    override fun fetchArticlePost(type: Int, callback: ((Boolean) -> Unit)?) {
        safeUpdate(isLoading, true)

        val query: String? = when (type) {
            FETCH_POST_INIT -> mUrl
            FETCH_POST_MORE -> nextPageUrl
            else -> null
        }

        if (query.isNullOrBlank()) {
            safeUpdate(isLoading, false)
            return
        }

        viewModelScope.launch(IOPool) {
            try {
                ArticlesRemoteDataSource.getArticleDetail(
                    query, type == FETCH_POST_INIT
                )?.let { response ->
                    articleAbstract = response.articleAbstractResponse
                    response.articleTitle.takeIf { it.isNotBlank() }?.let {
                        safeUpdate(articleTitle, it)
                    }

                    if (response.postList.isNotEmpty()) {
                        val currentList = posts.value ?: mutableListOf()
                        val newList = when (type) {
                            FETCH_POST_INIT -> response.postList.toMutableList()
                            else -> currentList.apply { addAll(response.postList) }
                        }
                        safeUpdate(posts, newList)
                    }

                    nextPageUrl = response.nextPageUrl.also { url ->
                        callback?.invoke(url.isEmpty())
                    }
                    mFormHash = response.fromHash
                    safeUpdate(shouldDisplayPosts, true)
                }
            } catch (e: Exception) {
                handleFetchError(e)
            } finally {
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

    fun addPostToTop(post: Post) {
        posts.value?.let { currentList ->
            val newList = currentList.toMutableList().apply { add(1, post) }
            safeUpdate(posts, newList)
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