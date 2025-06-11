package top.easelink.lcg.ui.webview.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.webkit.WebSettings
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.utils.getScreenHeight
import top.easelink.lcg.databinding.DialogHalfScreenWebviewBinding
import top.easelink.framework.customview.webview.HorizontalScrollDisableWebView

class HalfScreenWebViewFragment: TopDialog() {

    private var _binding: DialogHalfScreenWebviewBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    companion object {
        private const val HTML = "html"
        fun newInstance(html: String): HalfScreenWebViewFragment {
            val args = Bundle().apply {
                putString(HTML, html)
            }
            val fragment = HalfScreenWebViewFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setWindowAnimations(R.style.BottomInOutAnim)
        _binding = DialogHalfScreenWebviewBinding.inflate(inflater, container, false)
        return binding.root // 返回 View Binding 的根视图
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(HTML)?.let { htmlContent -> // 使用更明确的参数名
            updateWebViewSettingsLocal()
            binding.webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "") // 通过 binding 访问 web_view
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val window = dialog?.window
        if (window != null) {
            val windowParam = window.attributes
            dialog?.setCanceledOnTouchOutside(true)
            context?.let { ctx ->
                windowParam.width = WindowManager.LayoutParams.MATCH_PARENT
                windowParam.height = getScreenHeight(ctx) / 2
                windowParam.gravity = Gravity.BOTTOM
                window.attributes = windowParam
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun updateWebViewSettingsLocal() {
        // 确保 web_view 是 HorizontalScrollDisableWebView 类型才能调用 setScrollEnable
        if (binding.webView is HorizontalScrollDisableWebView) {
            (binding.webView as HorizontalScrollDisableWebView).setScrollEnable(true)
        } else {
            // 如果不是，可能需要日志记录或不同的处理
            // Timber.w("WebView is not HorizontalScrollDisableWebView, setScrollEnable not called.")
        }

        binding.webView.settings.apply {
            // Zoom Setting
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            defaultTextEncodingName = "UTF-8"
            builtInZoomControls = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理绑定对象，避免内存泄漏
        _binding = null
        // 建议在这里也安全地移除 WebView，避免内存泄漏
        // (binding.webView.parent as? ViewGroup)?.removeView(binding.webView)
        // binding.webView.destroy()
        // 注意：如果你在 onCreateView/onDestroyView 中处理 WebView 的生命周期，
        // 且 WebView 是 Fragment 布局的一部分，通常 Fragment 自身的 View 生命周期管理会处理掉。
        // 但对于某些复杂的 WebView 使用场景，手动销毁和移除可能更安全。
        // 这里暂时注释掉，如果遇到内存泄漏问题再考虑加上。
    }
}