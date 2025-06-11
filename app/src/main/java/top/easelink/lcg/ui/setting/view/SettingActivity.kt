package top.easelink.lcg.ui.setting.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.lifecycle.ViewModelProvider
import top.easelink.framework.topbase.TopActivity
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.databinding.ActivitySettingsBinding
import top.easelink.lcg.ui.main.login.view.LoginHintDialog
import top.easelink.lcg.ui.main.logout.view.LogoutHintDialog
import top.easelink.lcg.ui.setting.viewmodel.SettingViewModel
import top.easelink.lcg.utils.showMessage

class SettingActivity : TopActivity() {

    private lateinit var mViewModel: SettingViewModel
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
        setUp()
    }

    override fun onResume() {
        super.onResume()
        // 根据屏幕方向调整布局
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏可能需要特殊处理
        }
    }

    private fun setUp() {
        setupToolBar()
        setupComponents()
        setupObserver()
        mViewModel.init()
    }

    private fun setupToolBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupComponents() {
        // 账户状态相关UI设置
        if (!UserDataRepo.isLoggedIn) {
            with(binding) {
                syncFavoritesSwitch.isEnabled = false
                autoSignSwitch.isEnabled = false
                accountBtn.text = getString(R.string.login_btn)
                accountBtn.setOnClickListener {
                    LoginHintDialog().show(supportFragmentManager, null)
                }
            }
        } else {
            binding.accountBtn.text =
                String.format(getString(R.string.logout_confirm_message), UserDataRepo.username)
            binding.accountBtn.setOnClickListener {
                tryLogout()
            }
        }

        binding.apply {
            checkUpdateBtn.setOnClickListener {
                showMessage(R.string.app_is_latest)
            }

            syncFavoritesSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                mViewModel.scheduleJob(isChecked)
            }

            autoSignSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                mViewModel.setSyncFavorite(isChecked)
            }

            searchEngineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    AppConfig.defaultSearchEngine = position
                }
            }

            showSearchResultInWebview.setOnCheckedChangeListener { _, isChecked ->
                AppConfig.searchResultShowInWebView = isChecked
            }

            showArticleInWebview.setOnCheckedChangeListener { _, isChecked ->
                AppConfig.articleShowInWebView = isChecked
            }

            articleHandlePreTag.setOnCheckedChangeListener { _, isChecked ->
                AppConfig.articleHandlePreTag = isChecked
            }

            showRecommendFlag.setOnCheckedChangeListener { _, isChecked ->
                AppConfig.articleShowRecommendFlag = isChecked
            }
        }
    }

    private fun setupObserver() {
        // 使用lambda简化LiveData观察者
        mViewModel.apply {
            syncFavoriteEnable.observe(this@SettingActivity) { isEnabled ->
                binding.syncFavoritesSwitch.isChecked = (UserDataRepo.isLoggedIn && isEnabled)
            }

            autoSignInEnable.observe(this@SettingActivity) { isEnabled ->
                binding.autoSignSwitch.isChecked = (UserDataRepo.isLoggedIn && isEnabled)
            }

            searchEngineSelected.observe(this@SettingActivity) { position ->
                binding.searchEngineSpinner.setSelection(position, true)
            }

            openSearchResultInWebView.observe(this@SettingActivity) { isChecked ->
                binding.showSearchResultInWebview.isChecked = isChecked
            }

            openArticleInWebView.observe(this@SettingActivity) { isChecked ->
                binding.showArticleInWebview.isChecked = isChecked
            }

            showRecommendFlag.observe(this@SettingActivity) { isChecked ->
                binding.showRecommendFlag.isChecked = isChecked
            }

            handlePreTagInArticle.observe(this@SettingActivity) { isChecked ->
                binding.articleHandlePreTag.isChecked = isChecked
            }
        }
    }

    private fun tryLogout() {
        LogoutHintDialog(
            positive = {
                UserDataRepo.clearAll()
                showMessage(R.string.clear_cookie)
                finish()
            }
        ).show(supportFragmentManager, LogoutHintDialog::class.java.simpleName)
    }
}