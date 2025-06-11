package top.easelink.lcg.ui.main.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.databinding.FragmentDiscoverBinding
import top.easelink.lcg.ui.main.discover.model.ForumListModel
import top.easelink.lcg.ui.main.discover.model.RankListModel
import top.easelink.lcg.ui.main.discover.viewmodel.DiscoverViewModel

class DiscoverFragment : TopFragment(), ControllableFragment {

    private lateinit var mViewModel: DiscoverViewModel
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    override fun isControllable(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(this)[DiscoverViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUp()
    }

    private fun setUp() {
        setUpRV()
        mViewModel.initOptions(mContext)
    }

    private fun setUpRV() {
        binding.forumRv.apply {
            layoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            val multiTypeAdapter = MultiTypeAdapter().apply {
                register(ForumListModel::class.java, ForumNavigationBinder())
                register(RankListModel::class.java, RankListBinder())
            }
            mViewModel.aggregationModels.observe(viewLifecycleOwner, Observer {
                multiTypeAdapter.items = it
                multiTypeAdapter.notifyDataSetChanged()
            })
            adapter = multiTypeAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}