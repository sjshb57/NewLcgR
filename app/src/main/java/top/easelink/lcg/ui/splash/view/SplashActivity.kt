package top.easelink.lcg.ui.splash.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import top.easelink.lcg.ui.main.MainActivity
import top.easelink.lcg.utils.ThemeHelper

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(null)
        // 原实现固定 setTheme(R.style.AppTheme)，无视用户的 MD3 偏好；改走 ThemeHelper
        // 与其它 Activity 一致。
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(baseContext, MainActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }, 200)
    }
}