package top.easelink.lcg.ui.main.message.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.customview.htmltextview.HtmlCoilImageGetter
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.message.viewmodel.NotificationViewModel
import top.easelink.lcg.ui.main.model.BaseNotification
import top.easelink.lcg.databinding.ItemNotificationViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding


class NotificationsAdapter(
    private val notificationViewModel: NotificationViewModel,
    private val mFragment: Fragment
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val mNotifications: MutableList<BaseNotification> = mutableListOf()

    override fun getItemCount(): Int {
        return if (mNotifications.isEmpty()) {
            1
        } else {
            mNotifications.size + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mNotifications.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mNotifications.size) {
                VIEW_TYPE_LOAD_MORE
            } else VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val binding = ItemNotificationViewBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding)
            }
            else -> EmptyViewHolder(
                inflater.inflate(R.layout.item_empty_view, parent, false)
            )
        }
    }

    fun addItems(notifications: List<BaseNotification>) {
        mNotifications.addAll(notifications)
        notifyDataSetChanged()
    }

    fun appendItems(notifications: List<BaseNotification>) {
        val count = itemCount
        mNotifications.addAll(notifications)
        notifyItemRangeInserted(count - 1, notifications.size)
    }

    fun clearItems() {
        mNotifications.clear()
    }

    inner class ArticleViewHolder internal constructor(private val binding: ItemNotificationViewBinding) :
        BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val notification = mNotifications[position]
            binding.apply {
                line.visibility = if (position == 0) View.GONE else View.VISIBLE
                notificationTitle.apply {
                    setHtml(
                        notification.content, HtmlCoilImageGetter(
                            root.context,
                            this
                        )
                    )
                    linksClickable = false
                }
                dateTime.text = notification.dateTime
                notificationAvatar.load(notification.avatar) {
                    transformations(RoundedCornersTransformation(2.dpToPx(root.context))) // 使用 binding.root.context
                    placeholder(R.drawable.ic_noavatar_middle_gray)
                }
            }
        }

    }

    inner class EmptyViewHolder(view: View) : BaseViewHolder(view) {
        override fun onBind(position: Int) {}
    }

    inner class LoadMoreViewHolder internal constructor(private val binding: ItemLoadMoreViewBinding) :
        BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            binding.loading.visibility = View.VISIBLE
            notificationViewModel.fetchMoreNotifications {
                binding.loading.post {
                    binding.loading.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
        private const val VIEW_TYPE_LOAD_MORE = 2
    }

}