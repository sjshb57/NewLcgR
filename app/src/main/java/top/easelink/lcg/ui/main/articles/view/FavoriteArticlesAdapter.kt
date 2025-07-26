package top.easelink.lcg.ui.main.articles.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemFavoriteArticleEmptyViewBinding
import top.easelink.lcg.databinding.ItemFavoriteArticleViewV2Binding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleFetcher
import top.easelink.lcg.ui.main.articles.viewmodel.FavoriteArticlesViewModel
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.source.model.ArticleEntity
import top.easelink.lcg.utils.getDateFrom
import java.util.Collections

class FavoriteArticlesAdapter(private var favoriteArticlesViewModel: FavoriteArticlesViewModel) :
    RecyclerView.Adapter<BaseViewHolder>(), ItemTouchHelperCallback.onMoveAndSwipedListener {

    private val mArticleEntities: MutableList<ArticleEntity> = mutableListOf()

    override fun getItemCount(): Int {
        return if (mArticleEntities.isEmpty()) {
            1
        } else {
            mArticleEntities.size + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mArticleEntities.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mArticleEntities.size) {
                VIEW_TYPE_LOAD_MORE
            } else {
                VIEW_TYPE_NORMAL
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val binding = ItemFavoriteArticleViewV2Binding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding)
            }
            VIEW_TYPE_EMPTY -> {
                val binding = ItemFavoriteArticleEmptyViewBinding.inflate(inflater, parent, false)
                EmptyViewHolder(binding)
            }
            else -> throw IllegalStateException()
        }
    }

    fun addItems(articleEntityList: List<ArticleEntity>) {
        mArticleEntities.addAll(articleEntityList)
        notifyDataSetChanged()
    }

    fun clearItems() {
        mArticleEntities.clear()
    }

    inner class ArticleViewHolder(private val binding: ItemFavoriteArticleViewV2Binding) :
        BaseViewHolder(binding.root) {

        private fun onItemClick(url: String) {
            EventBus.getDefault().post(OpenArticleEvent(url))
        }

        override fun onBind(position: Int) {
            val articleEntity = mArticleEntities[position]
            binding.apply {
                root.startAnimation(
                    AnimationUtils.loadAnimation(
                        root.context,
                        R.anim.recycler_item_show
                    )
                )
                favoriteContainer.setOnClickListener {
                    onItemClick(articleEntity.url)
                }
                titleTextView.text = articleEntity.title
                dateTime.text = getDateFrom(articleEntity.timestamp)

                authorTextView.apply {
                    text = articleEntity.author
                    visibility = if (articleEntity.author.isNotBlank()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    inner class EmptyViewHolder(private val binding: ItemFavoriteArticleEmptyViewBinding) :
        BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            binding.syncFavorites.setOnClickListener {
                favoriteArticlesViewModel.syncFavorites()
            }
        }
    }

    inner class LoadMoreViewHolder(private val binding: ItemLoadMoreViewBinding) :
        BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            binding.loading.visibility = View.VISIBLE
            favoriteArticlesViewModel.fetchArticles(ArticleFetcher.FetchType.FETCH_MORE) {
                binding.root.post { binding.loading.visibility = View.GONE }
            }
        }
    }

    override fun onItemRemove(position: Int) {
        if (position in mArticleEntities.indices) {
            mArticleEntities.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition in mArticleEntities.indices && toPosition in mArticleEntities.indices) {
            Collections.swap(mArticleEntities, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
            return true
        }
        return false
    }

    companion object {
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_NORMAL = 1
        const val VIEW_TYPE_LOAD_MORE = 2
    }
}