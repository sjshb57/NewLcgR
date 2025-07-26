package top.easelink.lcg.ui.main.follow.view

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.message.model.MessageTabModel
import top.easelink.lcg.utils.WebsiteConstant

class FollowViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    context: Context
) : FragmentStateAdapter(fragmentActivity) {

    private val tabModels: List<MessageTabModel> = listOf(
        MessageTabModel(context.getString(R.string.tab_title_following_feed), ""),
        MessageTabModel(
            context.getString(R.string.tab_title_following),
            WebsiteConstant.FOLLOWING_USERS_QUERY
        ),
        MessageTabModel(
            context.getString(R.string.tab_title_subscriber),
            WebsiteConstant.FOLLOWER_USERS_QUERY
        )
    )

    override fun getItemCount(): Int = tabModels.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FollowingContentFragment()
            else -> FollowDetailFragment(tabModels[position].url)
        }
    }
}