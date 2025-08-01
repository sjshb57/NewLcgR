package top.easelink.lcg.ui.main.article.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import timber.log.Timber
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.utils.WebsiteConstant.CHECK_RULE_URL
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.getCookies
import top.easelink.lcg.utils.updateCookies

class ReplyPostViewModel : ViewModel() {

    val sending = MutableLiveData<Boolean>()

    fun sendReply(query: String?, content: String, callback: (Boolean) -> Unit) {
        sending.value = true
        viewModelScope.launch {
            try {
                sendReplyAsync(query, content, callback)
            } catch (e: Exception) {
                Timber.e(e)
                callback(false)
                sending.value = false
            }
        }
    }

    private fun sendReplyAsync(query: String?, content: String, callback: (Boolean) -> Unit) {
        Timber.d(content)
        if (query.isNullOrEmpty() || content.isBlank()) {
            sending.value = false
            return
        }

        val queryMap = mutableMapOf<String, String>().apply {
            query.split("&").forEach {
                val l = it.split("=")
                put(l[0], if (l.size == 2) l[1] else "")
            }
        }

        try {
            JsoupClient
                .sendGetRequestWithQuery(query)
                .run {
                    val noticeauthormsg = selectFirst("input[name=noticeauthormsg]")?.attr("value").orEmpty()
                    val noticetrimstr = selectFirst("input[name=noticetrimstr]")?.attr("value").orEmpty()
                    val noticeauthor = selectFirst("input[name=noticeauthor]")?.attr("value").orEmpty()
                    val handlekey = selectFirst("input[name=handlekey]")?.attr("value") ?: "reply"
                    val usesig = selectFirst("input[name=usesig]")?.attr("value") ?: "1"
                    val reppid = selectFirst("input[name=reppid]")?.attr("value").orEmpty()
                    val reppost = selectFirst("input[name=reppost]")?.attr("value").orEmpty()
                    val formHash = selectFirst("input[name=formhash]")?.attr("value").orEmpty()

                    var response = Jsoup
                        .connect(CHECK_RULE_URL)
                        .timeout(60 * 1000)
                        .cookies(getCookies())
                        .method(Connection.Method.GET)
                        .execute()

                    if (response.statusCode() in 200 until 300) {
                        updateCookies(response.cookies())
                        val url = "${SERVER_BASE_URL}forum.php?mod=post&infloat=yes&action=reply" +
                                "&fid=${queryMap["fid"]}&extra=${queryMap["extra"]}&tid=${queryMap["tid"]}&replysubmit=yes&inajax=1"

                        response = Jsoup
                            .connect(url)
                            .cookies(getCookies())
                            .data(
                                "formhash", formHash,
                                "handlekey", handlekey,
                                "noticeauthor", noticeauthor,
                                "noticetrimstr", noticetrimstr,
                                "noticeauthormsg", noticeauthormsg,
                                "usesig", usesig,
                                "reppid", reppid,
                                "reppost", reppost,
                                "subject", "",
                                "message", content
                            )
                            .postDataCharset("gbk")
                            .method(Connection.Method.POST)
                            .execute()

                        updateCookies(response.cookies())
                        callback(response.statusCode() in 200 until 300)
                    } else {
                        Timber.e(response.body())
                        callback(false)
                    }
                }
        } finally {
            sending.value = false
        }
    }
}