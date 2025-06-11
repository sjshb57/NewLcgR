package top.easelink.lcg.ui.main.largeimg.view

import android.os.Bundle
import android.view.*
import coil.load
import coil.size.OriginalSize
import coil.size.SizeResolver
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogLargeImageBinding

class LargeImageDialog : TopDialog() {

    private var _binding: DialogLargeImageBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): LargeImageDialog {
            return LargeImageDialog().also {
                it.arguments = Bundle().apply {
                    putString(IMAGE_URL, imageUrl)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setWindowAnimations(R.style.FadeInOutAnim)
        _binding = DialogLargeImageBinding.inflate(inflater, container, false)
        return binding.root // 返回绑定类的根视图
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(IMAGE_URL)?.let {
            binding.photo.load(it) {
                size(SizeResolver(OriginalSize))
            }
        }
        binding.exit.setOnClickListener {
            dismissDialog()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val window = dialog?.window
        if (window != null) {
            val windowParam = window.attributes
            windowParam.width = WindowManager.LayoutParams.MATCH_PARENT
            windowParam.height = WindowManager.LayoutParams.MATCH_PARENT
            windowParam.gravity = Gravity.CENTER
            window.attributes = windowParam
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}