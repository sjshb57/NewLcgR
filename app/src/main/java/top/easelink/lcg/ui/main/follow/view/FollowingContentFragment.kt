package top.easelink.lcg.ui.main.follow.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.databinding.FragmentFollowingBinding
import top.easelink.lcg.ui.main.follow.viewmodel.FollowingFeedViewModel

class FollowingContentFragment : TopFragment() {

    private lateinit var mFollowVM: FollowingFeedViewModel
    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mFollowVM = ViewModelProvider(this)[FollowingFeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRV()
        mFollowVM.fetchData()
    }

    private fun setUpRV() {
        binding.followList.apply {
            layoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            adapter = FollowingFeedAdapter(mFollowVM)
            mFollowVM.apply {
                isLoading.observe(viewLifecycleOwner, Observer {
                    if (it) {
                        binding.loading.visibility = View.VISIBLE
                        binding.followList.visibility = View.GONE
                    } else {
                        binding.loading.visibility = View.GONE
                        binding.followList.visibility = View.VISIBLE
                    }
                })
                follows.observe(viewLifecycleOwner, Observer {
                    (adapter as FollowingFeedAdapter).run {
                        if (itemCount > 1) {
                            appendItems(it)
                        } else {
                            clearItems()
                            addItems(it)
                        }
                    }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}