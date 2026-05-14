package top.easelink.lcg.utils

import android.app.Activity
import top.easelink.lcg.R
import top.easelink.lcg.config.AppConfig

/**
 * 主题切换助手：根据用户在 Settings 里选的"Material Design 3"开关，
 * 决定当前 Activity 应用哪个主题。
 *
 * 必须在 [Activity.onCreate] 的 super.onCreate(...) **之前** 调用，
 * 否则 setTheme 不会影响 view inflation。继承 TopActivity 的页面已在基类里挂好；
 * 直接继承 AppCompatActivity 的（WebViewActivity / SplashActivity）需要自行调用。
 *
 * 切换后立即生效靠 [Activity.recreate]；其它已存在的 Activity 在它们下一次
 * onCreate 时自动应用新主题，无需额外通信。
 */
object ThemeHelper {

    fun applyTheme(activity: Activity) {
        val themeResId = if (AppConfig.materialDesign3Enabled) {
            R.style.Theme_LCG_Material3
        } else {
            R.style.AppTheme
        }
        activity.setTheme(themeResId)
    }
}
