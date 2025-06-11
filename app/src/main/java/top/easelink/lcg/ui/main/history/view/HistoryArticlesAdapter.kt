package top.easelink.lcg.ui.main.history.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.threadpool.IOPool
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
    private val mHistoryList: MutableList<HistoryModel> = mutableListOf()

    override fun getItemCount(): Int {
        return when {
            mHistoryList.isEmpty() -> 1
            else -> mHistoryList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mHistoryList.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val binding = ItemHistoryArticleViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_EMPTY -> {
                val emptyViewBinding =
                    ItemArticleEmptyViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent, false
                    )
                EmptyViewHolder(
                    emptyViewBinding
                )
            }
            else -> {
                val emptyViewBinding =
                    ItemArticleEmptyViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent, false
                    )
                EmptyViewHolder(emptyViewBinding)
            }
        }
    }

    fun addItems(articleList: List<HistoryModel>) {
        mHistoryList.addAll(articleList)
        notifyDataSetChanged()
    }

    fun appendItems(notifications: List<HistoryModel>) {
        val count = itemCount
        mHistoryList.addAll(notifications)
        notifyItemRangeInserted(count - 1, notifications.size)
    }

    fun clearItems() {
        mHistoryList.clear()
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.mFragmentManager = WeakReference(fragmentManager)
    }

    inner class ArticleViewHolder internal constructor(
        private val binding: ItemHistoryArticleViewBinding
    ) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val article = mHistoryList[position]
            binding.titleTextView.apply {
                setOnLongClickListener {
                    showMessage("长按预览文章")
                    mFragmentManager?.get()?.let {
                        PostPreviewDialog.newInstance(
                            mHistoryList[position].url
                        ).show(it, PostPreviewDialog.TAG)
                    }
                    true
                }
                setOnClickListener {
                    EventBus.getDefault().post(OpenArticleEvent(article.url))
                }
            }
            binding.titleTextView.text = article.title
            binding.authorTextView.text = article.author
            binding.removeButton.setOnClickListener {
                GlobalScope.launch(IOPool) {
                    ArticlesDatabase.getInstance().articlesDao().deleteHistory(article.url)
                }
            }
        }
    }

    inner class EmptyViewHolder internal constructor(
        private val mBinding: ItemArticleEmptyViewBinding
    ) : BaseViewHolder(mBinding.root), ArticleEmptyItemViewModelListener {
        override fun onBind(position: Int) {
            val emptyItemViewModel = ArticleEmptyItemViewModel(this)
            mBinding.viewModel = emptyItemViewModel
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