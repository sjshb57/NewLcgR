package top.easelink.lcg.appinit

import android.app.Application
import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import top.easelink.framework.guard.AppGuardStarter
import top.easelink.framework.threadpool.BackGroundPool
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.service.work.SignInWorker

class LCGApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 初始化日志系统
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        initCoil()
        AppGuardStarter.init(this)
        ShiplyInitialization.init(this@LCGApp)
        trySignIn()
        CacheCleanerTask.clearCachesIfNeeded()
    }

    private fun trySignIn() = GlobalScope.launch(BackGroundPool) {
        if (AppConfig.autoSignEnable && UserDataRepo.isLoggedIn) {
            delay(2000)
            try {
                SignInWorker.sendSignInRequest()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    companion object {
        lateinit var instance: LCGApp
            private set

        @JvmStatic
        val context: Context
            get() = instance
    }
}