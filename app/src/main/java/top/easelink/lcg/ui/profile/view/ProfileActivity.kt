package top.easelink.lcg.ui.profile.view

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import top.easelink.framework.topbase.TopActivity
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.addFragmentInActivity
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ActivityProfileBinding
import top.easelink.lcg.ui.profile.viewmodel.ProfileViewModel
import top.easelink.lcg.utils.ThemeHelper
import top.easelink.lcg.utils.setStatusBarPadding

const val KEY_PROFILE_URL = "profile_url"

class ProfileActivity : TopActivity() {

    private lateinit var mViewModel: ProfileViewModel
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUp()
    }

    private fun setUp() {
        setupToolBar()
        val profileUrl = intent.getStringExtra(KEY_PROFILE_URL) ?: ""
        showFragment(ProfileFragment.newInstance(profileUrl))
    }

    private fun showFragment(fragment: TopFragment) {
        addFragmentInActivity(
            supportFragmentManager,
            fragment,
            R.id.profile_root
        )
    }

    private fun setupToolBar() {
        // targetSdk 36 edge-to-edge：toolbar 顶部、Fragment 容器底部分别加 inset padding。
        binding.toolbar.setStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = bars.bottom)
            insets
        }
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