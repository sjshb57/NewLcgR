package top.easelink.lcg.ui.setting.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
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
    val nightModeSelected = MutableLiveData<Int>()

    fun init() {
        with(AppConfig) {
            autoSignInEnable.value = autoSignEnable
            syncFavoriteEnable.value = syncFavorites
            searchEngineSelected.value = defaultSearchEngine
            openSearchResultInWebView.value = searchResultShowInWebView
            openArticleInWebView.value = articleShowInWebView
            showRecommendFlag.value = articleShowRecommendFlag
            handlePreTagInArticle.value = articleHandlePreTag
            nightModeSelected.value = nightMode
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

    /**
     * 持久化暗夜模式偏好并立刻通过 AppCompatDelegate 应用（系统会自动 recreate 所有 Activity）。
     * mode 与 AppConfig.NIGHT_MODE_* 常量对齐。
     */
    fun setNightMode(mode: Int) {
        if (mode == AppConfig.nightMode) return
        AppConfig.nightMode = mode
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(mode))
    }

    companion object {
        fun toDelegateMode(mode: Int): Int = when (mode) {
            AppConfig.NIGHT_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppConfig.NIGHT_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
