package top.easelink.lcg.ui.main.article.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.model.BlockException
import top.easelink.lcg.ui.main.source.remote.ArticlesRemoteDataSource

class PostPreviewViewModel : ViewModel() {

    val author = MutableLiveData<String>()
    val avatar = MutableLiveData<String>()
    val date = MutableLiveData<String>()
    val content = MutableLiveData<String>()
    val loadingResult = MutableLiveData<Int>()

    fun initUrl(query: String) {
        loadingResult.value = R.string.preview_loading
        viewModelScope.launch {
            try {
                val post = ArticlesRemoteDataSource.getPostPreview(query)
                if (post != null) {
                    post.let {
                        author.postValue(it.author)
                        avatar.postValue(it.avatar)
                        date.postValue(it.date)
                        content.postValue(it.content)
                    }
                    // -1 means loaded successfully
                    loadingResult.postValue(-1)
                } else {
                    loadingResult.postValue(R.string.preview_fail_info_post_deleted)
                }
            } catch (_: BlockException) {
                loadingResult.postValue(R.string.preview_fail_info_post_deleted)
            } catch (_: Exception) {
                loadingResult.postValue(R.string.preview_fail_info)
            }
        }
    }
}