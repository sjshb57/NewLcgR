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
 * 取系统 WebView 的默认 UA。
 *
 * 注意：WebSettings.getDefaultUserAgent 返回的 UA 末尾带 " wv)" 标记
 * （Android WebView 标识符），有些 WAF 会用这个标识做"可疑流量"评分。
 * 出于防御性目的，对 Jsoup/OkHttp 这种纯后台请求去掉 wv 标记，让请求
 * 看起来像普通 Chrome on Android。WebView 自己用的 UA 不动（webview
 * 本来就是 webview，伪装也没意义）。
 */
fun getDeviceUserAgent(context: Context): String {
    return WebSettings.getDefaultUserAgent(context)
        .replace("; wv) ", ") ")
        .replace(" wv) ", ") ")
}

fun getScreenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels
fun getScreenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels