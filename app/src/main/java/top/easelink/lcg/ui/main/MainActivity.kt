package top.easelink.lcg.ui.main

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.google.android.material.navigation.NavigationBarView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.easelink.framework.topbase.TopActivity
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.WRITE_EXTERNAL_CODE
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.R
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.databinding.ActivityMainBinding
import top.easelink.lcg.ui.main.about.view.AboutFragment
import top.easelink.lcg.ui.main.article.view.ArticleFragment
import top.easelink.lcg.ui.main.articles.view.FavoriteArticlesFragment
import top.easelink.lcg.ui.main.articles.view.ForumArticlesFragment
import top.easelink.lcg.ui.main.discover.view.DiscoverFragment
import top.easelink.lcg.ui.main.forumnav.view.ForumNavigationFragment
import top.easelink.lcg.ui.main.largeimg.view.LargeImageDialog
import top.easelink.lcg.ui.main.me.view.MeFragment
import top.easelink.lcg.ui.main.message.view.MessageFragment
import top.easelink.lcg.ui.main.recommand.view.RecommendFragment
import top.easelink.lcg.ui.main.model.*
import top.easelink.lcg.ui.setting.view.SettingActivity
import top.easelink.lcg.ui.webview.view.HalfScreenWebViewFragment
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.showMessage

class MainActivity : TopActivity(), NavigationBarView.OnItemSelectedListener {

    private var lastBackPressed = 0L
    private lateinit var binding: ActivityMainBinding
    private var currentTabId: Int = R.id.action_home
    private val fragmentTags = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupDrawer(binding.toolbar)
        setupBottomNavMenu()
        setupBackPressHandler()
        setupFragmentBackStackListener()

        EventBus.getDefault().register(this)

        if (savedInstanceState == null) {
            showInitialFragment()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.statusBarBackground.apply {
                layoutParams.height = systemBars.top
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.lcg_surface))
            }

            binding.toolbar.updatePadding(top = 0)
            binding.fragmentContainer.updatePadding(bottom = systemBars.bottom)

            // BottomNav 用 bottomMargin 抬到系统栏之上，配合 navigationBarColor=lcg_surface
            // 让系统导航区域和 nav 颜色一致，视觉上"贴底"。padding 方式会让图标位置上移，
            // 改回 margin 让图标保持垂直居中。
            (binding.bottomNavigation.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.apply {
                if (bottomMargin != systemBars.bottom) {
                    bottomMargin = systemBars.bottom
                    binding.bottomNavigation.requestLayout()
                }
            }

            insets
        }
    }

    private fun showInitialFragment() {
        showFragment(RecommendFragment::class.java)
        setStatusBarAppearance(true)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else if (System.currentTimeMillis() - lastBackPressed > 2000) {
                    showMessage(R.string.app_exit_tip)
                    lastBackPressed = System.currentTimeMillis()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupFragmentBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val backStackCount = supportFragmentManager.backStackEntryCount
            if (backStackCount == 0) {
                showBottomNavigation()
                // 直接恢复当前tab对应的Fragment，而不是依赖查找第一个可见Fragment
                restoreCurrentTabFragment()
            } else {
                // Check if current top fragment is ArticleFragment or not
                val fragments = supportFragmentManager.fragments
                for (fragment in fragments) {
                    if (fragment.isVisible) {
                        if (shouldShowBottomNavigation(fragment)) {
                            showBottomNavigation()
                        } else {
                            hideBottomNavigation()
                        }
                        break
                    }
                }
            }
        }
    }

    private fun shouldShowBottomNavigation(fragment: Fragment): Boolean {
        return fragment is RecommendFragment
                || fragment is MessageFragment
                || fragment is DiscoverFragment
                || fragment is MeFragment
                || fragment is ForumArticlesFragment
                || fragment is ForumNavigationFragment
                || fragment is FavoriteArticlesFragment
    }

    private fun restoreCurrentTabFragment() {
        // 确保清除所有可能的回退栈状态
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        
        // 根据当前tabId重新创建并显示正确的Fragment
        when (currentTabId) {
            R.id.action_home -> showFragment(RecommendFragment::class.java)
            R.id.action_message -> showFragment(MessageFragment::class.java)
            R.id.action_forum_navigation -> showFragment(DiscoverFragment::class.java)
            R.id.action_about_me -> showFragment(MeFragment::class.java)
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

        ActionBarDrawerToggle(
            this,
            binding.drawerView,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        ).apply {
            binding.drawerView.addDrawerListener(this)
            syncState()
        }

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
                onOpenArticleEvent(OpenArticleEvent(AppConfig.getAppReleaseUrl()))
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

    private fun showFragment(clazz: Class<out Fragment>) {
        val tag = clazz.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        val fragment = existingFragment ?: clazz.getConstructor().newInstance()

        supportFragmentManager.commit {
            supportFragmentManager.fragments.forEach {
                if (it != fragment) hide(it)
            }

            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(R.id.fragment_container, fragment, tag)
                if (fragment is ForumArticlesFragment || fragment is ForumNavigationFragment) {
                    addToBackStack(tag)
                }
            }

            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
        }

        updateUIForFragment(fragment)
        updateFragmentTags(tag)
    }

    private fun showFragment(fragment: Fragment) {
        val tag = fragment.javaClass.simpleName
        supportFragmentManager.commit {
            supportFragmentManager.fragments.forEach {
                if (it != fragment) hide(it)
            }

            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(R.id.fragment_container, fragment, tag)
                if (fragment is ForumArticlesFragment || fragment is ForumNavigationFragment) {
                    addToBackStack(tag)
                }
            }

            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
        }

        updateUIForFragment(fragment)
        updateFragmentTags(tag)
    }

    private fun updateUIForFragment(fragment: Fragment) {
        val isLightStatusBar = when (fragment) {
            is RecommendFragment,
            is MessageFragment,
            is DiscoverFragment,
            is MeFragment,
            is ArticleFragment,
            is ForumArticlesFragment,
            is FavoriteArticlesFragment -> true
            else -> false
        }
        setStatusBarAppearance(isLightStatusBar)

        binding.toolbar.visibility = when (fragment) {
            is RecommendFragment -> View.VISIBLE
            else -> View.GONE
        }
        
        // MessageFragment doesn't need auto-hide bottom navigation
        if (fragment is MessageFragment) {
            toggleBottomNavScrollBehavior(false)
        } else {
            toggleBottomNavScrollBehavior(true)
        }

        syncBottomNavigation()
    }

    private fun toggleBottomNavScrollBehavior(enable: Boolean) {
        val layoutParams = binding.bottomNavigation.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        if (enable) {
            if (layoutParams?.behavior == null) {
                layoutParams?.behavior = com.google.android.material.behavior.HideBottomViewOnScrollBehavior<View>()
            }
        } else {
            if (layoutParams?.behavior != null) {
                layoutParams?.behavior = null
                // Ensure it is visible when behavior is removed
                binding.bottomNavigation.translationY = 0f
            }
        }
    }

    private fun syncBottomNavigation() {
        binding.bottomNavigation.post {
            binding.bottomNavigation.setOnItemSelectedListener(null)
            binding.bottomNavigation.selectedItemId = currentTabId
            binding.bottomNavigation.setOnItemSelectedListener(this@MainActivity)
        }
    }

    private fun updateFragmentTags(tag: String) {
        if (!fragmentTags.contains(tag)) {
            fragmentTags.add(tag)
        }
    }

    fun hideBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { view, insets ->
            view.updatePadding(
                bottom = 0
            )
            insets
        }
    }

    fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
        val layoutParams = binding.bottomNavigation.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        val behavior = layoutParams?.behavior
        if (behavior is com.google.android.material.behavior.HideBottomViewOnScrollBehavior) {
            behavior.slideUp(binding.bottomNavigation)
        } else {
            binding.bottomNavigation.translationY = 0f
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun setStatusBarAppearance(isLight: Boolean) {
        // isLight 指"页面背景偏亮，状态栏图标应该用深色"。day mode 通常是 true，
        // night mode 系统会强制翻转，所以这里直接读 lcg_surface 资源即可，
        // 资源系统会按 night-qualifier 自动选对应版本。
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
        }
        binding.statusBarBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.lcg_surface)
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenArticleEvent(event: OpenArticleEvent) {
        if (AppConfig.articleShowInWebView) {
            WebViewActivity.startWebViewWith(SERVER_BASE_URL + event.url, this)
        } else {
            showArticleFragment(ArticleFragment.newInstance(event.url))
        }
    }

    private fun showArticleFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            // 隐藏所有Fragment，确保只有目标Fragment可见
            supportFragmentManager.fragments.forEach {
                hide(it)
            }
            add(R.id.fragment_container, fragment, fragment.javaClass.simpleName)
            addToBackStack(null)
            setReorderingAllowed(true)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        }
        hideBottomNavigation()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenForumEvent(event: OpenForumEvent) {
        showFragment(ForumArticlesFragment.newInstance(event.title, event.url, event.showTab))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewMessageEvent(event: NewMessageEvent) {
        if (event.notificationInfo.isNotEmpty()) {
            showMessage(getString(R.string.notification_arrival))
            binding.bottomNavigation.getOrCreateBadge(R.id.action_message).isVisible = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowFragmentEvent(clazz: Class<out TopFragment>) {
        showFragment(clazz)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenLargeImageViewEvent(event: OpenLargeImageViewEvent) {
        if (event.url.isNotEmpty()) {
            LargeImageDialog.newInstance(event.url).show(
                supportFragmentManager,
                LargeImageDialog::class.java.simpleName
            )
        } else {
            showMessage(R.string.tap_for_large_image_failed)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenHalfScreenWebViewEvent(event: OpenHalfWebViewFragmentEvent) {
        HalfScreenWebViewFragment.newInstance(event.html)
            .show(supportFragmentManager, HalfScreenWebViewFragment::class.java.simpleName)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSessionExpired(event: SessionExpiredEvent) {
        showMessage(R.string.session_expired_tips)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (binding.bottomNavigation.selectedItemId == item.itemId) {
            return false
        }

        // 清除回退栈，确保从新的tab开始
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        currentTabId = item.itemId

        return when (item.itemId) {
            R.id.action_message -> {
                binding.bottomNavigation.removeBadge(R.id.action_message)
                showFragment(MessageFragment::class.java)
                true
            }
            R.id.action_forum_navigation -> {
                showFragment(DiscoverFragment::class.java)
                true
            }
            R.id.action_about_me -> {
                showFragment(MeFragment::class.java)
                true
            }
            R.id.action_home -> {
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
}