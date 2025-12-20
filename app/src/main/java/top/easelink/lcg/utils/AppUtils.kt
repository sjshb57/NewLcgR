package top.easelink.lcg.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager
import android.webkit.WebSettings
import top.easelink.lcg.R

fun isApplicationAvailable(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

const val WECHAT_PACKAGE_NAME = "com.tencent.mm"

fun startWeChat(context: Context) {
    if (isApplicationAvailable(context, WECHAT_PACKAGE_NAME)) {
        context.startActivity(Intent(ACTION_MAIN).apply {
            addCategory(CATEGORY_LAUNCHER)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            component = ComponentName(WECHAT_PACKAGE_NAME, "com.tencent.mm.ui.LauncherUI")
        })
    } else {
        showMessage(R.string.install_wechat_tips)
    }
}

fun getScreenWidthDp(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}
fun getScreenHeightDp(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return (displayMetrics.heightPixels / displayMetrics.density).toInt()
}

/**
 * Get the device's real User-Agent
 *
 */
fun getDeviceUserAgent(context: Context): String {
    // 显示UA信息
    // showMessage(WebSettings.getDefaultUserAgent(context))
    return WebSettings.getDefaultUserAgent(context)
}

fun getScreenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels
fun getScreenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels