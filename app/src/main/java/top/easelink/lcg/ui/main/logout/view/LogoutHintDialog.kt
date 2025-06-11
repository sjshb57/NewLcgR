package top.easelink.lcg.ui.main.logout.view

import android.os.Bundle
import android.view.*
import top.easelink.framework.topbase.TopDialog
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.appinit.LCGApp
import top.easelink.lcg.databinding.DialogLogoutHintBinding

class LogoutHintDialog(
    private val positive: (() -> Unit)? = null,
    private val negative: (() -> Unit)? = null
) : TopDialog() {

    private var _binding: DialogLogoutHintBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setWindowAnimations(R.style.FadeInOutAnim)
        _binding = DialogLogoutHintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoutMessage.text = getString(R.string.logout_confirm_message, UserDataRepo.username)
        binding.logoutConfirmBtn.setOnClickListener {
            positive?.invoke()
            dismissDialog()
        }
        binding.cancelBtn.setOnClickListener {
            negative?.invoke()
            dismissDialog()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val window = dialog?.window
        if (window != null) {
            val windowParam = window.attributes
            windowParam.width = 300.dpToPx(LCGApp.context).toInt()
            windowParam.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowParam.gravity = Gravity.CENTER
            window.attributes = windowParam
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}