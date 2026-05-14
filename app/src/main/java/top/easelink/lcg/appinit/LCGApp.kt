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
        // 注意：此处原本会调用 LCGCookieJar.syncToWebView()，把 jar 里的 cookie 推回
        // WebView 的 CookieManager。但这会导致一个隐藏 bug：如果 jar 里残留上一次失败
        // 登录留下的过期/无效 cookie，启动时被推进 WebView 后，下次访问 52pojie 时
        // WebView 带着无效 cookie 请求，触发 WAF 拒绝访问 waf_text_verify.html。
        // WebView 的 CookieManager 自身已会跨 app 重启持久化，不需要从 jar 回推。
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
