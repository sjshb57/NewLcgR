package top.easelink.lcg.ui.main.message.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.ui.main.message.viewmodel.ConversationListViewModel
import top.easelink.lcg.ui.main.model.Conversation
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.databinding.ItemConversationViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding


class ConversationListAdapter(
    val mConversationVM: ConversationListViewModel
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val mConversations: MutableList<Conversation> = mutableListOf()

    override fun getItemCount(): Int {
        return if (mConversations.isEmpty()) {
            1
        } else {
            // todo add load more in the future
            mConversations.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mConversations.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mConversations.size) {
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
                val binding = ItemConversationViewBinding.inflate(inflater, parent, false)
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

    fun addItems(conversations: List<Conversation>) {
        mConversations.addAll(conversations)
        notifyDataSetChanged()
    }

    fun appendItems(conversations: List<Conversation>) {
        val count = itemCount
        mConversations.addAll(conversations)
        notifyItemRangeInserted(count - 1, conversations.size)
    }

    fun clearItems() {
        mConversations.clear()
    }

    inner class ArticleViewHolder internal constructor(private val binding: ItemConversationViewBinding) :
        BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val conversation = mConversations[position]
            binding.apply {
                dateTime.text = conversation.lastMessageDateTime
                conversation.avatar?.let {
                    conversationUserAvatar.load(it) {
                        transformations(RoundedCornersTransformation(2.dpToPx(root.context)))
                        error(R.drawable.ic_noavatar_middle_gray)
                    }
                }
                lastMessage.text = conversation.lastMessage
                username.text = conversation.username
                conversationListContainer.setOnClickListener {
                    WebViewActivity.startWebViewWith(
                        SERVER_BASE_URL + conversation.replyUrl,
                        root.context
                    )
//                    context.startActivity(Intent(context, ConversationDetailActivity::class.java))
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
            //TODO add fetch more in the future
            binding.loading.visibility = View.GONE
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
        private const val VIEW_TYPE_LOAD_MORE = 2
    }
}