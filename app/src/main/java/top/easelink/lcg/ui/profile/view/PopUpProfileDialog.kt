package top.easelink.lcg.ui.profile.view

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import coil.load
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import top.easelink.framework.utils.dpToPx
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
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
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

        binding.apply {
            extraInfoGrid.adapter = UserInfoGridViewAdapter(view.context, R.layout.item_profile_user_info).also {
                popUpInfo.extraUserInfo?.let { info ->
                    it.addAll(parseExtraUserInfoProfilePage(info))
                }
            }
            username.text = popUpInfo.userName

            popUpInfo.followInfo?.let { info ->
                subscribeBtn.visibility = View.VISIBLE
                subscribeBtn.text = info.first
                subscribeBtn.setOnClickListener {
                    onSubscribeClicked(info.second)
                }
            } ?: run {
                subscribeBtn.visibility = View.GONE
            }

            profileBtn.setOnClickListener {
                WebViewActivity.startWebViewWith(SERVER_BASE_URL + popUpInfo.profileUrl, it.context)
            }

            profileAvatar.load(popUpInfo.imageUrl) {
                transformations(RoundedCornersTransformation(4.dpToPx(mContext)))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val popUpInfo = arguments?.getParcelableCompat<PopUpProfileInfo>(POPUP_INFO) ?: return

        dialog?.apply {
            window?.apply {
                // 修复方案1：使用传统方式（推荐）
             //   setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // 或者修复方案2：确保已添加core-ktx依赖后使用
                 setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                setGravity(Gravity.START or Gravity.TOP)
                attributes = attributes.apply {
                    val padding = 10.dpToPx(mContext).toInt()
                    val statusBarHeight = getCompatStatusBarHeight()

                    x = popUpInfo.imageX - padding
                    y = popUpInfo.imageY - statusBarHeight - padding
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            setCanceledOnTouchOutside(true)
        }
    }

    private fun getCompatStatusBarHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
        } else {
            ResourcesCompat.getFloat(mContext.resources, R.dimen.dp_24).toInt()
        }
    }

    private fun onSubscribeClicked(url: String) {
        ioScope.launch {
            try {
                JsoupClient.sendGetRequestWithQuery(url).let {
                    it.getElementById("messagetext")?.text()?.let { msg ->
                        showMessage(msg)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(R.string.subscribe_failed)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key) as? T
        }
    }
}