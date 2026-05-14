package top.easelink.lcg.ui.main.history.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemArticleEmptyViewBinding
import top.easelink.lcg.databinding.ItemHistoryArticleViewBinding
import top.easelink.lcg.ui.main.article.view.PostPreviewDialog
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleEmptyItemViewModel
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleEmptyItemViewModel.ArticleEmptyItemViewModelListener
import top.easelink.lcg.ui.main.history.model.HistoryModel
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.source.local.ArticlesDatabase
import top.easelink.lcg.utils.showMessage
import java.lang.ref.WeakReference

class HistoryArticlesAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    private var mFragmentManager: WeakReference<FragmentManager>? = null
    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<HistoryModel>() {
        override fun areItemsTheSame(oldItem: HistoryModel, newItem: HistoryModel): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: HistoryModel, newItem: HistoryModel): Boolean {
            return oldItem == newItem
        }
    })

    override fun getItemCount(): Int {
        return differ.currentList.size.takeIf { it > 0 } ?: 1
    }


    override fun getItemViewType(position: Int): Int {
        return if (differ.currentList.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_NORMAL
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is ArticleViewHolder -> {
                if (differ.currentList.isNotEmpty() && position < differ.currentList.size) {
                    holder.bind(differ.currentList[position])
                }
            }
            is EmptyViewHolder -> holder.onBind(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> ArticleViewHolder(
                ItemHistoryArticleViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> EmptyViewHolder(
                ItemArticleEmptyViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    fun submitList(newList: List<HistoryModel>) {
        differ.submitList(if (newList.isEmpty()) emptyList() else ArrayList(newList))
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        mFragmentManager = WeakReference(fragmentManager)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }

    inner class ArticleViewHolder(
        private val binding: ItemHistoryArticleViewBinding
    ) : BaseViewHolder(binding.root) {

        fun bind(item: HistoryModel) {
            with(binding) {
                titleTextView.text = item.title
                authorTextView.text = item.author
                dateTextView.text = formatRelativeTime(item.timeStamp)

                // 整张卡片可点击进入文章；长按预览。
                layout.setOnClickListener {
                    EventBus.getDefault().post(OpenArticleEvent(item.url))
                }
                layout.setOnLongClickListener {
                    showMessage("长按预览文章")
                    mFragmentManager?.get()?.let { fm ->
                        PostPreviewDialog.newInstance(item.url).show(fm, PostPreviewDialog.TAG)
                    }
                    true
                }
                removeButton.setOnClickListener {
                    ioScope.launch {
                        ArticlesDatabase.getInstance().articlesDao().deleteHistory(item.url)
                    }
                }
            }
        }

        override fun onBind(position: Int) {}

        private fun formatRelativeTime(timestamp: Long): String {
            if (timestamp <= 0L) return ""
            val diff = System.currentTimeMillis() - timestamp
            val minutes = diff / 1000 / 60
            return when {
                minutes < 1L -> "刚刚"
                minutes < 60L -> "${minutes} 分钟前"
                minutes < 24 * 60L -> "${minutes / 60} 小时前"
                minutes < 30 * 24 * 60L -> "${minutes / 60 / 24} 天前"
                else -> {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    fmt.format(java.util.Date(timestamp))
                }
            }
        }
    }

    inner class EmptyViewHolder(
        binding: ItemArticleEmptyViewBinding
    ) : BaseViewHolder(binding.root), ArticleEmptyItemViewModelListener {
        init {
            binding.viewModel = ArticleEmptyItemViewModel(this)
        }

        override fun onBind(position: Int) {}

        override fun onRetryClick() {
            showMessage(R.string.history_articles_empty_tips)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
    }
}