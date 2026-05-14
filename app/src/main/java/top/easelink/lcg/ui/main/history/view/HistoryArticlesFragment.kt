package top.easelink.lcg.ui.main.history.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import top.easelink.framework.threadpool.IOPool
import top.easelink.framework.threadpool.Main
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentHistoryArticlesBinding
import top.easelink.lcg.ui.main.history.model.HistoryModel
import top.easelink.lcg.ui.main.source.local.ArticlesDatabase
import top.easelink.lcg.utils.setStatusBarPadding

class HistoryArticlesFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentHistoryArticlesBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAdapter: HistoryArticlesAdapter

    companion object {
        fun newInstance(): HistoryArticlesFragment {
            return HistoryArticlesFragment()
        }
    }

    override fun isControllable(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // toolbar 顶部 + recyclerview 底部分别处理状态栏 / 导航栏 inset，避免 edge-to-edge 下被吃。
        binding.toolbar.setStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_clear_all) {
                launch(IOPool) {
                    ArticlesDatabase.getInstance().articlesDao().deleteHistories()
                }
                true
            } else false
        }

        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        mAdapter = HistoryArticlesAdapter().apply {
            setFragmentManager(childFragmentManager)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = RecyclerView.VERTICAL
            }
            itemAnimator = null
            adapter = mAdapter
        }

        ArticlesDatabase
            .getInstance()
            .articlesDao()
            .getHistories()
            .observe(viewLifecycleOwner) { list ->
                launch(Main) {
                    mAdapter.submitList(list.map {
                        HistoryModel(
                            url = it.url,
                            author = it.author,
                            title = it.title,
                            timeStamp = it.timestamp
                        )
                    })
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
