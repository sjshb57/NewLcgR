package top.easelink.lcg.ui.main.follow.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import timber.log.Timber
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.main.follow.model.FeedInfo
import top.easelink.lcg.utils.WebsiteConstant.FOLLOW_FEED_QUERY
import java.util.Locale

class FollowingFeedViewModel : ViewModel() {

    val follows = MutableLiveData<List<FeedInfo>>()
    val isLoading = MutableLiveData<Boolean>()
    val isLoadingForLoadMore = MutableLiveData<Boolean>()
    var pageNum = 1

    fun fetchData() {
        val url = String.format(Locale.US, FOLLOW_FEED_QUERY, 1, 1)
        isLoading.value = true
        viewModelScope.launch {
            try {
                parseFeeds(JsoupClient.sendAjaxRequest(url))
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchMore(callBack: (Boolean) -> Unit) {
        isLoadingForLoadMore.value = true
        val url = String.format(Locale.US, FOLLOW_FEED_QUERY, pageNum, 1)
        viewModelScope.launch {
            try {
                val success = parseFeeds(JsoupClient.sendAjaxRequest(url))
                callBack(success)
                if (success) {
                    pageNum += 1
                }
            } catch (e: Exception) {
                Timber.e(e)
                callBack(false)
            } finally {
                isLoadingForLoadMore.value = false
            }
        }
    }

    private fun parseFeeds(xml: String): Boolean {
        if (xml == "false") return false

        val result = xml
            .substring(xml.indexOf("[CDATA[", 0, true) + 7)
            .removeSuffix("]]></root>")

        val doc = Jsoup.parse(result)
        val feedInfos = doc.select("li.cl").map {
            val avatarUrl = it.selectFirst("a.z > img")?.attr("src").orEmpty()
            var username = ""
            var dateTime = ""
            it.selectFirst("div.flw_author")?.let { author ->
                username = author.selectFirst("a")?.text().orEmpty()
                dateTime = author.selectFirst("span")?.text().orEmpty()
            }
            val title = it.selectFirst("h2")?.text().orEmpty()
            val articleUrl = it.selectFirst("h2 > a")?.attr("href").orEmpty()

            val followImages = it.getElementsByClass("flw_image")
                .select("ul > li")
                .mapNotNull { li ->
                    li.selectFirst("img")?.attr("src")
                }

            val content = it.selectFirst(".pbm")?.let { pbm ->
                pbm.select("div.flw_image").remove()
                pbm.select("img").remove()
                pbm.select("a.flw_readfull").remove()
                pbm.html()
            }.orEmpty()

            val forum = it.selectFirst("div.xg1 > a")?.text().orEmpty()
            val quote = it.selectFirst("div.flw_quotenote")?.text().orEmpty()

            FeedInfo(
                avatar = avatarUrl,
                username = username,
                dateTime = dateTime,
                title = title,
                articleUrl = articleUrl,
                content = content,
                forum = forum,
                quote = quote,
                images = followImages
            )
        }
        follows.postValue(feedInfos)
        return true
    }
}