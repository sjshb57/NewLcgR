package top.easelink.lcg.ui.profile.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import top.easelink.framework.utils.dpToPx
import top.easelink.framework.utils.getStatusBarHeight
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogProfileBinding
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.ui.main.source.parseExtraUserInfoProfilePage
import top.easelink.lcg.ui.profile.model.PopUpProfileInfo
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.showMessage

class PopUpProfileDialog : DialogFragment() {

    private var _binding: DialogProfileBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val POPUP_INFO = "popup_info"

        fun newInstance(popUpInfo: PopUpProfileInfo): PopUpProfileDialog {
            val args = Bundle().apply {
                putParcelable(POPUP_INFO, popUpInfo)
            }
            val fragment = PopUpProfileDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val popUpInfo = arguments?.getParcelableCompat<PopUpProfileInfo>(POPUP_INFO) ?: return
        binding.extraInfoGrid.adapter =
            UserInfoGridViewAdapter(view.context, R.layout.item_profile_user_info).also {
                popUpInfo.extraUserInfo?.let { info ->
                    it.addAll(parseExtraUserInfoProfilePage(info))
                }
            }
        binding.username.text = popUpInfo.userName
        popUpInfo.followInfo?.let { info ->
            binding.subscribeBtn.visibility = View.VISIBLE
            binding.subscribeBtn.text = info.first
            binding.subscribeBtn.setOnClickListener {
                onSubscribeClicked(info.second)
            }
        } ?: run {
            binding.subscribeBtn.visibility = View.GONE
        }

        binding.profileBtn.setOnClickListener {
            WebViewActivity.startWebViewWith(SERVER_BASE_URL + popUpInfo.profileUrl, it.context)
        }

        binding.profileAvatar.load(popUpInfo.imageUrl) {
            transformations(RoundedCornersTransformation(4.dpToPx(mContext)))
        }
    }

    override fun onStart() {
        super.onStart()
        val popUpInfo = arguments?.getParcelableCompat<PopUpProfileInfo>(POPUP_INFO) ?: return
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.setGravity(Gravity.START or Gravity.TOP)
            val padding = 10.dpToPx(mContext).toInt()
            window.attributes = window.attributes.apply {
                x = popUpInfo.imageX - padding
                y = popUpInfo.imageY - getStatusBarHeight(mContext) - padding
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        dialog?.setCanceledOnTouchOutside(true)
    }

    private fun onSubscribeClicked(url: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    JsoupClient.sendGetRequestWithQuery(url).getElementById("messagetext")?.text()?.let { msg ->
                        withContext(Dispatchers.Main) {
                            showMessage(msg)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    showMessage(R.string.subscribe_failed)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

inline fun <reified T : android.os.Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}