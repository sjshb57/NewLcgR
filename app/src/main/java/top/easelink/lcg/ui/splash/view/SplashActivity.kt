package top.easelink.lcg.ui.splash.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(null)
        // 把启动期临时的 SplashTheme 切换回正式 AppTheme，让 status/nav bar
        // 颜色与主界面一致；这是原作者的写法，保留。
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(baseContext, MainActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }, 200)
    }
}
