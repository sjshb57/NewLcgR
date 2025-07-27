package top.easelink.lcg.ui.main.follow.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.main.follow.model.FollowInfo
import top.easelink.lcg.ui.main.follow.model.FollowResult

class FollowListViewModel : ViewModel() {

    val follows = MutableLiveData<FollowResult>()
    val isLoading = MutableLiveData<Boolean>()
    val isLoadingForLoadMore = MutableLiveData<Boolean>()

    fun fetchData(url: String, isLoadMore: Boolean = false) {
        if (isLoadMore) {
            isLoadingForLoadMore.value = true
        } else {
            isLoading.value = true
        }

        viewModelScope.launch {
            try {
                val document = JsoupClient.sendGetRequestWithQuery(url)
                parseFollows(document)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                if (isLoadMore) {
                    isLoadingForLoadMore.value = false
                } else {
                    isLoading.value = false
                }
            }
        }
    }

    private fun parseFollows(doc: Document) {
        doc.apply {
            val followInfos = select("li.cl").map {
                val avatarUrl = it.selectFirst("img")?.attr("src").orEmpty()
                val username = it.getElementById("edit_avt")?.attr("title").orEmpty()
                val lastAction = it.selectFirst("p")?.text().orEmpty()
                val url = it.selectFirst("a[id^=a_followmod]")?.attr("href").orEmpty()

                var follower = 0
                var following = 0
                it.select("strong.xi2").takeIf { e -> e.size == 2 }?.let { e ->
                    follower = e[0].text().toIntOrNull() ?: 0
                    following = e[1].text().toIntOrNull() ?: 0
                }

                FollowInfo(
                    avatar = avatarUrl,
                    lastAction = lastAction,
                    username = username,
                    followerNum = follower,
                    followingNum = following,
                    followOrUnFollowUrl = url
                )
            }

            val nextPageUrl = selectFirst("a.nxt")?.attr("href")
            follows.value = FollowResult(followInfos, nextPageUrl)
        }
    }
}