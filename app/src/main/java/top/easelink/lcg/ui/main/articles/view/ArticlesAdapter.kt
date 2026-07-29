package top.easelink.lcg.ui.main.articles.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.base.BaseViewHolder
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.databinding.ItemArticleEmptyViewBinding
import top.easelink.lcg.databinding.ItemArticleViewBinding
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.ui.main.article.view.PostPreviewDialog
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleEmptyItemViewModel
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleEmptyItemViewModel.ArticleEmptyItemViewModelListener
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleFetcher
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.source.model.Article
import java.lang.ref.WeakReference

class ArticlesAdapter(
    private var articleFetcher: ArticleFetcher
) : RecyclerView.Adapter<BaseViewHolder>() {

    private var fragmentManager: WeakReference<FragmentManager>? = null
    private val mArticleList: MutableList<Article> = mutableListOf()

    override fun getItemCount(): Int {
        return when {
            mArticleList.isEmpty() -> 1
            mArticleList.size > 10 -> mArticleList.size + 1
            else -> mArticleList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mArticleList.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            if (position == mArticleList.size) {
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
                val binding = ItemArticleViewBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding)
            }
            VIEW_TYPE_EMPTY -> {
                val binding = ItemArticleEmptyViewBinding.inflate(inflater, parent, false)
                EmptyViewHolder(binding)
            }
            else -> {
                val binding = ItemArticleEmptyViewBinding.inflate(inflater, parent, false)
                EmptyViewHolder(binding)
            }
        }
    }

    fun addItems(articleList: List<Article>) {
        mArticleList.addAll(articleList)
        notifyDataSetChanged()
    }

    // 位置必须取数据列表旧长度；空视图占位或 footer 的存在性一变，就不是单纯插入
    fun appendItems(articles: List<Article>) {
        if (articles.isEmpty()) return
        val start = mArticleList.size
        val hadFooter = start > 10
        val wasEmpty = start == 0
        mArticleList.addAll(articles)
        if (wasEmpty || hadFooter != (mArticleList.size > 10)) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeInserted(start, articles.size)
        }
    }

    // 必须发通知：下拉刷新会单独调用它，否则 itemCount 与 RecyclerView 不一致
    fun clearItems() {
        if (mArticleList.isEmpty()) return
        mArticleList.clear()
        notifyDataSetChanged()
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = WeakReference(fragmentManager)
    }

    inner class ArticleViewHolder internal constructor(
        private val binding: ItemArticleViewBinding
    ) : BaseViewHolder(binding.root) {
        override fun onBind(position: Int) {
            val article = mArticleList[position]
            binding.apply {
                layout.setOnLongClickListener {
                    fragmentManager?.get()?.let { fm ->
                        PostPreviewDialog.newInstance(article.url)
                            .show(fm, PostPreviewDialog.TAG)
                    }
                    true
                }
                layout.setOnClickListener {
                    EventBus.getDefault().post(OpenArticleEvent(article.url))
                }
                titleTextView.text = article.title
                authorTextView.text = article.author
                dateTextView.text = article.date
                replyAndView.text = article.let { "${it.reply} / ${it.view}" }
                origin.text = article.origin
                if (article.isRecommended) {
                    recommendFlag.visibility = View.VISIBLE
                } else {
                    recommendFlag.visibility = View.GONE
                }
                if (article.author == UserDataRepo.username) {
                    stamp.apply {
                        visibility = View.VISIBLE
                        setStampColor(ContextCompat.getColor(root.context, R.color.orange))
                        setText(root.context.getString(R.string.my_post))
                        reDraw()
                    }
                } else {
                    when (article.helpCoin) {
                        0 -> stamp.visibility = View.GONE
                        -1 -> stamp.apply {
                            setDrawSpotEnable(true)
                            setStampColor(ContextCompat.getColor(root.context, R.color.light_gray))
                            setText(root.context.getString(R.string.help_request_solved))
                            visibility = View.VISIBLE
                            reDraw()
                        }
                        else -> stamp.apply {
                            stamp.setDrawSpotEnable(true)
                            stamp.setStampColor(
                                ContextCompat.getColor(
                                    root.context,
                                    R.color.colorAccent
                                )
                            )
                            stamp.setText(article.helpCoin.toString())
                            stamp.visibility = View.VISIBLE
                            stamp.reDraw()
                        }
                    }
                }
            }
        }
    }

    inner class EmptyViewHolder internal constructor(private val binding: ItemArticleEmptyViewBinding) :
        BaseViewHolder(binding.root), ArticleEmptyItemViewModelListener {
        override fun onBind(position: Int) {
            val emptyItemViewModel =
                ArticleEmptyItemViewModel(
                    this
                )
            binding.viewModel = emptyItemViewModel
        }

        override fun onRetryClick() {
            articleFetcher.fetchArticles(ArticleFetcher.FetchType.FETCH_INIT) {}
        }
    }

    inner class LoadMoreViewHolder internal constructor(
        private val binding: ItemLoadMoreViewBinding // 接受 ItemLoadMoreViewBinding 对象
    ) : BaseViewHolder(binding.root) { // 将绑定对象的根视图传递给 BaseViewHolder
        override fun onBind(position: Int) {
            binding.loading.visibility = View.VISIBLE // loading -> loading
            articleFetcher.fetchArticles(ArticleFetcher.FetchType.FETCH_MORE) {
                binding.root.post { // view.post -> binding.root.post
                    binding.loading.visibility = View.GONE // loading -> loading
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