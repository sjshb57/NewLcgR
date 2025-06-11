package top.easelink.lcg.ui.main.article.view

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import top.easelink.framework.topbase.TopDialog
import top.easelink.framework.utils.CommonUtils
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.databinding.DialogCommentArticleBinding
import top.easelink.lcg.ui.main.article.viewmodel.ReplyPostViewModel
import top.easelink.lcg.ui.main.source.model.Post
import top.easelink.lcg.utils.showMessage

class CommentArticleDialog : TopDialog() {

    private lateinit var replyPostViewModel: ReplyPostViewModel
    private var _binding: DialogCommentArticleBinding? = null
    private val binding get() = _binding!!
    private var lastClickTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setWindowAnimations(R.style.BottomInOutAnim)
        _binding = DialogCommentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        replyPostViewModel = ViewModelProvider(this)[ReplyPostViewModel::class.java]
        binding.btnCancel.setOnClickListener {
            dismissDialog()
        }
        val button = binding.btnConfirm
        replyPostViewModel.sending.observe(viewLifecycleOwner, object : Observer<Boolean> {
            var lastState: Boolean = false
            override fun onChanged(newState: Boolean) {
                if (lastState != newState) {
                    lastState = newState
                    if (newState) {
                        button.setText(R.string.reply_post_btn_sending)
                    } else {
                        button.setText(R.string.reply_post_btn_sent)
                    }
                }
            }
        })
        button.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 2000) {
                showMessage(R.string.reply_btn_debounced_notice)
                return@setOnClickListener
            }
            lastClickTime = System.currentTimeMillis()
            val content = binding.replyContent.text?.trimEnd()
            replyPostViewModel.sendReply(
                arguments?.getString(REPLY_POST_URL),
                content.toString()
            ) { success ->
                binding.root.postDelayed({
                    setResult(content = content.toString(), success = success)
                    dismissDialog()
                }, 1000L)
            }
        }
    }

    private fun setResult(content: String, success: Boolean) {
        if (targetFragment != null) {
            val bundle = Bundle().apply {
                putParcelable(
                    "post", Post(
                        UserDataRepo.username,
                        UserDataRepo.avatar,
                        CommonUtils.getCurrentDate(),
                        content, null, null
                    )
                )
            }
            targetFragment?.onActivityResult(
                ArticleFragment.REPLY_POST_RESULT,
                if (success) 1 else 0,
                Intent().putExtra("post", bundle)
            )
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
        private val TAG = CommentArticleDialog::class.java.simpleName
        private const val REPLY_POST_URL = "reply_article_url"

        @JvmStatic
        fun newInstance(replyPostUrl: String): CommentArticleDialog {
            return CommentArticleDialog().apply {
                arguments = Bundle().apply {
                    putString(REPLY_POST_URL, replyPostUrl)
                }
            }
        }
    }
}