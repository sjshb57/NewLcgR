package top.easelink.lcg.ui.main.article.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.source.remote.ArticlesRemoteDataSource

class PostPreviewViewModel : ViewModel() {

    private val _author = MutableLiveData<String>()
    private val _avatar = MutableLiveData<String>()
    private val _date = MutableLiveData<String>()
    private val _content = MutableLiveData<String>()
    private val _loadingResult = MutableLiveData<Int>()

    val author: MutableLiveData<String> get() = _author
    val avatar: MutableLiveData<String> get() = _avatar
    val date: MutableLiveData<String> get() = _date
    val content: MutableLiveData<String> get() = _content
    val loadingResult: MutableLiveData<Int> get() = _loadingResult

    private var currentQuery: String? = null
    private var loadJob: Job? = null

    fun initUrl(query: String) {
        currentQuery = query
        loadJob?.cancel() // 取消之前的加载任务
        _loadingResult.value = R.string.preview_loading

        loadJob = viewModelScope.launch {
            try {
                val post = withContext(Dispatchers.IO) {
                    ArticlesRemoteDataSource.getPostPreview(query)
                }

                post?.let {
                    _author.postValue(it.author)
                    _avatar.postValue(it.avatar)
                    _date.postValue(it.date)
                    _content.postValue(it.content)
                    _loadingResult.postValue(-1) // -1表示加载成功
                } ?: run {
                    _loadingResult.postValue(R.string.preview_fail_info)
                }
            } catch (_: Exception) {
                _loadingResult.postValue(R.string.preview_fail_info)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}