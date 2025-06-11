package top.easelink.lcg.ui.main.article.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogReplyPostBinding
import top.easelink.lcg.ui.main.article.viewmodel.ReplyPostViewModel
import top.easelink.lcg.utils.showMessage

class ReplyPostDialog : TopDialog() {

    private lateinit var replyPostViewModel: ReplyPostViewModel
    private var _binding: DialogReplyPostBinding? = null
    private val binding get() = _binding!!

    private var lastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_FullScreen_BottomInOut)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setWindowAnimations(R.style.BottomInOutAnim)
        _binding = DialogReplyPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        replyPostViewModel = ViewModelProvider(this).get(ReplyPostViewModel::class.java)
        binding.btnCancel.setOnClickListener {
            dismissDialog()
        }
        binding.replyTo.text = String.format(
            getString(R.string.reply_post_dialog_title),
            arguments?.getString(REPLY_POST_AUTHOR)
        )
        replyPostViewModel.sending.observe(viewLifecycleOwner, object : Observer<Boolean> {
            var lastState: Boolean = false
            override fun onChanged(newState: Boolean) {
                if (lastState != newState) {
                    lastState = newState
                    if (newState) {
                        binding.btnConfirm.setText(R.string.reply_post_btn_sending)
                    } else {
                        binding.btnConfirm.setText(R.string.reply_post_btn_sent)
                    }
                }
            }
        })
        binding.btnConfirm.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 2000) {
                showMessage(R.string.reply_btn_debounced_notice)
                return@setOnClickListener
            }
            lastClickTime = System.currentTimeMillis()
            val content = binding.replyContent.text?.trimEnd()
            replyPostViewModel.sendReply(
                arguments?.getString(REPLY_POST_URL),
                content.toString()
            ) {
                binding.root.postDelayed({
                    dismissDialog()
                }, 1000L)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun show(fragmentManager: FragmentManager) {
        super.show(fragmentManager, TAG)
    }

    companion object {
        private val TAG = ReplyPostDialog::class.java.simpleName
        private const val REPLY_POST_URL = "reply_post_url"
        private const val REPLY_POST_AUTHOR = "reply_post_author"

        @JvmStatic
        fun newInstance(replyPostUrl: String, author: String): ReplyPostDialog {
            return ReplyPostDialog().apply {
                arguments = Bundle().apply {
                    putString(REPLY_POST_URL, replyPostUrl)
                    putString(REPLY_POST_AUTHOR, author)
                }
            }
        }
    }
}