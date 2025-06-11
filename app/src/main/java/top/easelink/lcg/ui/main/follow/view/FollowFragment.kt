package top.easelink.lcg.ui.main.follow.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.databinding.FragmentFollowBinding

class FollowFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentFollowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isControllable(): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.followViewPager.adapter =
            FollowViewPagerAdapter(
                childFragmentManager,
                mContext
            )
        binding.followViewPager.offscreenPageLimit = 0
        binding.followTab.setupWithViewPager(binding.followViewPager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}