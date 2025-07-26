package top.easelink.lcg.ui.main.recommand.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentRecommendBinding
import top.easelink.lcg.ui.main.recommand.viewmodel.RecommendViewPagerAdapter

class RecommendFragment : Fragment(R.layout.fragment_recommend) {
    private var _binding: FragmentRecommendBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecommendBinding.bind(view)

        val adapter = RecommendViewPagerAdapter(requireActivity(), requireContext())
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.mainTab, binding.viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
            // 直接使用字符串模板替代资源引用
            tab.contentDescription = "Tab ${position + 1}"
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}