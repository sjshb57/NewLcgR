package top.easelink.lcg.ui.search.source

import android.util.ArrayMap
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.lcg.R
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.search.model.LCGSearchResultItem
import top.easelink.lcg.ui.search.model.LCGSearchResults
import top.easelink.lcg.utils.WebsiteConstant
import top.easelink.lcg.utils.showMessage
import java.net.SocketTimeoutException

/**
 * author : junzhang
 * date   : 2020-02-09 16:22
 * desc   : Class for sending request and parse response
 */

object LCGSearchService {
    private const val MIN_REQUEST_INTERVAL = 5000L
    private var lastRequestTime: Long = 0L

    private var mNextPageUrl: String? = null
    private var mTotalResults: String? = null

    fun doSearchWith(keyword: String): LCGSearchResults {
        mNextPageUrl = null
        mTotalResults = null
        if (keyword.isBlank() || !canSendRequest()) {
            return emptyResult()
        }
        try {

            val url = "${WebsiteConstant.SERVER_BASE_URL}search.php?searchsubmit=yes"
            val form = ArrayMap<String, String>().apply {
                put("formhash", JsoupClient.formHash)
                put("mod", "forum")
                put("srchtype", "title")
                put("srhfid", "")
                put("srhlocality", "forum::index")
                put("srchtxt", keyword)
                put("searchsubmit", "true")
            }
            val response = JsoupClient.sendPostRequestWithUrl(url, form)
            if (response.statusCode() in 200..299) {
                val doc = response.parse()
                val result = parseSearchResults(doc)
                try {
                    // Use safe call for selectFirst and text
                    mTotalResults = doc.selectFirst("h2")?.text()
                    result.totalResult = mTotalResults
                } catch (e: Exception) {
                    Timber.w(e)
                }
                return result
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return emptyResult()
    }

    fun doSearchNextPage(): LCGSearchResults {
        if (mNextPageUrl.isNullOrEmpty()) {
            return emptyResult()
        }
        try {
            // Use non-null asserted call for mNextPageUrl as it's checked for nullability above
            return parseSearchResults(JsoupClient.sendGetRequestWithQuery(mNextPageUrl!!))
        } catch (e: SocketTimeoutException) {
            showMessage(R.string.network_error)
        } catch (e: Exception) {
            Timber.e(e)
            showMessage(R.string.error)
        }
        return emptyResult()
    }

    private fun parseSearchResults(doc: Document): LCGSearchResults {
        val results: List<LCGSearchResultItem> =
            doc.getElementsByClass("pbw").mapNotNull {
                try {
                    val spans = it.getElementsByTag("span")
                    val aNode = it.selectFirst("a")
                    val pNode = it.selectFirst("p.xg1")

                    // Check if aNode and pNode are not null before proceeding
                    if (aNode != null && pNode != null && spans.isNotEmpty() && spans.size == 3) {
                        return@mapNotNull LCGSearchResultItem(
                            title = aNode.html(),
                            url = aNode.attr("href"),
                            replyView = pNode.text(),
                            // Use safe call for nextElementSibling and provide empty string if null
                            content = pNode.nextElementSibling()?.html().orEmpty(),
                            author = spans[1].text(),
                            date = spans[0].text(),
                            forum = spans[2].text()
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
                null
            }
        return LCGSearchResults(results).also {
            mNextPageUrl = try {
                // Use safe call for selectFirst and attr
                doc.selectFirst("a.nxt")?.attr("href")
            } catch (e: Exception) {
                Timber.w(e)
                null
            }
        }
    }

    private fun canSendRequest(): Boolean {
        return if (System.currentTimeMillis() - lastRequestTime < MIN_REQUEST_INTERVAL) {
            showMessage(R.string.request_too_often_warning)
            false
        } else {
            lastRequestTime = System.currentTimeMillis()
            true
        }
    }

    private fun emptyResult(): LCGSearchResults {
        mNextPageUrl = null
        return LCGSearchResults(emptyList()).also {
            if (!mTotalResults.isNullOrEmpty()) {
                it.totalResult = mTotalResults
            }
        }
    }
}