package top.easelink.lcg.ui.main

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationBarView
import com.tencent.upgrade.core.DefaultUpgradeStrategyRequestCallback
import com.tencent.upgrade.core.UpgradeManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.easelink.framework.threadpool.BackGroundPool
import top.easelink.framework.topbase.TopActivity
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.WRITE_EXTERNAL_CODE
import top.easelink.framework.utils.addFragmentInActivity
import top.easelink.framework.utils.popBackFragmentUntil
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.R
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.databinding.ActivityMainBinding
import top.easelink.lcg.ui.main.about.view.AboutFragment
import top.easelink.lcg.ui.main.article.view.ArticleFragment
import top.easelink.lcg.ui.main.articles.view.ForumArticlesFragment.Companion.newInstance
import top.easelink.lcg.ui.main.discover.view.DiscoverFragment
import top.easelink.lcg.ui.main.largeimg.view.LargeImageDialog
import top.easelink.lcg.ui.main.me.view.MeFragment
import top.easelink.lcg.ui.main.message.view.MessageFragment
import top.easelink.lcg.ui.main.model.NewMessageEvent
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.model.OpenForumEvent
import top.easelink.lcg.ui.main.model.OpenHalfWebViewFragmentEvent
import top.easelink.lcg.ui.main.model.OpenLargeImageViewEvent
import top.easelink.lcg.ui.main.recommand.view.RecommendFragment
import top.easelink.lcg.ui.setting.view.SettingActivity
import top.easelink.lcg.ui.webview.view.HalfScreenWebViewFragment
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.showMessage
import java.util.EmptyStackException
import kotlin.system.exitProcess

class MainActivity : TopActivity(), NavigationBarView.OnItemSelectedListener {
    private var lastBackPressed = 0L
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.statusBarBackground.layoutParams.height = getStatusBarHeight()
        binding.statusBarBackground.setBackgroundColor(resources.getColor(R.color.white, theme))


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.statusBarBackground.layoutParams.height = systemBars.top
            binding.statusBarBackground.requestLayout()

            (binding.bottomNavigation.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                bottomMargin = systemBars.bottom
            }
            binding.bottomNavigation.requestLayout()

            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })

        EventBus.getDefault().register(this)
        setupDrawer(binding.toolbar)
        setupBottomNavMenu()
        showFragment(RecommendFragment::class.java)
        setStatusBarIconColorForFragment(RecommendFragment::class.java.simpleName)

        GlobalScope.launch(BackGroundPool) {
            UpgradeManager.getInstance().checkUpgrade(false, null, DefaultUpgradeStrategyRequestCallback())
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun handleBackPressed() {
        if (mFragmentTags.isNotEmpty() && mFragmentTags.size > 1) {
            while (onFragmentDetached(mFragmentTags.pop())) {
                syncBottomViewNavItemState()
                return
            }
        }
        if (System.currentTimeMillis() - lastBackPressed > 2000) {
            Toast.makeText(this, R.string.app_exit_tip, Toast.LENGTH_SHORT).show()
            lastBackPressed = System.currentTimeMillis()
        } else {
            finish()
            exitProcess(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun setupDrawer(toolbar: Toolbar) {
        val header = layoutInflater.inflate(R.layout.nav_header, binding.navigationView, false)
        binding.navigationView.addHeaderView(header)
        binding.appVersion.text = BuildConfig.VERSION_NAME
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerView,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        binding.drawerView.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener { item ->
            binding.drawerView.closeDrawer(GravityCompat.START)
            handleNavigationItemSelected(item)
        }
    }

    private fun handleNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_item_about -> {
                showFragment(AboutFragment())
                true
            }
            R.id.nav_item_release -> {
                onMessageEvent(OpenArticleEvent(AppConfig.getAppReleaseUrl()))
                true
            }
            R.id.nav_item_portal -> {
                WebViewActivity.startWebViewWith(SERVER_BASE_URL, this)
                true
            }
            R.id.nav_item_setting -> {
                startActivity(Intent(this, SettingActivity::class.java))
                true
            }
            else -> false
        }
    }

    private fun setupBottomNavMenu() {
        binding.bottomNavigation.setOnItemSelectedListener(this)
    }

    private fun syncBottomViewNavItemState() {
        val view = binding.bottomNavigation
        try {
            view.setOnItemSelectedListener(null)
            when (mFragmentTags.peek()) {
                RecommendFragment::class.java.simpleName -> view.selectedItemId = R.id.action_home
                MessageFragment::class.java.simpleName -> view.selectedItemId = R.id.action_message
                DiscoverFragment::class.java.simpleName -> view.selectedItemId = R.id.action_forum_navigation
                MeFragment::class.java.simpleName -> view.selectedItemId = R.id.action_about_me
            }
        } catch (ese: EmptyStackException) {
            view.selectedItemId = R.id.action_home
        } finally {
            view.setOnItemSelectedListener(this)
        }
    }

    private fun setStatusBarIconColorForFragment(fragmentSimpleName: String) {
        val isLightStatusBar = when (fragmentSimpleName) {
            RecommendFragment::class.java.simpleName,
            MessageFragment::class.java.simpleName,
            DiscoverFragment::class.java.simpleName,
            MeFragment::class.java.simpleName,
            ArticleFragment::class.java.simpleName ->
                true
            else ->
                true
        }

        binding.statusBarBackground.setBackgroundColor(resources.getColor(R.color.white, theme))

        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = isLightStatusBar
        }
    }

    // EventBus 订阅方法
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenArticleEvent) {
        if (AppConfig.articleShowInWebView) {
            WebViewActivity.startWebViewWith(SERVER_BASE_URL + event.url, this)
        } else {
            showFragment(ArticleFragment.newInstance(event.url))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenForumEvent) {
        showFragment(newInstance(event.title, event.url, event.showTab))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: NewMessageEvent) {
        if (event.notificationInfo.isNotEmpty()) {
            showMessage(getString(R.string.notification_arrival))
            showBubbleView(PRIVATE_MESSAGE_POS)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowFragmentEvent(clazz: Class<TopFragment>) {
        showFragment(clazz)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenLargeImageViewEvent) {
        if (event.url.isNotEmpty()) {
            LargeImageDialog.newInstance(event.url).show(
                supportFragmentManager,
                LargeImageDialog::class.java.simpleName
            )
        } else {
            showMessage(R.string.tap_for_large_image_failed)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenHalfScreenWebViewEvent(event: OpenHalfWebViewFragmentEvent) {
        HalfScreenWebViewFragment.newInstance(event.html)
            .show(supportFragmentManager, HalfScreenWebViewFragment::class.simpleName)
    }

    private fun showBubbleView(pos: Int) {
        binding.bottomNavigation.getOrCreateBadge(bottomNavItemId(pos)).isVisible = true
    }

    private fun removeBubbleView(pos: Int) {
        binding.bottomNavigation.removeBadge(bottomNavItemId(pos))
    }

    private fun bottomNavItemId(pos: Int): Int {
        return when (pos) {
            0 -> R.id.action_home
            1 -> R.id.action_forum_navigation
            2 -> R.id.action_message
            3 -> R.id.action_about_me
            else -> R.id.action_home
        }
    }

    private fun showFragmentWithTag(tag: String): Boolean {
        supportFragmentManager.findFragmentByTag(tag)?.let {
            if (popBackFragmentUntil(supportFragmentManager, tag)) {
                var d = mFragmentTags.search(tag)
                while (d > 1) {
                    mFragmentTags.pop()
                    d--
                }
                setStatusBarIconColorForFragment(tag)
                return true
            }
        }
        return false
    }

    private fun showFragment(clazz: Class<*>) {
        if (!showFragmentWithTag(clazz.simpleName)) {
            val fragment = clazz.getConstructor().newInstance() as Fragment
            addFragmentInActivity(
                supportFragmentManager,
                fragment,
                R.id.fragment_container
            )
            setStatusBarIconColorForFragment(clazz.simpleName)
        }
    }

    private fun showFragment(fragment: Fragment) {
        addFragmentInActivity(
            supportFragmentManager,
            fragment,
            R.id.fragment_container
        )
        setStatusBarIconColorForFragment(fragment.javaClass.simpleName)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (binding.bottomNavigation.selectedItemId == item.itemId) {
            return false
        }

        return when (item.itemId) {
            R.id.action_message, R.id.action_forum_navigation, R.id.action_about_me -> {
                binding.toolbar.visibility = View.GONE
                when (item.itemId) {
                    R.id.action_message -> {
                        removeBubbleView(PRIVATE_MESSAGE_POS)
                        MessageFragment::class.java
                    }
                    R.id.action_forum_navigation -> DiscoverFragment::class.java
                    R.id.action_about_me -> MeFragment::class.java
                    else -> null
                }?.let {
                    showFragment(it)
                    true
                } ?: false
            }
            R.id.action_home -> {
                binding.toolbar.visibility = View.VISIBLE
                showFragment(RecommendFragment::class.java)
                true
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PERMISSION_GRANTED) {
                showMessage(R.string.permission_denied)
            }
        }
    }

    companion object {
        private const val PRIVATE_MESSAGE_POS = 2
    }
}