package top.easelink.framework.guard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import timber.log.Timber
import top.easelink.framework.BuildConfig

class AppGuard(
    private var mContext: Context,
    private val mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!BuildConfig.DEBUG) {
            restartApp(mContext)
            mDefaultUncaughtExceptionHandler?.uncaughtException(t, e)
            Process.killProcess(Process.myPid())
        }
    }

    private fun restartApp(context: Context) {
        context.runCatching {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES
            }

            packageManager
                .getPackageInfo(context.packageName, flags)
                .activities
                ?.getOrNull(0)
                ?.let {
                    val intent = Intent().apply {
                        setClassName(context.packageName, it.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
        }.getOrElse {
            Timber.e(it)
        }
    }
}