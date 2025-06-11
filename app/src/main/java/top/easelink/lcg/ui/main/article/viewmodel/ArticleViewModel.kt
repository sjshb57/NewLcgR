package top.easelink.lcg.ui.main.article.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
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
        isLoading.value = true
        val query: String? =
            when (type) {
                FETCH_POST_INIT -> mUrl
                FETCH_POST_MORE -> nextPageUrl
                else -> null
            }

        if (query.isNullOrBlank()) {
            isLoading.value = false
            return
        }
        GlobalScope.launch(IOPool) {
            try {
                ArticlesRemoteDataSource.getArticleDetail(
                    query, type == FETCH_POST_INIT
                )?.let {
                    articleAbstract = it.articleAbstractResponse
                    if (it.articleTitle.isNotBlank()) {
                        articleTitle.postValue(it.articleTitle)
                    }
                    if (it.postList.isNotEmpty()) {
                        if (type == FETCH_POST_INIT) {
                            posts.postValue(it.postList.toMutableList())
                        } else {
                            val list = posts.value
                            if (list != null && list.isNotEmpty()) {
                                list.addAll(it.postList)
                                posts.postValue(list)
                            } else {
                                posts.postValue(it.postList.toMutableList())
                            }
                        }
                    }
                    nextPageUrl = it.nextPageUrl.also { url ->
                        callback?.invoke(url.isEmpty())
                    }
                    mFormHash = it.fromHash
                    shouldDisplayPosts.postValue(true)
                }
            } catch (e: Exception) {
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
            isLoading.postValue(false)
        }
    }

    override fun replyAdd(url: String) {
        if (TextUtils.isEmpty(url)) {
            isLoading.value = false
            throw IllegalStateException()
        }
        GlobalScope.launch(IOPool) {
            ArticlesRemoteDataSource.replyAdd(url).also {
                showMessage(it)
            }
        }
    }

    fun extractDownloadUrl(): ArrayList<String>? {
        val patternLanzous = "https://[a-zA-Z0-9.]{0,20}lanzou[a-z]{1}.com/[a-zA-Z0-9]{4,12}"
        val patternBaidu = "https://pan.baidu.com/s/.{23}"
        val patternT = "http://t.cn/[a-zA-Z0-9]{8}"
        val pattern189 = "https://cloud.189.cn/t/[a-zA-Z0-9]{4,12}"
        val patternXunlei = "https://pan.xunlei.com/s/[a-zA-Z0-9_]{1,20}"
        val list: List<Post>? = posts.value
        var resSet: HashSet<String>? = null
        if (list != null && list.isNotEmpty()) {
            val content = list[0].content
            resSet = RegexUtils.extractInfoFrom(content, patternLanzous)
            resSet.addAll(RegexUtils.extractInfoFrom(content, patternBaidu))
            resSet.addAll(RegexUtils.extractInfoFrom(content, patternT))
            resSet.addAll(RegexUtils.extractInfoFrom(content, pattern189))
            resSet.addAll(RegexUtils.extractInfoFrom(content, patternXunlei))
        }
        return resSet?.let {
            ArrayList(it)
        }
    }

    fun addToFavorite() {
        val posts: MutableList<Post> = posts.value ?: mutableListOf()
        if (posts.isEmpty()) {
            showMessage(R.string.add_to_favorite_failed)
            return // 添加 return 确保在 posts 为空时不执行后续逻辑
        }
        GlobalScope.launch(IOPool) {
            try {
                val title = articleTitle.value ?: articleAbstract?.title
                val threadId = extractThreadId(mUrl)
                val author = posts[0].author
                val content = articleAbstract?.description ?: ""
                val articleEntity = ArticleEntity(
                    title = title ?: "未知标题",
                    author = author,
                    url = mUrl!!,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                if (AppConfig.syncFavorites && UserDataRepo.isLoggedIn) {
                    if (threadId != null && mFormHash != null) {
                        ArticlesRemoteDataSource.addFavorites(threadId, mFormHash!!).let {
                            if (it) showMessage(R.string.sync_favorite_successfully)
                            else showMessage(R.string.sync_favorite_failed)
                        }
                    } else {
                        showMessage(R.string.sync_favorite_failed)
                    }
                }
                try {
                    val res = ArticlesLocalDataSource.addArticleToFavorite(articleEntity)
                    if (!AppConfig.syncFavorites) {
                        if (res) {
                            showMessage(R.string.add_to_favorite_successfully)
                        } else {
                            showMessage(R.string.add_to_favorite_failed)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    showMessage(R.string.add_to_favorite_failed)
                }
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(R.string.add_to_favorite_failed) // 确保外层异常也被捕获并提示用户
            }
        }
    }

    fun addPostToTop(post: Post) {
        posts.value?.let {
            it.add(1, post)
            posts.value = it
        }
    }

    private fun setArticleNotFound() {
        isNotFound.postValue(true)
        shouldDisplayPosts.postValue(false)
    }

    private fun setArticleBlocked(message: String) {
        blockMessage.postValue(message)
        shouldDisplayPosts.postValue(false)
    }

    private fun extractThreadId(url: String?): String? {
        return if (TextUtils.isEmpty(url)) {
            null
        } else try {
            url!!.split("-").toTypedArray()[1]
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}