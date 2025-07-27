package top.easelink.lcg.ui.main.articles.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import timber.log.Timber
import top.easelink.framework.base.BaseFragment
import top.easelink.lcg.R
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.databinding.FragmentForumArticlesBinding
import top.easelink.lcg.ui.main.articles.viewmodel.*
import top.easelink.lcg.ui.main.source.model.ForumThread

class ForumArticlesFragment : BaseFragment<FragmentForumArticlesBinding, ForumArticlesViewModel>() {

    private var _binding: FragmentForumArticlesBinding? = null
    private val binding get() = _binding!!

    private var showTab = false

    override fun isControllable(): Boolean = true

    override fun getLayoutId(): Int = R.layout.fragment_forum_articles

    override fun initViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentForumArticlesBinding {
        return FragmentForumArticlesBinding.inflate(inflater, container, false).also {
            _binding = it
        }
    }

    override fun getViewModel(): ForumArticlesViewModel =
        ViewModelProvider(this)[ForumArticlesViewModel::class.java]

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.run {
            showTab = getBoolean(ARG_SHOW_TAB, true)
        }
        setUpToolbar()
        setUp()
    }

    private fun setUpToolbar() {
        binding.articleToolbar.apply {
            title = viewModel.title.value ?: getString(R.string.ic_my_articles)

            inflateMenu(R.menu.forum_articles)
            setOnMenuItemClickListener { menuItem ->
                val order = when (menuItem.itemId) {
                    R.id.action_order_by_datetime -> DATE_LINE_ORDER
                    R.id.action_order_by_lastpost -> LAST_POST_ORDER
                    else -> DEFAULT_ORDER
                }
                try {
                    val pos = binding.forumTab.selectedTabPosition
                    viewModel
                        .threadList
                        .value
                        ?.get(pos)
                        ?.threadUrl
                        ?.let {
                            viewModel.initUrlAndFetch(
                                url = it,
                                fetchType = ArticleFetcher.FetchType.FETCH_INIT,
                                order = order
                            )
                        }
                } catch (e: Exception) {
                    Timber.e(e)
                }
                true
            }
        }
    }

    private fun setUp() {
        viewModel.threadList.observe(viewLifecycleOwner, Observer { threadList ->
            setUpTabLayout(threadList)
        })
        arguments?.let {
            viewModel.initUrlAndFetch(
                url = it.getString(ARG_PARAM)!!,
                fetchType = ArticleFetcher.FetchType.FETCH_INIT
            )
            viewModel.setTitle(it.getString(ARG_TITLE).orEmpty())
        }
        setUpRecyclerView()
    }

    private fun setUpTabLayout(forumThreadList: List<ForumThread>?) {
        if (forumThreadList.isNullOrEmpty() || !showTab) {
            binding.forumTab.apply {
                visibility = View.GONE
                removeAllTabs()
            }
            return
        }
        binding.forumTab.apply {
            visibility = View.VISIBLE
            removeAllTabs()
            forumThreadList.forEach {
                addTab(newTab().setText(it.threadName))
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    viewModel.initUrlAndFetch(
                        url = forumThreadList[tab.position].threadUrl,
                        fetchType = ArticleFetcher.FetchType.FETCH_INIT
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun setUpRecyclerView() {
        binding.refreshLayout.apply {
            LCGApp.context.let {
                setColorSchemeColors(
                    ContextCompat.getColor(it, R.color.colorPrimary),
                    ContextCompat.getColor(it, R.color.colorAccent),
                    ContextCompat.getColor(it, R.color.colorPrimaryDark)
                )
            }
            setScrollUpChild(
                binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(context).apply {
                        orientation = RecyclerView.VERTICAL
                    }
                    itemAnimator = DefaultItemAnimator()
                    adapter = ArticlesAdapter(viewModel).also {
                        it.setFragmentManager(childFragmentManager)
                    }
                }
            )
            setOnRefreshListener {
                viewModel.fetchArticles(ArticleFetcher.FetchType.FETCH_INIT) {}
            }
        }
        viewModel.articles.observe(viewLifecycleOwner, Observer { articleList ->
            if (articleList.isEmpty() && viewModel.isLoading.value == true) {
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                (binding.recyclerView.adapter as? ArticlesAdapter)?.apply {
                    clearItems()
                    addItems(articleList)
                }
            }
        })
    }

    companion object {
        private const val ARG_PARAM = "url"
        private const val ARG_TITLE = "title"
        private const val ARG_SHOW_TAB = "showTab"
        private const val DATE_LINE_ORDER = "dateline"
        private const val LAST_POST_ORDER = "lastpost"
        private const val DEFAULT_ORDER = ""

        @JvmStatic
        fun newInstance(
            title: String,
            param: String,
            showTab: Boolean = true
        ): ForumArticlesFragment {
            return ForumArticlesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM, param)
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_SHOW_TAB, showTab)
                }
            }
        }
    }
}