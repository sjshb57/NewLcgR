package top.easelink.lcg.ui.main.article.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.OriginalSize
import timber.log.Timber
import top.easelink.lcg.R
import top.easelink.lcg.databinding.DialogScreenCaptureBinding
import top.easelink.lcg.utils.startWeChat
import top.easelink.lcg.utils.syncSystemGallery

class ScreenCaptureDialog : DialogFragment() {

    private var _binding: DialogScreenCaptureBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "ScreenCaptureDialog"
        private const val IMAGE_PATH = "image_path"

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
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.5f)
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
            arguments?.getString(IMAGE_PATH)?.let { path ->
                loadImage(path)
                setupShareButton(path)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun loadImage(path: String) {
        ImageRequest.Builder(requireContext())
            .data(path)
            .size(OriginalSize)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(binding.imgScreenCapture)
            .let { Coil.imageLoader(requireContext()).enqueue(it.build()) }
    }

    private fun setupShareButton(path: String) {
        binding.share.setOnClickListener {
            val fileName = path.substringAfterLast('/')
            syncSystemGallery(requireContext(), path, fileName)
            startWeChat(requireContext())
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}