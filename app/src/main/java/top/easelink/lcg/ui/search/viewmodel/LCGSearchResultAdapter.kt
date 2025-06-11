package top.easelink.lcg.ui.search.viewmodel

import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemLcgSearchResultViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.ui.search.model.LCGSearchResultItem
import top.easelink.lcg.ui.search.model.OpenSearchResultEvent

class LCGSearchResultAdapter(
    private var mFetcher: ContentFetcher
) : RecyclerView.Adapter<BaseViewHolder>() {
    private val mSearchResults: MutableList<LCGSearchResultItem> = mutableListOf()

    override fun getItemCount(): Int {
        return if (mSearchResults.isEmpty()) {
            1
        } else {
            mSearchResults.size + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mSearchResults.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mSearchResults.size) {
                VIEW_TYPE_LOAD_MORE
            } else VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> SearchResultViewHolder(
                ItemLcgSearchResultViewBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false // 使用 View Binding inflate 布局
                )
            )
            VIEW_TYPE_LOAD_MORE -> LoadMoreViewHolder(
                ItemLoadMoreViewBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false // 使用 View Binding inflate 布局
                )
            )
            //VIEW_TYPE_EMPTY
            else -> EmptyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_empty_view, parent, false)
            )
        }
    }

    fun addItems(LCGSearchResults: List<LCGSearchResultItem>) {
        mSearchResults.addAll(LCGSearchResults)
        notifyDataSetChanged()
    }

    fun clearItems() {
        mSearchResults.clear()
    }

    interface ContentFetcher {
        fun fetch(type: Type, callback: ((Boolean) -> Unit)?)

        enum class Type {
            INIT,
            NEXT_PAGE
        }
    }

    private inner class SearchResultViewHolder(private val binding: ItemLcgSearchResultViewBinding) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val searchResult = mSearchResults[position].also {
                Log.d("Leon406",it.toString())
            }
            binding.apply {
                titleTv.text = Html.fromHtml(searchResult.title)
                authorTv.text = searchResult.author
                replyAndViewTv.text = searchResult.replyView
                dateTv.text = searchResult.date
                forumTv.text = searchResult.forum
                contentTv.text = Html.fromHtml(searchResult.content)
                root.setOnClickListener {
                    EventBus.getDefault().post(OpenSearchResultEvent(searchResult.fullUrl))
                }
            }
        }
    }

    private inner class EmptyViewHolder(view: View) : BaseViewHolder(view) {
        override fun onBind(position: Int) {}
    }

    private inner class LoadMoreViewHolder(private val binding: ItemLoadMoreViewBinding) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            mFetcher.fetch(ContentFetcher.Type.NEXT_PAGE) { success ->
                binding.root.post {
                    binding.loading.visibility = if (success) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
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