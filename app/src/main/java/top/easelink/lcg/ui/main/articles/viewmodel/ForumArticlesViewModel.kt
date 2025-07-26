package top.easelink.lcg.ui.main.articles.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import top.easelink.framework.threadpool.IOPool
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.model.LoginRequiredException
import top.easelink.lcg.ui.main.source.model.Article
import top.easelink.lcg.ui.main.source.model.ForumThread
import top.easelink.lcg.ui.main.source.remote.ArticlesRemoteDataSource.getForumArticles
import top.easelink.lcg.utils.WebsiteConstant.FORUM_URL_QUERY
import top.easelink.lcg.utils.showMessage

@Suppress("unused")
const val LAST_POST_ORDER = "&orderby=lastpost"
@Suppress("unused")
const val DATE_LINE_ORDER = "&orderby=dateline"
const val DEFAULT_ORDER = ""

class ForumArticlesViewModel : ViewModel(), ArticleFetcher {
    private var mUrl: String? = null
    private var mFetchType = ArticleFetcher.FetchType.FETCH_INIT
    private var orderType = DEFAULT_ORDER
    private var mCurrentPage = 1
    private var isTabSet = false

    val title = MutableLiveData<String>()
    val articles = MutableLiveData<List<Article>>()
    val threadList = MutableLiveData<List<ForumThread>>()
    val isLoading = MutableLiveData<Boolean>()

    fun initUrlAndFetch(
        url: String,
        fetchType: ArticleFetcher.FetchType,
        order: String = DEFAULT_ORDER
    ) {
        mUrl = if (url.startsWith("forum-") && url.endsWith("html")) {
            try {
                String.format(FORUM_URL_QUERY, url.split("-")[1])
            } catch (e: Exception) {
                Timber.e(e, "Forum URL parse failed")
                url
            }
        } else {
            url
        }
        mFetchType = fetchType
        orderType = order
        fetchArticles(mFetchType) {}
    }

    @MainThread
    fun setTitle(t: String) {
        title.value = t
    }

    private fun composeUrlByRequestType(type: ArticleFetcher.FetchType): String {
        when (type) {
            ArticleFetcher.FetchType.FETCH_INIT -> rewindPageNum()
            ArticleFetcher.FetchType.FETCH_MORE -> nextPage()
        }
        return "$mUrl&page=$mCurrentPage$orderType"
    }

    override fun fetchArticles(fetchType: ArticleFetcher.FetchType, callback: (Boolean) -> Unit) {
        isLoading.value = true
        viewModelScope.launch(IOPool) {
            try {
                val query = composeUrlByRequestType(fetchType)
                getForumArticles(query, fetchType == ArticleFetcher.FetchType.FETCH_INIT)?.let { forumPage ->
                    forumPage.articleList.takeIf { it.isNotEmpty() }?.let { articleList ->
                        handleArticleList(fetchType, articleList, callback)
                    }

                    if (!isTabSet) {
                        threadList.postValue(forumPage.threadList.ifEmpty { emptyList() })
                        isTabSet = true
                    }
                }
            } catch (_: LoginRequiredException) {
                showMessage(R.string.login_required_error)
            } catch (_: Exception) {
                showMessage(R.string.error)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    private fun handleArticleList(
        fetchType: ArticleFetcher.FetchType,
        newList: List<Article>,
        callback: (Boolean) -> Unit
    ) {
        val currentList = articles.value
        when {
            fetchType != ArticleFetcher.FetchType.FETCH_MORE || currentList.isNullOrEmpty() -> {
                articles.postValue(newList)
                callback(true)
            }
            newList.last().title == currentList.last().title -> {
                showMessage(R.string.no_more_content)
                callback(false)
            }
            else -> {
                articles.postValue(currentList + newList)
                callback(true)
            }
        }
    }

    private fun rewindPageNum() {
        mCurrentPage = 1
    }

    private fun nextPage() {
        mCurrentPage++
    }
}