package top.easelink.lcg.ui.main.article.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.easelink.framework.topbase.ControllableFragment
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentArticleBinding
import top.easelink.lcg.ui.main.article.view.DownloadLinkDialog.Companion.newInstance as newDownloadLinkDialog
import top.easelink.lcg.ui.main.article.view.ReplyPostDialog.Companion.newInstance as newReplyPostDialog
import top.easelink.lcg.ui.main.article.view.ScreenCaptureDialog.Companion.TAG
import top.easelink.lcg.ui.main.article.viewmodel.ArticleAdapterListener.Companion.FETCH_POST_INIT
import top.easelink.lcg.ui.main.article.viewmodel.ArticleViewModel
import top.easelink.lcg.ui.main.model.ReplyPostEvent
import top.easelink.lcg.ui.main.model.ScreenCaptureEvent
import top.easelink.lcg.ui.main.source.model.Post
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant
import top.easelink.lcg.utils.showMessage

class ArticleFragment : TopFragment(), ControllableFragment {

    companion object {
        fun newInstance(url: String): ArticleFragment {
            return ArticleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARTICLE_URL, url)
                }
            }
        }

        private const val ARTICLE_URL = "article_url"
        private const val REPLY_POST_REQUEST_KEY = "reply_post_request"
    }

    private lateinit var viewModel: ArticleViewModel
    private var articleUrl: String = ""
    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    override fun isControllable(): Boolean = true

    override fun getBackStackTag(): String = articleUrl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        articleUrl = arguments?.getString(ARTICLE_URL).orEmpty()
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.clRootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.postRecyclerView.setPadding(
                binding.postRecyclerView.paddingLeft,
                binding.postRecyclerView.paddingTop,
                binding.postRecyclerView.paddingRight,
                systemBars.bottom
            )

            insets
        }

        viewModel = ViewModelProvider(this)[ArticleViewModel::class.java]
        initObserver()
        setUp()
        setupToolBar()
        viewModel.setUrl(articleUrl)
        viewModel.fetchArticlePost(FETCH_POST_INIT)
    }

    override fun onDetach() {
        super.onDetach()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initObserver() {
        viewModel.articleTitle.observe(viewLifecycleOwner) {
            binding.articleToolbar.title = it
        }
        viewModel.shouldDisplayPosts.observe(viewLifecycleOwner) {
            binding.postRecyclerView.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.blockMessage.observe(viewLifecycleOwner) {
            binding.blockText.text = it
            binding.blockContainer.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isNotFound.observe(viewLifecycleOwner) {
            binding.notFoundContainer.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.fetchingProgressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun setUp() {
        binding.postRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            adapter = ArticleAdapter(viewModel, this@ArticleFragment)

            viewModel.posts.observe(viewLifecycleOwner) { posts ->
                if (posts.isNotEmpty() && posts[0].replyUrl != null) {
                    binding.comment.visibility = View.VISIBLE
                    binding.comment.setOnClickListener {
                        showCommentDialog(posts[0].replyUrl!!)
                    }
                } else {
                    binding.comment.visibility = View.GONE
                }
                (adapter as? ArticleAdapter)?.apply {
                    clearItems()
                    addItems(posts)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.safeGetPost(key: String): Post? {
        return when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                getParcelable(key, Post::class.java)
            }
            else -> getParcelable(key) as? Post
        }
    }

    private fun showCommentDialog(replyUrl: String) {
        try {
            parentFragmentManager.setFragmentResultListener(
                REPLY_POST_REQUEST_KEY,
                viewLifecycleOwner
            ) { _, bundle ->
                bundle.safeGetPost("post")?.let { post ->
                    viewModel.addPostToTop(post)
                    binding.postRecyclerView.scrollToPosition(1)
                    showMessage(R.string.reply_post_succeed)
                } ?: showMessage(R.string.reply_post_failed)
            }

            val dialog = CommentArticleDialog.newInstance(replyUrl)
            dialog.show(parentFragmentManager, "CommentArticleDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupToolBar() {
        binding.articleToolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.article)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_open_in_webview -> {
                        WebViewActivity.startWebViewWith(
                            WebsiteConstant.SERVER_BASE_URL + articleUrl,
                            requireContext()
                        )
                    }
                    R.id.action_extract_urls -> {
                        viewModel.extractDownloadUrl()?.takeIf { it.isNotEmpty() }?.let { urls ->
                            newDownloadLinkDialog(urls).show(
                                parentFragmentManager,
                                "DownloadLinkDialog"
                            )
                        } ?: run {
                            showMessage(R.string.download_link_not_found)
                        }
                    }
                    R.id.action_add_to_my_favorite -> {
                        viewModel.addToFavorite()
                    }
                    else -> { /* Do nothing */ }
                }
                true
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ReplyPostEvent) {
        try {
            newReplyPostDialog(event.replyUrl, event.author).show(
                parentFragmentManager,
                "ReplyPostDialog"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ScreenCaptureEvent) {
        try {
            ScreenCaptureDialog.newInstance(event.imagePath).show(
                parentFragmentManager,
                TAG
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}