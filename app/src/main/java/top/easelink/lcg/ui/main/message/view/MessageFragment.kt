package top.easelink.lcg.ui.main.message.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.addFragmentInFragment
import top.easelink.lcg.R
import top.easelink.lcg.account.AccountManager
import top.easelink.lcg.databinding.FragmentMessageBinding
import top.easelink.lcg.ui.main.login.view.LoginHintFragment

class MessageFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isControllable(): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AccountManager.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn) {
                binding.messageTab.visibility = View.VISIBLE
                binding.messageViewPager.visibility = View.VISIBLE

                binding.messageViewPager.adapter = MessageViewPagerAdapter(
                    requireActivity(),
                    requireContext()
                )

                TabLayoutMediator(binding.messageTab, binding.messageViewPager) { tab, position ->
                    tab.text = when(position) {
                        0 -> getString(R.string.tab_title_notification)
                        1 -> getString(R.string.tab_title_private_message)
                        else -> ""
                    }
                }.attach()
            } else {
                binding.messageTab.visibility = View.GONE
                binding.messageViewPager.visibility = View.GONE
                addFragmentInFragment(
                    fragmentManager = childFragmentManager,
                    fragment = LoginHintFragment(),
                    frameId = R.id.root
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}