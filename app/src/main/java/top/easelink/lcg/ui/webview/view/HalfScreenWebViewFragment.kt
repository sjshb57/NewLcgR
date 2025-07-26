package top.easelink.lcg.ui.webview.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.webkit.WebSettings
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.utils.getScreenHeight
import top.easelink.lcg.databinding.DialogHalfScreenWebviewBinding

class HalfScreenWebViewFragment : TopDialog() {

    private var _binding: DialogHalfScreenWebviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val HTML = "html"
        fun newInstance(html: String): HalfScreenWebViewFragment {
            return HalfScreenWebViewFragment().apply {
                arguments = Bundle().apply {
                    putString(HTML, html)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setWindowAnimations(R.style.BottomInOutAnim)
        _binding = DialogHalfScreenWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(HTML)?.let { htmlContent ->
            updateWebViewSettingsLocal()
            binding.webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "")
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.let { window ->
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
        binding.webView.setScrollEnable(true)

        binding.webView.settings.apply {
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            defaultTextEncodingName = "UTF-8"
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}