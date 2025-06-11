package top.easelink.lcg.ui.main.article.view

import android.os.Bundle
import android.view.*
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.SizeResolver
import timber.log.Timber
import top.easelink.framework.topbase.TopDialog
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogScreenCaptureBinding
import top.easelink.lcg.utils.startWeChat
import top.easelink.lcg.utils.syncSystemGallery

class ScreenCaptureDialog : TopDialog() {

    private var _binding: DialogScreenCaptureBinding? = null
    private val binding get() = _binding!!

    companion object {
        val TAG: String = ScreenCaptureDialog::class.java.simpleName
        private const val IMAGE_PATH = "image_path"

        @JvmStatic
        fun newInstance(imagePath: String): ScreenCaptureDialog {
            return ScreenCaptureDialog().apply {
                arguments = Bundle().apply {
                    putString(IMAGE_PATH, imagePath)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_FullScreen_BottomInOut)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogScreenCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            arguments?.getString(IMAGE_PATH)?.also { path ->
                ImageRequest.Builder(binding.root.context)
                    .data(path)
                    .size(SizeResolver(OriginalSize))
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .target { drawable ->
                        binding.imgScreenCapture.post {
                            binding.imgScreenCapture.setImageDrawable(drawable)
                        }
                    }.build().let {
                        Coil.imageLoader(binding.root.context).enqueue(it)
                    }
                binding.share.setOnClickListener { shareView ->
                    syncSystemGallery(shareView.context, path, path.let {
                        val p = it.lastIndexOf("/")
                        if (p >= 0 && p < it.length - 1) {
                            it.substring(p + 1)
                        } else {
                            "screenshot.png"
                        }
                    })
                    startWeChat(shareView.context)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}