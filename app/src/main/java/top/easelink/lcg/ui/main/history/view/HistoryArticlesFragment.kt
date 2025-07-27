package top.easelink.lcg.ui.main.history.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import top.easelink.framework.threadpool.IOPool
import top.easelink.framework.threadpool.Main
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.databinding.FragmentHistoryArticlesBinding
import top.easelink.lcg.ui.main.history.model.HistoryModel
import top.easelink.lcg.ui.main.source.local.ArticlesDatabase

class HistoryArticlesFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentHistoryArticlesBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAdapter: HistoryArticlesAdapter

    companion object {
        fun newInstance(): HistoryArticlesFragment {
            return HistoryArticlesFragment()
        }
    }

    override fun isControllable(): Boolean {
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.clearAll.setOnClickListener {
            launch(IOPool) {
                ArticlesDatabase.getInstance().articlesDao().deleteHistories()
            }
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
            itemAnimator = DefaultItemAnimator()
            adapter = mAdapter
            setHasFixedSize(true)
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