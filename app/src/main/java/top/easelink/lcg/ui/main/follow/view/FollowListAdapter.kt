package top.easelink.lcg.ui.main.follow.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemEmptyViewBinding
import top.easelink.lcg.databinding.ItemFollowViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.ui.main.follow.model.FollowInfo
import top.easelink.lcg.ui.main.follow.viewmodel.FollowListViewModel


class FollowListAdapter(
    private val followListViewModel: FollowListViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val mFollowing: MutableList<FollowInfo> = mutableListOf()
    var nextPageUrl: String? = null

    override fun getItemCount(): Int {
        return if (mFollowing.isEmpty()) {
            1
        } else {
            mFollowing.size + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mFollowing.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mFollowing.size) {
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
                val binding = ItemFollowViewBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding, lifecycleOwner, followListViewModel)
            }
            else -> { // VIEW_TYPE_EMPTY
                val binding = ItemEmptyViewBinding.inflate(inflater, parent, false)
                EmptyViewHolder(binding)
            }
        }
    }

    fun addItems(follows: List<FollowInfo>) {
        mFollowing.addAll(follows)
        notifyDataSetChanged()
    }

    fun appendItems(follows: List<FollowInfo>) {
        val count = itemCount
        mFollowing.addAll(follows)
        notifyItemRangeInserted(count - 1, follows.size)
    }

    fun clearItems() {
        mFollowing.clear()
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        if (holder is LoadMoreViewHolder) {
            holder.removeObserver()
        }
        super.onViewRecycled(holder)
    }

    inner class ArticleViewHolder internal constructor(
        private val binding: ItemFollowViewBinding
    ) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val follow = mFollowing[position]
            binding.apply {
                follow.avatar.let {
                    avatar.load(it) {
                        transformations(RoundedCornersTransformation(2.dpToPx(root.context)))
                            .error(R.drawable.ic_noavatar_middle_gray)
                    }
                }
                lastAction.text = follow.lastAction
                username.text = follow.username
                followerNum.text =
                    root.context.getString(R.string.follower_num_template, follow.followerNum)
                followingNum.text =
                    root.context.getString(R.string.following_num_template, follow.followingNum)
                followListContainer.setOnClickListener {
                    //open article
                }
            }
        }
    }

    inner class EmptyViewHolder(binding: ItemEmptyViewBinding) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {}
    }

    inner class LoadMoreViewHolder internal constructor(
        private val binding: ItemLoadMoreViewBinding,
        private val lifecycleOwner: LifecycleOwner,
        private val followListViewModel: FollowListViewModel
    ) : BaseViewHolder(binding.root) {

        private val observer: Observer<Boolean> = Observer {
            binding.loading.apply {
                if (it) {
                    visibility = View.VISIBLE
                    playAnimation()
                } else {
                    cancelAnimation()
                    visibility = View.GONE
                }
            }
        }

        override fun onBind(position: Int) {
            nextPageUrl?.let {
                followListViewModel.isLoadingForLoadMore.observe(lifecycleOwner, observer)
                followListViewModel.fetchData(it, true)
            } ?: run {
                binding.loading.apply {
                    cancelAnimation()
                    visibility = View.GONE
                }
            }
        }

        fun removeObserver() {
            followListViewModel.isLoadingForLoadMore.removeObserver(observer)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
        private const val VIEW_TYPE_LOAD_MORE = 2
    }
}