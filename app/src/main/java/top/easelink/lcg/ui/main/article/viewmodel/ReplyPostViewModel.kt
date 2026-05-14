package top.easelink.lcg.ui.main.article.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.utils.WebsiteConstant.CHECK_RULE_URL
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL

class ReplyPostViewModel : ViewModel() {

    val sending = MutableLiveData<Boolean>()

    fun sendReply(query: String?, content: String, callback: (Boolean) -> Unit) {
        sending.value = true
        viewModelScope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) { sendReplyAsync(query, content) }
            }.onFailure { Timber.e(it) }.getOrDefault(false)
            callback(ok)
            sending.value = false
        }
    }

    /** 走统一的 JsoupClient：共享 cookie jar、带会话失效检测、统一超时。 */
    private fun sendReplyAsync(query: String?, content: String): Boolean {
        Timber.d(content)
        if (query.isNullOrEmpty() || content.isBlank()) return false

        val queryMap = query.split("&")
            .mapNotNull { entry ->
                val pair = entry.split("=", limit = 2)
                if (pair[0].isNotBlank()) pair[0] to (pair.getOrNull(1) ?: "") else null
            }
            .toMap()

        val formDoc = JsoupClient.sendGetRequestWithQuery(query)
        val noticeauthormsg = formDoc.selectFirst("input[name=noticeauthormsg]")?.attr("value").orEmpty()
        val noticetrimstr = formDoc.selectFirst("input[name=noticetrimstr]")?.attr("value").orEmpty()
        val noticeauthor = formDoc.selectFirst("input[name=noticeauthor]")?.attr("value").orEmpty()
        val handlekey = formDoc.selectFirst("input[name=handlekey]")?.attr("value") ?: "reply"
        val usesig = formDoc.selectFirst("input[name=usesig]")?.attr("value") ?: "1"
        val reppid = formDoc.selectFirst("input[name=reppid]")?.attr("value").orEmpty()
        val reppost = formDoc.selectFirst("input[name=reppost]")?.attr("value").orEmpty()
        val formHash = formDoc.selectFirst("input[name=formhash]")?.attr("value").orEmpty()

        // 触发 checkpostrule，主要为了让站点维护 token；返回不是关键，沿用旧逻辑用 GET。
        JsoupClient.sendGetRequestWithUrl(CHECK_RULE_URL)

        val replyUrl = "${SERVER_BASE_URL}forum.php?mod=post&infloat=yes&action=reply" +
                "&fid=${queryMap["fid"]}&extra=${queryMap["extra"]}&tid=${queryMap["tid"]}" +
                "&replysubmit=yes&inajax=1"

        val response = JsoupClient.sendPostRequestWithUrl(
            replyUrl,
            mutableMapOf(
                "formhash" to formHash,
                "handlekey" to handlekey,
                "noticeauthor" to noticeauthor,
                "noticetrimstr" to noticetrimstr,
                "noticeauthormsg" to noticeauthormsg,
                "usesig" to usesig,
                "reppid" to reppid,
                "reppost" to reppost,
                "subject" to "",
                "message" to content
            )
        )
        return response.statusCode() in 200 until 300
    }
}
