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
 * 取系统 WebView 的默认 UA,并去掉 Android WebView 的 " wv)" 标记。
 *
 * WebSettings.getDefaultUserAgent 返回的 UA 末尾带 " wv)" 标识符,
 * 知道创宇盾 / Cloudflare 等 WAF 会读 navigator.userAgent,看到 wv 就把请求
 * 评分为"可疑流量",触发更严格的挑战(环境检查页死循环 / 滑块验证拒绝放行)。
 *
 * 实地观测:52pojie 进入登录页时,带 wv 的 UA 会被卡在"浏览器环境检查中"自我
 * 刷新循环;strip 之后服务端把 WebView 当成普通 Chrome on Android,正常下发
 * wzws_cid 并直接渲染表单。
 *
 * 这个工具同时被 Jsoup/OkHttp(后台请求)和 WebView(webSettings.userAgentString)
 * 共用,确保前后端 UA 一致——避免出现 WAF 把两种流量判成不同来源的情况。
 */
fun getDeviceUserAgent(context: Context): String {
    return WebSettings.getDefaultUserAgent(context)
        .replace("; wv) ", ") ")
        .replace(" wv) ", ") ")
}

fun getScreenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels
fun getScreenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels