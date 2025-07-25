package top.easelink.lcg.ui.main.message.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.framework.threadpool.IOPool
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.main.model.BaseNotification
import top.easelink.lcg.ui.main.model.NotificationModel
import top.easelink.lcg.ui.main.model.SystemNotification
import top.easelink.lcg.utils.WebsiteConstant.NOTIFICATION_HOME_QUERY

class NotificationViewModel : ViewModel() {

    val notifications = MutableLiveData<NotificationModel>()
    val isLoading = MutableLiveData<Boolean>()
    var nextPageUrl = ""

    fun fetchMoreNotifications(callback: (Boolean) -> Unit) {
        if (nextPageUrl.isEmpty()) {
            callback.invoke(false)
            return
        }
        GlobalScope.launch(IOPool) {
            try {
                JsoupClient.sendGetRequestWithQuery(nextPageUrl).let {
                    val model = parseResponse(it)
                    notifications.postValue(model)
                }
            } catch (e: Exception) {
                Timber.e(e)
                callback.invoke(false)
            }
        }
    }

    fun fetchNotifications() {
        GlobalScope.launch(IOPool) {
            isLoading.postValue(true)
            try {
                JsoupClient.sendGetRequestWithQuery(NOTIFICATION_HOME_QUERY).let {
                    notifications.postValue(parseResponse(it))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            isLoading.postValue(false)
        }
    }

    private fun parseSystemResponse(doc: Document): List<SystemNotification> {
        doc.apply {
            val dateTimeList = select("span.xg1").map {
                it.text()
            }
            return select("dl.cl").mapIndexed { index, element ->
                val title = element.selectFirst("dd.ntc_body")?.html().orEmpty()
                val dateTime = dateTimeList.getOrNull(index).orEmpty()

                SystemNotification(
                    title = title,
                    dateTime = dateTime
                )
            }
        }
    }

    private fun parseResponse(doc: Document): NotificationModel {
        val notifications = doc.select("dl.cl").mapNotNull { element ->
            try {
                val ntc = element.selectFirst("dd.ntc_body")
                val avatarElement = element.selectFirst("img")
                val dateTimeElement = element.selectFirst("span.xg1")

                if (ntc == null || avatarElement == null || dateTimeElement == null) {
                    return@mapNotNull null
                }

                ntc.getElementsByTag("a").forEach {
                    val href = it.attr("href")
                    if (href.isNotEmpty()) {
                        it.attr("href", "lcg:$href")
                    }
                }

                BaseNotification(
                    avatar = avatarElement.attr("src"),
                    content = ntc.html(),
                    dateTime = dateTimeElement.text()
                )
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
        nextPageUrl = doc.select("a.nxt")?.attr("href").orEmpty()
        return NotificationModel(notifications, nextPageUrl)
    }
}