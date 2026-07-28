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
 * 系统 WebView 默认 UA，去掉 " wv)" 标记，让 Jsoup/OkHttp 与 WebView 用同一个 UA。
 *
 * 注意：这不是过盾手段。实测带/不带 wv 请求首页结果完全一致，
 * 未复现"strip wv 能绕过知道创宇盾"的说法。登录死循环别再往这个方向查。
 */
fun getDeviceUserAgent(context: Context): String {
    return WebSettings.getDefaultUserAgent(context)
        .replace("; wv) ", ") ")
        .replace(" wv) ", ") ")
}

fun getScreenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels
fun getScreenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels