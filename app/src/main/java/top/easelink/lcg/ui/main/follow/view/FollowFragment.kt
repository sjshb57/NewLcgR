package top.easelink.lcg.ui.main.follow.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentFollowBinding

class FollowFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentFollowBinding? = null
    private val binding get() = _binding!!
    private lateinit var tabMediator: TabLayoutMediator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isControllable(): Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.followViewPager.adapter = FollowViewPagerAdapter(
            requireActivity(),
            requireContext()
        ).apply {
            binding.followViewPager.offscreenPageLimit = 1
        }

        tabMediator = TabLayoutMediator(
            binding.followTab,
            binding.followViewPager
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_title_following_feed)
                1 -> getString(R.string.tab_title_following)
                2 -> getString(R.string.tab_title_subscriber)
                else -> ""
            }
        }.apply { attach() }
    }

    override fun onDestroyView() {
        tabMediator.detach()
        _binding = null
        super.onDestroyView()
    }
}