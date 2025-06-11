package top.easelink.lcg.ui.main.login.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentLoginHintBinding
import top.easelink.lcg.ui.webview.view.WebViewActivity

class LoginHintFragment : TopFragment() {

    private var _binding: FragmentLoginHintBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginHintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginHintInstruction.setHtml(R.raw.login_instruction)
        binding.loginHintBtn.setOnClickListener {
            WebViewActivity.openLoginPage(mContext)
        }
        binding.qqLogin.setOnClickListener {
            WebViewActivity.openQQLoginPage(mContext)
        }
        binding.messageAnimation.setOnClickListener {
            binding.messageAnimation.playAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}