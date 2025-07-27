package top.easelink.lcg.ui.main.history.model

data class HistoryModel(
    val title: String,
    val author: String,
    val url: String,
    val timeStamp: Long,
    val content: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HistoryModel
        return url == other.url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}