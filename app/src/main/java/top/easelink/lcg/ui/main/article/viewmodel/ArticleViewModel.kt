package top.easelink.lcg.ui.main.article.viewmodel

import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.LiveData
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

    private val _posts = MutableLiveData<MutableList<Post>>()
    val posts: LiveData<MutableList<Post>> get() = _posts

    private val _blockMessage = MutableLiveData<String>()
    val blockMessage: LiveData<String> get() = _blockMessage

    private val _isNotFound = MutableLiveData<Boolean>()
    val isNotFound: LiveData<Boolean> get() = _isNotFound

    private val _shouldDisplayPosts = MutableLiveData<Boolean>()
    val shouldDisplayPosts: LiveData<Boolean> get() = _shouldDisplayPosts

    private val _articleTitle = MutableLiveData<String>()
    val articleTitle: LiveData<String> get() = _articleTitle

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var mUrl: String? = null
    private var nextPageUrl: String? = null
    private var mFormHash: String? = null
    private var articleAbstract: ArticleAbstractResponse? = null

    fun setUrl(url: String) {
        mUrl = url
    }

    override fun fetchArticlePost(type: Int, callback: ((Boolean) -> Unit)?) {
        safePostValue(_isLoading, true)

        val query: String? = when (type) {
            FETCH_POST_INIT -> mUrl
            FETCH_POST_MORE -> nextPageUrl
            else -> null
        }

        if (query.isNullOrBlank()) {
            safePostValue(_isLoading, false)
            return
        }

        viewModelScope.launch(IOPool) {
            try {
                ArticlesRemoteDataSource.getArticleDetail(
                    query, type == FETCH_POST_INIT
                )?.let { response ->
                    articleAbstract = response.articleAbstractResponse
                    response.articleTitle.takeIf { it.isNotBlank() }?.let {
                        safePostValue(_articleTitle, it)
                    }

                    if (response.postList.isNotEmpty()) {
                        val currentList = _posts.value ?: mutableListOf()
                        val newList = when (type) {
                            FETCH_POST_INIT -> response.postList.toMutableList()
                            else -> currentList.apply { addAll(response.postList) }
                        }
                        safePostValue(_posts, newList)
                    }

                    nextPageUrl = response.nextPageUrl.also { url ->
                        callback?.invoke(url.isEmpty())
                    }
                    mFormHash = response.fromHash
                    safePostValue(_shouldDisplayPosts, true)
                }
            } catch (e: Exception) {
                handleFetchError(e)
            } finally {
                safePostValue(_isLoading, false)
            }
        }
    }

    private fun handleFetchError(e: Exception) {
        when (e) {
            is BlockException -> {
                if (_posts.value?.isNotEmpty() == true) {
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

    private fun <T> safePostValue(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            liveData.value = value
        } else {
            liveData.postValue(value)
        }
    }

    override fun replyAdd(url: String) {
        if (TextUtils.isEmpty(url)) {
            safePostValue(_isLoading, false)
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

        return _posts.value?.firstOrNull()?.content?.let { content ->
            patterns.flatMap { RegexUtils.extractInfoFrom(content, it) }
                .takeIf { it.isNotEmpty() }
                ?.let { ArrayList(it) }
        }
    }

    fun addToFavorite() {
        val currentPosts = _posts.value ?: run {
            showMessage(R.string.add_to_favorite_failed)
            return
        }

        viewModelScope.launch(IOPool) {
            try {
                val title = _articleTitle.value ?: articleAbstract?.title ?: "未知标题"
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
        _posts.value?.let { currentList ->
            val newList = currentList.toMutableList().apply { add(1, post) }
            safePostValue(_posts, newList)
        }
    }

    private fun setArticleNotFound() {
        safePostValue(_isNotFound, true)
        safePostValue(_shouldDisplayPosts, false)
    }

    private fun setArticleBlocked(message: String) {
        safePostValue(_blockMessage, message)
        safePostValue(_shouldDisplayPosts, false)
    }

    private fun extractThreadId(url: String?): String? {
        return url?.split("-")?.getOrNull(1)
    }
}