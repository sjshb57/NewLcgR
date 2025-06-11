package top.easelink.lcg.ui.main.login.view

import android.os.Bundle
import android.view.*
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogLoginHintBinding
import top.easelink.lcg.ui.webview.view.WebViewActivity

class LoginHintDialog : TopDialog() {

    private var _binding: DialogLoginHintBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_FullScreen_BottomInOut)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setWindowAnimations(R.style.BottomInOutAnim)
        _binding = DialogLoginHintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginHintInstruction.setHtml(R.raw.login_instruction)
        binding.loginHintBtn.setOnClickListener {
            WebViewActivity.openLoginPage(mContext)
            dismissDialog()
        }
        binding.qqLogin.setOnClickListener {
            WebViewActivity.openQQLoginPage(mContext)
            dismissDialog()
        }
        binding.loginCancelBtn.setOnClickListener {
            dismissDialog()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val window = dialog?.window
        if (window != null) {
            val windowParam = window.attributes
            windowParam.width = WindowManager.LayoutParams.MATCH_PARENT
            windowParam.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowParam.gravity = Gravity.BOTTOM
            window.attributes = windowParam
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}