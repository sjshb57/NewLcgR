package top.easelink.lcg.appinit

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import top.easelink.framework.guard.AppGuardStarter
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.network.LCGCookieJar
import top.easelink.lcg.service.work.SignInWorker
import top.easelink.lcg.ui.setting.viewmodel.SettingViewModel

class LCGApp : Application() {
    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(applicationJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        // 在所有 Activity 创建之前应用用户选择的暗夜模式偏好。
        AppCompatDelegate.setDefaultNightMode(
            SettingViewModel.toDelegateMode(AppConfig.nightMode)
        )
        initCoil()
        AppGuardStarter.init(this)
        ShiplyInitialization.init(this@LCGApp)
        // 让 WebView 与原生网络栈共用同一份 cookie，避免重启后 WebView "看着已登出"。
        applicationScope.launch { LCGCookieJar.syncToWebView() }
        trySignIn()
        CacheCleanerTask.clearCachesIfNeeded()
    }

    private fun trySignIn() {
        applicationScope.launch {
            if (!AppConfig.autoSignEnable || !UserDataRepo.isLoggedIn) return@launch
            // 与 SignInWorker 去重：如 7 小时内已成功签过，不再触发，避免冷启动 + Worker 双跑。
            if (SignInWorker.isRecentlyExecuted()) {
                Timber.d("trySignIn skipped: recently executed")
                return@launch
            }
            delay(2000)
            try {
                SignInWorker.sendSignInRequest()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationJob.cancel()
    }

    companion object {
        lateinit var instance: LCGApp
            private set

        @JvmStatic
        val context: Context
            get() = instance
    }
}
