package top.easelink.lcg.ui.setting.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.service.work.SignInWorker

class SettingViewModel(application: Application) : AndroidViewModel(application) {
    val autoSignInEnable = MutableLiveData<Boolean>()
    val syncFavoriteEnable = MutableLiveData<Boolean>()
    val searchEngineSelected = MutableLiveData<Int>()
    val openSearchResultInWebView = MutableLiveData<Boolean>()
    val openArticleInWebView = MutableLiveData<Boolean>()
    val handlePreTagInArticle = MutableLiveData<Boolean>()
    val showRecommendFlag = MutableLiveData<Boolean>()

    fun init() {
        with(AppConfig) {
            autoSignInEnable.value = autoSignEnable
            syncFavoriteEnable.value = syncFavorites
            searchEngineSelected.value = defaultSearchEngine
            openSearchResultInWebView.value = searchResultShowInWebView
            openArticleInWebView.value = articleShowInWebView
            showRecommendFlag.value = articleShowRecommendFlag
            handlePreTagInArticle.value = articleHandlePreTag
        }
    }

    fun scheduleJob(enable: Boolean) {
        AppConfig.autoSignEnable = enable
        val context = getApplication<Application>().applicationContext
        if (enable) {
            SignInWorker.startSignInWork(context)
        } else {
            WorkManager.getInstance(context).cancelAllWorkByTag(SignInWorker.TAG)
        }
    }

    fun setSyncFavorite(enable: Boolean) {
        AppConfig.syncFavorites = enable
    }
}