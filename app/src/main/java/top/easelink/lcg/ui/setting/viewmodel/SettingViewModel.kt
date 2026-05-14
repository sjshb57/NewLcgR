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

    // 注意命名与 AppConfig.materialDesign3Enabled 区分，避免 with(AppConfig) 块里
    // unqualified 解析时拿到 AppConfig 的 Boolean 字段而不是 LiveData，导致 .value
    // 调用编译失败（其他 LiveData 都遵循"与 AppConfig 字段不同名"的约定）。
    val useMaterialDesign3 = MutableLiveData<Boolean>()

    fun init() {
        with(AppConfig) {
            autoSignInEnable.value = autoSignEnable
            syncFavoriteEnable.value = syncFavorites
            searchEngineSelected.value = defaultSearchEngine
            openSearchResultInWebView.value = searchResultShowInWebView
            openArticleInWebView.value = articleShowInWebView
            showRecommendFlag.value = articleShowRecommendFlag
            handlePreTagInArticle.value = articleHandlePreTag
            useMaterialDesign3.value = materialDesign3Enabled
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

    /** 仅持久化偏好；Activity 重建由 SettingActivity 控制时机。 */
    fun setMaterialDesign3(enable: Boolean) {
        AppConfig.materialDesign3Enabled = enable
    }
}