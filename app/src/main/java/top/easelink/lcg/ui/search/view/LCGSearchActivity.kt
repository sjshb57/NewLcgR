package top.easelink.lcg.ui.search.view

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.easelink.framework.topbase.TopActivity
import top.easelink.framework.utils.addFragmentInActivity
import top.easelink.lcg.R
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.databinding.ActivitySearchLcgBinding
import top.easelink.lcg.ui.main.article.view.ArticleFragment
import top.easelink.lcg.ui.main.largeimg.view.LargeImageDialog
import top.easelink.lcg.ui.main.model.OpenLargeImageViewEvent
import top.easelink.lcg.ui.search.model.OpenSearchResultEvent
import top.easelink.lcg.ui.search.viewmodel.LCGSearchResultAdapter
import top.easelink.lcg.ui.search.viewmodel.LCGSearchViewModel
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.showMessage

class LCGSearchActivity : TopActivity() {

    companion object {
        const val KEY_WORD = "key_word"
    }

    private lateinit var mSearchViewModel: LCGSearchViewModel
    private lateinit var binding: ActivitySearchLcgBinding
    private lateinit var backPressedCallback: OnBackPressedCallback

    private val threadRegex by lazy {
        Regex(
            "thread-[0-9]+-[0-9]+-[0-9]+.html$",
            RegexOption.IGNORE_CASE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchLcgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        EventBus.getDefault().register(this)
        mSearchViewModel = ViewModelProvider(this)[LCGSearchViewModel::class.java]
        setUp()
        val kw = intent.getStringExtra(KEY_WORD)
        if (!kw.isNullOrBlank()) {
            mSearchViewModel.setKeyword(kw)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.remove()
        EventBus.getDefault().unregister(this)
    }

    private fun handleBackPress() {
        if (mFragmentTags.isNotEmpty() && mFragmentTags.size >= 1) {
            while (onFragmentDetached(mFragmentTags.pop()).also {
                    mFragmentTags.clear()
                }
            ) {
                if (mFragmentTags.isEmpty()) {
                    binding.toolbar.visibility = View.VISIBLE
                }
                return
            }
        }
        finish()
    }

    private fun setUp() {
        setupRecyclerView()
        setupObserver()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LCGSearchActivity).also {
                it.orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            adapter = LCGSearchResultAdapter(mSearchViewModel)
        }
    }

    private fun setupObserver() {
        mSearchViewModel.totalResult.observe(this) {
            binding.toolbar.title = it
        }
        mSearchViewModel.searchResults.observe(this@LCGSearchActivity) {
            (binding.recyclerView.adapter as? LCGSearchResultAdapter)?.apply {
                clearItems()
                addItems(it)
            }
        }
        mSearchViewModel.isLoading.observe(this@LCGSearchActivity) {
            if (it) {
                binding.searchingFile.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.searchingFile.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenSearchResultEvent) {
        when {
            threadRegex.containsMatchIn(event.url) ||
                    event.url.startsWith("forum.php") -> {
                if (AppConfig.searchResultShowInWebView) {
                    WebViewActivity.startWebViewWith(SERVER_BASE_URL + event.url, this)
                } else {
                    showFragment(ArticleFragment.newInstance(event.url))
                }
            }
            event.url.startsWith("http") ||
                    event.url.startsWith(SERVER_BASE_URL) -> {
                WebViewActivity.startWebViewWith(event.url, this)
            }
            else -> showMessage(R.string.general_error)
        }
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

    private fun showFragment(fragment: Fragment) {
        addFragmentInActivity(
            supportFragmentManager,
            fragment,
            R.id.view_root
        )
        binding.toolbar.visibility = View.GONE
    }
}