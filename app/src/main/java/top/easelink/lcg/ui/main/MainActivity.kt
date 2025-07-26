package top.easelink.lcg.ui.main

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.navigation.NavigationBarView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
import top.easelink.lcg.ui.main.articles.view.ForumArticlesFragment
import top.easelink.lcg.ui.main.discover.view.DiscoverFragment
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
import java.util.EmptyStackException

class MainActivity : TopActivity(), NavigationBarView.OnItemSelectedListener {

    private var lastBackPressed = 0L
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.statusBarBackground.apply {
                layoutParams.height = systemBars.top
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            }

            binding.toolbar.updatePadding(top = 0)

            binding.fragmentContainer.updatePadding(
                top = 0,
                bottom = systemBars.bottom
            )

            (binding.bottomNavigation.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                bottomMargin = systemBars.bottom
            }

            insets
        }

        setupDrawer(binding.toolbar)
        setupBottomNavMenu()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mFragmentTags.size > 1) {
                    while (onFragmentDetached(mFragmentTags.pop())) {
                        syncBottomViewNavItemState()
                        return
                    }
                } else {
                    if (System.currentTimeMillis() - lastBackPressed > 2000) {
                        showMessage(R.string.app_exit_tip)
                        lastBackPressed = System.currentTimeMillis()
                    } else {
                        finishAffinity()
                    }
                }
            }
        })

        EventBus.getDefault().register(this)

        showFragment(RecommendFragment::class.java)
        setStatusBarAppearance(true)
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
        } catch (_: EmptyStackException) {
            view.selectedItemId = R.id.action_home
        } finally {
            view.setOnItemSelectedListener(this)
        }
    }

    private fun setStatusBarAppearance(isLight: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
        }
        binding.statusBarBackground.setBackgroundColor(
            if (isLight) ContextCompat.getColor(this, R.color.white)
            else ContextCompat.getColor(this, R.color.black)
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenArticleEvent(event: OpenArticleEvent) {
        if (AppConfig.articleShowInWebView) {
            WebViewActivity.startWebViewWith(SERVER_BASE_URL + event.url, this)
        } else {
            showFragment(ArticleFragment.newInstance(event.url))
        }
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

    private fun showFragmentWithTag(tag: String): Boolean {
        supportFragmentManager.findFragmentByTag(tag)?.let {
            if (popBackFragmentUntil(supportFragmentManager, tag)) {
                var d = mFragmentTags.search(tag)
                while (d > 1) {
                    mFragmentTags.pop()
                    d--
                }
                setStatusBarAppearance(
                    when (tag) {
                        RecommendFragment::class.java.simpleName,
                        MessageFragment::class.java.simpleName,
                        DiscoverFragment::class.java.simpleName,
                        MeFragment::class.java.simpleName,
                        ArticleFragment::class.java.simpleName -> true
                        else -> false
                    }
                )
                return true
            }
        }
        return false
    }

    private fun showFragment(clazz: Class<out Fragment>) {
        if (!showFragmentWithTag(clazz.simpleName)) {
            val fragment = clazz.getConstructor().newInstance() as Fragment
            addFragmentInActivity(
                supportFragmentManager,
                fragment,
                R.id.fragment_container
            )
            setStatusBarAppearance(
                when (clazz.simpleName) {
                    RecommendFragment::class.java.simpleName,
                    MessageFragment::class.java.simpleName,
                    DiscoverFragment::class.java.simpleName,
                    MeFragment::class.java.simpleName,
                    ArticleFragment::class.java.simpleName -> true
                    else -> false
                }
            )
        }
    }

    private fun showFragment(fragment: Fragment) {
        addFragmentInActivity(
            supportFragmentManager,
            fragment,
            R.id.fragment_container
        )
        setStatusBarAppearance(
            when (fragment.javaClass.simpleName) {
                RecommendFragment::class.java.simpleName,
                MessageFragment::class.java.simpleName,
                DiscoverFragment::class.java.simpleName,
                MeFragment::class.java.simpleName,
                ArticleFragment::class.java.simpleName -> true
                else -> false
            }
        )
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
                        binding.bottomNavigation.removeBadge(R.id.action_message)
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
}