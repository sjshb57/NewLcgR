package top.easelink.lcg.ui.main.message.view

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.message.model.MessageTabModel

class MessageViewPagerAdapter internal constructor(
    fragmentActivity: FragmentActivity,
    context: Context
) : FragmentStateAdapter(fragmentActivity) {

    private val tabModels: List<MessageTabModel> = listOf(
        MessageTabModel(context.getString(R.string.tab_title_notification), ""),
        MessageTabModel(context.getString(R.string.tab_title_private_message), "")
    )

    override fun getItemCount(): Int {
        return tabModels.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationFragment()
            1 -> ConversationListFragment()
            else -> throw IllegalStateException("can't reach here")
        }
    }

}