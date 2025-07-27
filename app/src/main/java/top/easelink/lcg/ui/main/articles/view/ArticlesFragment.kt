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
import top.easelink.framework.base.BaseFragment
import top.easelink.lcg.R
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.databinding.FragmentArticlesBinding
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleFetcher
import top.easelink.lcg.ui.main.articles.viewmodel.ArticlesViewModel

class ArticlesFragment : BaseFragment<FragmentArticlesBinding, ArticlesViewModel>() {

    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!

    var controllableFlag: Boolean = true

    override fun isControllable(): Boolean {
        return controllableFlag
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_articles
    }

    override fun initViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentArticlesBinding {
        return FragmentArticlesBinding.inflate(inflater, container, false).also {
            _binding = it
        }
    }

    override fun getViewModel(): ArticlesViewModel {
        return ViewModelProvider(this)[ArticlesViewModel::class.java]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scrollToTop() {
        binding.backToTop.playAnimation()
        binding.recyclerView.let {
            val pos =
                (it.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            if (pos != null && pos > 30) {
                it.scrollToPosition(30)
                it.smoothScrollToPosition(0)
            } else {
                it.smoothScrollToPosition(0)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
    }

    private fun setUpView() {
        setupRecyclerView()
        arguments?.getString(ARG_PARAM)?.let {
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
            viewModel.initUrl(it)
        }
        binding.backToTop.setOnClickListener {
            scrollToTop()
        }
        binding.refreshLayout.run {
            val context = context ?: LCGApp.context
            setColorSchemeColors(
                ContextCompat.getColor(context, R.color.colorPrimary),
                ContextCompat.getColor(context, R.color.colorAccent),
                ContextCompat.getColor(context, R.color.colorPrimaryDark)
            )
            setScrollUpChild(binding.recyclerView)
            setOnRefreshListener {
                viewModel.fetchArticles(ArticleFetcher.FetchType.FETCH_INIT) {}
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            val mLayoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            layoutManager = mLayoutManager
            itemAnimator = DefaultItemAnimator()
            adapter = ArticlesAdapter(
                viewModel
            ).also {
                it.setFragmentManager(childFragmentManager)
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    binding.backToTop.let {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (mLayoutManager.findFirstVisibleItemPosition() <= 1) {
                                it.visibility = View.GONE
                                it.pauseAnimation()
                            } else {
                                it.visibility = View.VISIBLE
                            }
                        } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            it.visibility = View.GONE
                            it.pauseAnimation()
                        }
                    }
                }
            })
        }
    }

    companion object {
        private const val ARG_PARAM = "param"

        @JvmStatic
        fun newInstance(param: String, isControllable: Boolean = true): ArticlesFragment {
            return ArticlesFragment().apply {
                arguments = Bundle().also {
                    it.putString(ARG_PARAM, param)
                }
                controllableFlag = isControllable
            }
        }
    }
}