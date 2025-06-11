package top.easelink.lcg.ui.main.follow.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.SizeResolver
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.threadpool.IOPool
import top.easelink.framework.utils.dpToPx
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemEmptyViewBinding
import top.easelink.lcg.databinding.ItemFollowContentViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.ui.main.follow.model.FeedInfo
import top.easelink.lcg.ui.main.follow.viewmodel.FollowingFeedViewModel
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.model.OpenLargeImageViewEvent


class FollowingFeedAdapter(
    private val followingFeedViewModel: FollowingFeedViewModel
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val mFeeds: MutableList<FeedInfo> = mutableListOf()

    override fun getItemCount(): Int {
        return if (mFeeds.isEmpty()) {
            1
        } else {
            mFeeds.size + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mFeeds.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mFeeds.size) {
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
                val binding = ItemFollowContentViewBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding, followingFeedViewModel)
            }
            else -> {
                val binding = ItemEmptyViewBinding.inflate(inflater, parent, false)
                EmptyViewHolder(binding)
            }
        }
    }

    fun addItems(follows: List<FeedInfo>) {
        mFeeds.addAll(follows)
        notifyDataSetChanged()
    }

    fun appendItems(follows: List<FeedInfo>) {
        val count = itemCount
        mFeeds.addAll(follows)
        notifyItemRangeInserted(count - 1, follows.size)
    }

    fun clearItems() {
        mFeeds.clear()
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        if (holder is LoadMoreViewHolder) {
            holder.removeObserver()
        }
        super.onViewRecycled(holder)
    }

    inner class ArticleViewHolder internal constructor(
        private val binding: ItemFollowContentViewBinding
    ) : BaseViewHolder(binding.root) {
        @SuppressLint("SetTextI19n")
        override fun onBind(position: Int) {
            val feed = mFeeds[position]
            binding.apply {
                val round = 4.dpToPx(root.context)
                openArticle.setOnClickListener {
                    EventBus.getDefault().post(OpenArticleEvent(feed.articleUrl))
                }

                feed.avatar.let {
                    if (it.isNotEmpty()) {
                        avatar.visibility = View.VISIBLE
                        avatar.load(it) {
                            transformations(RoundedCornersTransformation(round))
                            error(R.drawable.ic_noavatar_middle_gray)
                            crossfade(true)
                        }
                    } else {
                        avatar.visibility = View.GONE
                        avatar.setImageDrawable(null)
                    }
                }
                username.text = feed.username
                dateTime.text = feed.dateTime
                title.text = feed.title
                forum.text = "#${feed.forum}"
                if (feed.quote.isNotBlank()) {
                    content.setHtml(feed.quote)
                    preview.visibility = View.INVISIBLE
                    preview.setImageDrawable(null)
                    preview.layoutParams.also {
                        it.height = 0
                    }
                } else {
                    feed.images?.takeIf {
                        it.isNotEmpty()
                    }?.let { images ->
                        preview.setOnClickListener {
                            EventBus.getDefault().post(OpenLargeImageViewEvent(images[0]))
                        }
                        preview.visibility = View.VISIBLE
                        GlobalScope.launch(IOPool) {
                            ImageRequest.Builder(root.context)
                                .data(images[0])
                                .size(SizeResolver(OriginalSize))
                                .transformations(RoundedCornersTransformation(round))
                                .target {
                                    val newH =
                                        it.intrinsicHeight.toFloat() / it.intrinsicWidth.toFloat() * preview.width.toFloat()
                                    preview.apply {
                                        layoutParams.height = newH.toInt()
                                        setImageDrawable(it)
                                    }
                                }.build()
                                .let {
                                    Coil.imageLoader(root.context).enqueue(it)
                                }
                        }
                    } ?: run {
                        preview.visibility = View.INVISIBLE
                        preview.setImageDrawable(null)
                        preview.layoutParams.height = 0
                    }
                    content.setHtml(feed.content)
                }
            }
        }
    }

    class EmptyViewHolder internal constructor(binding: ItemEmptyViewBinding) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {}
    }

    inner class LoadMoreViewHolder internal constructor(
        private val binding: ItemLoadMoreViewBinding,
        private val followingFeedViewModel: FollowingFeedViewModel
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
            followingFeedViewModel.isLoadingForLoadMore.observeForever(observer)
            followingFeedViewModel.fetchMore {
                if (!it) {
                    binding.loading.apply {
                        cancelAnimation()
                        visibility = View.GONE
                    }
                    removeObserver()
                }
            }
        }

        fun removeObserver() {
            followingFeedViewModel.isLoadingForLoadMore.removeObserver(observer)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
        private const val VIEW_TYPE_LOAD_MORE = 2
    }
}