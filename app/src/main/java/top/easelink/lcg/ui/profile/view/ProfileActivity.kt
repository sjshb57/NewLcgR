package top.easelink.lcg.ui.profile.view

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import top.easelink.framework.topbase.TopActivity
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.addFragmentInActivity
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ActivityProfileBinding
import top.easelink.lcg.ui.profile.viewmodel.ProfileViewModel

const val KEY_PROFILE_URL = "profile_url"

class ProfileActivity : TopActivity() {

    private lateinit var mViewModel: ProfileViewModel
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUp()
    }

    private fun setUp() {
        setupToolBar()
        // 修复 Argument type mismatch 错误：
        // 1. 使用 ?.let {} 安全地处理可能为 null 的 StringExtra，并只在不为 null 时创建 Fragment。
        // 2. 或者，如果 ProfileFragment.newInstance 确实只接受非null String，可以提供一个默认值（例如空字符串 ""）。
        // 这里采用提供默认空字符串的方式，假设 ProfileFragment可以处理空字符串。
        val profileUrl = intent.getStringExtra(KEY_PROFILE_URL) ?: ""
        showFragment(ProfileFragment.newInstance(profileUrl))
    }

    private fun showFragment(fragment: TopFragment) {
        addFragmentInActivity(
            supportFragmentManager,
            fragment,
            R.id.profile_root // This ID should be correct as it's defined in your layout
        )
    }

    private fun setupToolBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
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
}