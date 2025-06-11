package top.easelink.lcg.ui.main.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemForumsGridBinding
import top.easelink.lcg.ui.main.discover.model.ForumNavigationModel
import top.easelink.lcg.ui.main.forumnav.view.ForumNavigationFragment
import top.easelink.lcg.ui.main.model.OpenForumEvent


class ForumNavigationAdapter : RecyclerView.Adapter<BaseViewHolder>() {
    private val mForumItems: MutableList<ForumNavigationModel> = mutableListOf()
    override fun getItemCount(): Int {
        return mForumItems.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == mForumItems.size) {
            VIEW_TYPE_LOAD_MORE
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val binding = ItemForumsGridBinding.inflate(inflater, parent, false)
                ForumViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemForumsGridBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding)
            }
            else -> throw IllegalStateException()
        }
    }

    fun addItems(forumModels: List<ForumNavigationModel>) {
        mForumItems.addAll(forumModels)
        notifyDataSetChanged()
    }

    inner class ForumViewHolder internal constructor(private val binding: ItemForumsGridBinding) : BaseViewHolder(binding.root) {

        private fun onItemClick(title: String, url: String) {
            EventBus.getDefault().post(OpenForumEvent(title, url, true))
        }

        override fun onBind(position: Int) {
            val forumModel = mForumItems[position]
            binding.apply {
                root.setOnClickListener {
                    onItemClick(forumModel.title, forumModel.url)
                }
                gridText.text = forumModel.title
                gridImage.setImageResource(forumModel.drawableRes)
            }
        }
    }

    inner class LoadMoreViewHolder internal constructor(private val binding: ItemForumsGridBinding) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            binding.apply {
                root.setOnClickListener {
                    EventBus.getDefault().post(ForumNavigationFragment::class.java)
                }
                gridText.text = root.context.getText(R.string.more_forums)
                gridImage.setImageResource(R.drawable.ic_more)
            }
        }
    }

    companion object {
        const val VIEW_TYPE_NORMAL = 1
        const val VIEW_TYPE_LOAD_MORE = 2
    }
}