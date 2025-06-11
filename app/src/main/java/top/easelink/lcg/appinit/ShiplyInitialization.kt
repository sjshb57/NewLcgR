package top.easelink.lcg.appinit

import android.content.Context
import android.os.Build
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.callback.Logger
import com.tencent.upgrade.core.UpgradeManager
import timber.log.Timber
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.account.UserDataRepo


object ShiplyInitialization {

    fun init(context: Context) {
        val config = UpgradeConfig.Builder()
            .appId(BuildConfig.SHIPLY_APP_ID)
            .appKey(BuildConfig.SHIPLY_APP_KEY)
            .isDebugPackage(BuildConfig.DEBUG)
            .apply {
                if (UserDataRepo.isLoggedIn) {
                    userId(UserDataRepo.username)
                }
            }
            .customLogger(object : Logger {
                override fun v(p0: String?, p1: String?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串
                    Timber.tag(p0 ?: "").v(p1 ?: "")
                }

                override fun v(p0: String?, p1: String?, p2: Throwable?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串。p1优先，若p1也为null则尝试p2?.message
                    Timber.tag(p0 ?: "").v(p2, p1 ?: p2?.message ?: "")
                }

                override fun d(p0: String?, p1: String?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串
                    Timber.tag(p0 ?: "").d(p1 ?: "")
                }

                override fun d(p0: String?, p1: String?, p2: Throwable?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串。p1优先，若p1也为null则尝试p2?.message
                    Timber.tag(p0 ?: "").d(p2, p1 ?: p2?.message ?: "")
                }

                override fun i(p0: String?, p1: String?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串
                    Timber.tag(p0 ?: "").i(p1 ?: "")
                }

                override fun i(p0: String?, p1: String?, p2: Throwable?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串。p1优先，若p1也为null则尝试p2?.message
                    Timber.tag(p0 ?: "").i(p2, p1 ?: p2?.message ?: "")
                }

                override fun w(p0: String?, p1: String?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串
                    Timber.tag(p0 ?: "").w(p1 ?: "")
                }

                override fun w(p0: String?, p1: String?, p2: Throwable?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串。p1优先，若p1也为null则尝试p2?.message
                    Timber.tag(p0 ?: "").w(p2, p1 ?: p2?.message ?: "")
                }

                override fun e(p0: String?, p1: String?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串
                    Timber.tag(p0 ?: "").e(p1 ?: "")
                }

                override fun e(p0: String?, p1: String?, p2: Throwable?) {
                    // 修改：p0 和 p1 可能为 null，提供默认空字符串。p1优先，若p1也为null则尝试p2?.message
                    Timber.tag(p0 ?: "").e(p2, p1 ?: p2?.message ?: "")
                }

            })
            .systemVersion(Build.VERSION.SDK_INT.toString())
            .build()
        UpgradeManager.getInstance().init(context, config)
    }
}