package top.easelink.lcg.ui.main.discover.viewmodel

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.easelink.framework.threadpool.IOPool
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.discover.model.DiscoverModel
import top.easelink.lcg.ui.main.discover.model.ForumListModel
import top.easelink.lcg.ui.main.discover.model.generateAllForums
import top.easelink.lcg.ui.main.discover.source.DateType
import top.easelink.lcg.ui.main.discover.source.RankType
import top.easelink.lcg.ui.main.discover.source.fetchRank
import top.easelink.lcg.utils.showMessage
import java.net.SocketTimeoutException

class DiscoverViewModel : ViewModel() {
    val aggregationModels = MutableLiveData<MutableList<DiscoverModel>>()

    @MainThread
    fun initOptions(context: Context) {
        aggregationModels.value = mutableListOf(ForumListModel(generateAllForums(context)))
        viewModelScope.launch(IOPool) {
            runCatching {
                fetchRank(RankType.HEAT, DateType.TODAY).let { ranks ->
                    aggregationModels.value?.let {
                        it.add(ranks)
                        aggregationModels.postValue(it)
                    }
                }
            }.getOrElse {
                when (it) {
                    is SocketTimeoutException -> showMessage(R.string.network_error)
                    else -> showMessage(R.string.error)
                }
            }
        }
    }
}