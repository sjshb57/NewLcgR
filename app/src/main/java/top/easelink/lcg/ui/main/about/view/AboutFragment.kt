package top.easelink.lcg.ui.main.about.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentAboutBinding
import top.easelink.lcg.ui.webview.view.WebViewActivity
import java.util.*


class AboutFragment : TopFragment(), ControllableFragment {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isControllable(): Boolean {
        return true
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        syncAuthorState()
        binding.githubUrl.setOnClickListener {
            WebViewActivity.startWebViewWith(getString(R.string.github_url), requireContext())
        }
        binding.authorEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.author_email))) // EXTRA_EMAIL 期望 String 数组
            intent.putExtra(Intent.EXTRA_SUBJECT, "问题反馈")
            startActivity(Intent.createChooser(intent, "发送反馈邮件"))
        }
    }

    private fun syncAuthorState() {
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        when {
            hour < 7 -> {
                binding.me.setAnimation(R.raw.moon_stars)
            }
            hour < 12 -> {
                binding.me.setAnimation(R.raw.personal_mac_daytime)
            }
            hour == 12 -> {
                binding.me.setAnimation(R.raw.sun)
            }
            hour < 18 -> {
                binding.me.setAnimation(R.raw.personal_phone_daytime)
            }
            hour < 22 -> {
                binding.me.setAnimation(R.raw.personal_mac_night)
            }
            else -> {
                binding.me.setAnimation(R.raw.personal_phone_night)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): AboutFragment {
            val args = Bundle()
            val fragment = AboutFragment()
            fragment.arguments = args
            return fragment
        }
    }
}