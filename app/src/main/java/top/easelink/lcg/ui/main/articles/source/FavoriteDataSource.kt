package top.easelink.lcg.ui.main.articles.source

import androidx.annotation.WorkerThread
import org.jsoup.nodes.Document
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.main.source.model.ArticleEntity
import top.easelink.lcg.utils.WebsiteConstant.GET_FAVORITE_QUERY
import top.easelink.lcg.utils.toTimeStamp

object FavoriteDataSource {
    @WorkerThread
    fun getAllRemoteFavorites(): List<ArticleEntity> {
        val favorites = mutableListOf<ArticleEntity>()
        var nextPageUrl: String? = GET_FAVORITE_QUERY
        while (nextPageUrl != null) {
            val doc = JsoupClient.sendGetRequestWithQuery(nextPageUrl)
            favorites.addAll(parseFavorites(doc))
            nextPageUrl = doc.selectFirst("a.nxt")?.attr("href")
        }
        return favorites
    }

    private fun parseFavorites(doc: Document): List<ArticleEntity> {
        return doc.getElementById("favorite_ul")
            ?.children()
            ?.mapNotNull {
                val delUrl = it.selectFirst("a.y")?.attr("href").orEmpty()
                val dateAdded = it.selectFirst("span.xg1")?.text().orEmpty()

                val targetElement = it.getElementsByAttribute("target").firstOrNull()

                if (targetElement != null) {
                    val title = targetElement.text().orEmpty()
                    val articleUrl = targetElement.attr("href").orEmpty()
                    ArticleEntity(
                        title = title,
                        author = "",
                        url = articleUrl,
                        content = "",
                        timestamp = toTimeStamp(dateAdded),
                        delUrl = delUrl
                    )
                } else {
                    null
                }
            }.orEmpty()
    }
}