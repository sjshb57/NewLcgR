package top.easelink.lcg.ui.main.history.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
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
    private val mHistoryList = mutableListOf<HistoryModel>()
    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    override fun getItemCount() = if (mHistoryList.isEmpty()) 1 else mHistoryList.size

    override fun getItemViewType(position: Int) =
        if (mHistoryList.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_NORMAL

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
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

    fun addItems(articleList: List<HistoryModel>) {
        val startPosition = mHistoryList.size
        mHistoryList.addAll(articleList)
        notifyItemRangeInserted(startPosition, articleList.size)
    }

    fun clearItems() {
        val size = mHistoryList.size
        mHistoryList.clear()
        notifyItemRangeRemoved(0, size)
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
        override fun onBind(position: Int) {
            val article = mHistoryList[position]
            with(binding) {
                titleTextView.text = article.title
                titleTextView.setOnClickListener {
                    EventBus.getDefault().post(OpenArticleEvent(article.url))
                }
                titleTextView.setOnLongClickListener {
                    showMessage("长按预览文章")
                    mFragmentManager?.get()?.let { fm ->
                        PostPreviewDialog.newInstance(article.url).show(fm, PostPreviewDialog.TAG)
                    }
                    true
                }
                authorTextView.text = article.author
                removeButton.setOnClickListener {
                    ioScope.launch {
                        ArticlesDatabase.getInstance().articlesDao().deleteHistory(article.url)
                    }
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

        override fun onBind(position: Int) {
            // 空实现
        }

        override fun onRetryClick() {
            showMessage(R.string.history_articles_empty_tips)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_NORMAL = 1
    }
}