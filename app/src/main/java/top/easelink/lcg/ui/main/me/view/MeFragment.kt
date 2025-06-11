package top.easelink.lcg.ui.main.me.view

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.framework.utils.addFragmentInActivity
import top.easelink.framework.utils.addFragmentInFragment
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.account.AccountManager
import top.easelink.lcg.databinding.FragmentMeBinding
import top.easelink.lcg.ui.main.articles.view.FavoriteArticlesFragment
import top.easelink.lcg.ui.main.follow.view.FollowFragment
import top.easelink.lcg.ui.main.history.view.HistoryArticlesFragment
import top.easelink.lcg.ui.main.me.viewmodel.MeViewModel
import top.easelink.lcg.ui.main.model.OpenForumEvent
import top.easelink.lcg.ui.setting.view.SettingActivity
import top.easelink.lcg.utils.WebsiteConstant.MY_ARTICLES_QUERY
import top.easelink.lcg.utils.avatar.PlaceholderDrawable
import top.easelink.lcg.utils.avatar.getDefaultAvatar
import top.easelink.lcg.utils.showMessage

class MeFragment : TopFragment(), ControllableFragment {

    private lateinit var viewModel: MeViewModel
    private var _binding: FragmentMeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private var lastFetchInfoTime = 0L
        private const val MINIMUM_REQUEST_INTERVAL = 30_000 // 30s

        private var lastShowLoginHintTime = 0L
        private const val MINIMUM_HINT_SHOW_INTERVAL = 120_000 // 120s
    }

    override fun isControllable(): Boolean {
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MeViewModel::class.java]
        binding.settingBtn.setOnClickListener {
            startActivity(Intent(context, SettingActivity::class.java))
        }
        updateIconButtons()
        registerObservers()
        if (SystemClock.elapsedRealtime() - lastFetchInfoTime > MINIMUM_REQUEST_INTERVAL) {
            lastFetchInfoTime = SystemClock.elapsedRealtime()
            viewModel.fetchUserInfoDirect()
        }
    }

    private fun registerObservers() {
        AccountManager.isLoggedIn.observe(viewLifecycleOwner) { loggedIn ->
            updateViewState(loggedIn)
            if (!loggedIn
                && isAdded
                && isVisible
                && SystemClock.elapsedRealtime() - MINIMUM_HINT_SHOW_INTERVAL > lastShowLoginHintTime
            ) {
                lastShowLoginHintTime = SystemClock.elapsedRealtime()
                showMessage(R.string.login_hint_message)
            }
        }
        AccountManager.userInfo.observe(viewLifecycleOwner) { info ->
            binding.meUserName.text = info.userName
            binding.meUserGroup.text = info.groupInfo
            binding.meWuaicoin.text = info.wuaiCoin
            binding.meAnwserRate.text = info.answerRate
            binding.meCredit.text = info.credit
            binding.meEnthusiastic.text = info.enthusiasticValue
            info.signInStateUrl?.let { url ->
                binding.meSignInState.visibility = View.VISIBLE
                ImageRequest.Builder(binding.meSignInState.context)
                    .data(url)
                    .allowRgb565(true)
                    .lifecycle(this@MeFragment)
                    .target {
                        binding.meSignInState.apply {
                            layoutParams.width = it.intrinsicWidth / it.intrinsicHeight * height
                            setImageDrawable(it)
                        }
                    }
                    .build()
                    .let { request ->
                        Coil.imageLoader(binding.meSignInState.context).enqueue(request)
                    }
            } ?: run {
                binding.meSignInState.visibility = View.GONE
            }
            info.avatarUrl?.let {
                binding.meUserAvatar.load(it) {
                    placeholder(PlaceholderDrawable)
                    lifecycle(viewLifecycleOwner)
                    transformations(RoundedCornersTransformation(4.dpToPx(binding.meUserAvatar.context)))
                    error(getDefaultAvatar(it))
                }
            }
        }
    }

    private fun updateIconButtons() {
        binding.cardviewMeNotifications.iconNotifications.apply {
            root.setOnClickListener {
                badge.visibility = View.GONE
            }
            btnIcon.setImageResource(R.drawable.ic_favorite)
            tvIcon.setText(R.string.ic_favorite)
        }
        binding.cardviewMeNotifications.iconMyArticles.apply {
            root.setOnClickListener {
                EventBus
                    .getDefault()
                    .post(
                        OpenForumEvent(
                            getString(R.string.ic_my_articles), MY_ARTICLES_QUERY, false
                        )
                    )
            }
            btnIcon.setImageResource(R.drawable.ic_my_articles)
            tvIcon.setText(R.string.ic_my_articles)
        }
        binding.cardviewMeNotifications.iconFollow.apply {
            root.setOnClickListener {
                showFragment(FollowFragment())
            }
            btnIcon.setImageResource(R.drawable.ic_follow)
            tvIcon.setText(R.string.ic_follow)
        }
        binding.cardviewMeNotifications.iconHistory.apply {
            root.setOnClickListener {
                showFragment(HistoryArticlesFragment.newInstance())
            }
            btnIcon.setImageResource(R.drawable.ic_history)
            tvIcon.setText(R.string.ic_history)
        }
    }

    private fun updateViewState(loggedIn: Boolean) {
        if (loggedIn) {
            binding.iconGroup.visibility = View.VISIBLE
        } else {
            binding.iconGroup.visibility = View.GONE
        }

        val colorTab = if (loggedIn) {
            ContextCompat.getColor(mContext, R.color.black_effective)
        } else {
            ContextCompat.getColor(mContext, R.color.semi_gray)
        }
        binding.cardviewMeNotifications.iconFollow.apply {
            root.isEnabled = loggedIn
            btnIcon.setColorFilter(colorTab)
            tvIcon.setTextColor(colorTab)
        }
        binding.cardviewMeNotifications.iconMyArticles.apply {
            root.isEnabled = loggedIn
            btnIcon.setColorFilter(colorTab)
            tvIcon.setTextColor(colorTab)
        }
    }

    private fun showFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.let {
            addFragmentInActivity(
                it,
                fragment,
                R.id.clRootView
            )
        }
    }

    private fun showChildFragment(fragment: Fragment) {
        addFragmentInFragment(childFragmentManager, fragment, R.id.child_root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}