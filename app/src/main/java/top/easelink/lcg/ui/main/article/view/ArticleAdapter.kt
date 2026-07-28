package top.easelink.lcg.ui.main.article.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import top.easelink.framework.base.BaseViewHolder
import top.easelink.framework.customview.htmltextview.DrawPreCodeSpan
import top.easelink.framework.customview.htmltextview.HtmlCoilImageGetter
import top.easelink.framework.utils.*
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo.isLoggedIn
import top.easelink.lcg.account.UserDataRepo.username
import top.easelink.lcg.config.AppConfig
import top.easelink.lcg.databinding.ItemLoadMoreViewBinding
import top.easelink.lcg.databinding.ItemPostViewBinding
import top.easelink.lcg.databinding.ItemReplyViewBinding
import top.easelink.lcg.databinding.LayoutSkeltonArticleBinding
import top.easelink.lcg.ui.main.article.viewmodel.ArticleAdapterListener
import top.easelink.lcg.ui.main.model.OpenArticleEvent
import top.easelink.lcg.ui.main.model.OpenLargeImageViewEvent
import top.easelink.lcg.ui.main.model.ReplyPostEvent
import top.easelink.lcg.ui.main.model.ScreenCaptureEvent
import top.easelink.lcg.ui.main.source.model.Post
import top.easelink.lcg.ui.profile.model.PopUpProfileInfo
import top.easelink.lcg.ui.profile.view.KEY_PROFILE_URL
import top.easelink.lcg.ui.profile.view.PopUpProfileDialog
import top.easelink.lcg.ui.profile.view.ProfileActivity
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant.SERVER_BASE_URL
import top.easelink.lcg.utils.avatar.PlaceholderDrawable
import top.easelink.lcg.utils.avatar.getDefaultAvatar
import top.easelink.lcg.utils.copyContent
import top.easelink.lcg.utils.saveImageToGallery
import top.easelink.lcg.utils.showMessage
import top.easelink.lcg.utils.toTimeStamp
import java.util.*

class ArticleAdapter(
    private val mListener: ArticleAdapterListener,
    private val mFragment: Fragment
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val mPostList: MutableList<Post> = ArrayList()

    /**
     * 还有下一页就显示 LoadMore item。
     * 原实现按 `size > 10` 硬阈值判断 → 短回复贴永远没"加载更多"，长贴第二页拉不到。
     */
    private var hasMoreItem: Boolean = false

    override fun getItemCount() = when {
        mPostList.isEmpty() -> 1 // show empty view
        hasMoreItem -> mPostList.size + 1
        else -> mPostList.size
    }

    override fun getItemViewType(position: Int) = when {
        mPostList.isEmpty() -> VIEW_TYPE_EMPTY
        position == 0 -> VIEW_TYPE_POST
        position == mPostList.size -> VIEW_TYPE_LOAD_MORE
        else -> VIEW_TYPE_REPLY
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_POST -> {
                val binding = ItemPostViewBinding.inflate(inflater, parent, false)
                PostViewHolder(binding)
            }
            VIEW_TYPE_REPLY -> {
                val binding = ItemReplyViewBinding.inflate(inflater, parent, false)
                ReplyViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val binding = ItemLoadMoreViewBinding.inflate(inflater, parent, false)
                LoadMoreViewHolder(binding)
            }
            else -> {
                val binding = LayoutSkeltonArticleBinding.inflate(inflater, parent, false)
                PostEmptyViewHolder(binding.root)
            }
        }
    }

    /** 整列表替换。仅用于"首次加载 / 切换文章"。 */
    @SuppressLint("NotifyDataSetChanged")
    fun setItems(postList: List<Post>) {
        mPostList.clear()
        mPostList.addAll(postList)
        notifyDataSetChanged()
    }

    /** 翻页 append：按区间通知，保持滚动位置 + 不重绑已显示项。 */
    fun appendItems(newPosts: List<Post>) {
        if (newPosts.isEmpty()) return
        val start = mPostList.size
        mPostList.addAll(newPosts)
        notifyItemRangeInserted(start, newPosts.size)
    }

    /** 单条插入（回复成功后插到指定行）。 */
    fun insertItem(position: Int, post: Post) {
        val safePos = position.coerceIn(0, mPostList.size)
        mPostList.add(safePos, post)
        notifyItemInserted(safePos)
    }

    /** 设置/更新 LoadMore item 的显示状态。 */
    fun setHasMore(hasMore: Boolean) {
        if (hasMoreItem == hasMore) return
        val wasShowing = hasMoreItem
        hasMoreItem = hasMore
        // 用 mPostList.size 作为 LoadMore 的位置 —— 即末尾的下一个 item。
        when {
            hasMore && !wasShowing && mPostList.isNotEmpty() -> notifyItemInserted(mPostList.size)
            !hasMore && wasShowing && mPostList.isNotEmpty() -> notifyItemRemoved(mPostList.size)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearItems() {
        mPostList.clear()
        hasMoreItem = false
        notifyDataSetChanged()
    }

    inner class PostViewHolder internal constructor(
        private val binding: ItemPostViewBinding
    ) : BaseViewHolder(binding.root), View.OnClickListener {

        private var post: Post? = null
        private val htmlHttpImageGetter: Html.ImageGetter = HtmlCoilImageGetter(binding.root.context, binding.contentTextView, mFragment)

        override fun onBind(position: Int) {
            val p = mPostList.getOrNull(position) ?: return
            post = p
            with(binding) {
                try {
                    authorTextView.text = p.author
                    dateTextView.text = getDateDiff(p.date)
                    postAvatar.setOnClickListener { _ ->
                        mFragment.runCatching { parentFragmentManager }
                            .getOrNull()
                            ?.let {
                                val location = IntArray(2)
                                postAvatar.getLocationInWindow(location)
                                val popUpInfo = PopUpProfileInfo(
                                    location[0],
                                    location[1],
                                    p.avatar,
                                    p.author,
                                    p.extraInfo,
                                    p.followInfo,
                                    p.profileUrl
                                )
                                PopUpProfileDialog.newInstance(popUpInfo).show(
                                    it,
                                    PopUpProfileDialog::class.java.simpleName
                                )
                            }
                            ?: root.context.startActivity(
                                Intent(root.context, ProfileActivity::class.java).also {
                                    it.putExtra(KEY_PROFILE_URL, p.profileUrl)
                                })
                    }
                    postAvatar.load(p.avatar) {
                        crossfade(false)
                        lifecycle(mFragment)
                        transformations(RoundedCornersTransformation(4.dpToPx(root.context)))
                        error(R.drawable.ic_noavatar_middle_gray)
                    }
                    contentTextView.run {
                        requestFocus()
                        if (AppConfig.articleHandlePreTag) {
                            setClickablePreCodeSpan(ClickablePreCodeSpanImpl())
                            setDrawPreCodeSpan(DrawPreCodeSpan().apply {
                                tableLinkText = context.getString(R.string.tap_for_code)
                            })
                        } else {
                            setClickablePreCodeSpan(null)
                            setDrawPreCodeSpan(null)
                        }
                        setImageTagClickListener { _: Context, imageUrl: String, _: Int ->
                            EventBus.getDefault().post(OpenLargeImageViewEvent(imageUrl))
                        }
                        setOnLinkTagClickListener { c: Context?, url: String ->
                            if (url.startsWith(SERVER_BASE_URL + "thread")) {
                                EventBus.getDefault()
                                    .post(OpenArticleEvent(url.substring(SERVER_BASE_URL.length)))
                            } else {
                                WebViewActivity.startWebViewWith(url, c)
                            }
                        }
                        setHtml(p.content, htmlHttpImageGetter)
                    }
                    postBtnCapture.visibility = View.VISIBLE
                    postBtnCapture.setOnClickListener(this@PostViewHolder)
                    if (isLoggedIn) {
                        postBtnGroup.visibility = View.VISIBLE
                        if (TextUtils.isEmpty(p.replyUrl)) {
                            postBtnReply.visibility = View.GONE
                        } else {
                            postBtnReply.visibility = View.VISIBLE
                            postBtnReply.setOnClickListener(this@PostViewHolder)
                        }
                        if (TextUtils.isEmpty(p.replyAddUrl)) {
                            postBtnThumbUp.visibility = View.GONE
                        } else {
                            postBtnThumbUp.visibility = View.VISIBLE
                            postBtnThumbUp.setOnClickListener(this@PostViewHolder)
                        }
                        postBtnCopy.setOnClickListener(this@PostViewHolder)
                    } else {
                        postBtnGroup.visibility = View.GONE
                    }
                    // fix issue occurs on some manufactures os, like MIUI
                    contentTextView.setOnLongClickListener { true }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        override fun onClick(v: View) {
            post?.let { p ->
                when (v.id) {
                    R.id.post_btn_reply -> p.replyUrl?.let {
                        EventBus.getDefault().post(ReplyPostEvent(it, p.author))
                    }
                    R.id.post_btn_copy -> if (copyContent(p.content, p.author)) {
                        showMessage(R.string.copy_succeed)
                    } else {
                        showMessage(R.string.copy_failed)
                    }
                    R.id.post_btn_thumb_up -> p.replyAddUrl?.let {
                        mListener.replyAdd(it)
                    }
                    R.id.post_btn_capture -> {
                        convertViewToBitmap(itemView, Bitmap.Config.ARGB_8888)?.let {
                            val path = saveImageToGallery(it, System.currentTimeMillis().toString())
                            EventBus.getDefault().post(ScreenCaptureEvent(path))
                        } ?: showMessage(R.string.general_error)
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    inner class ReplyViewHolder internal constructor(
        private val binding: ItemReplyViewBinding
    ) : BaseViewHolder(binding.root), View.OnClickListener {

        private var post: Post? = null
        private val htmlHttpImageGetter: Html.ImageGetter by lazy {
            HtmlCoilImageGetter(binding.root.context, binding.replyContentTextView, mFragment)
        }

        @SuppressLint("SetTextI18n")
        override fun onBind(position: Int) {
            post = mPostList[position]
            post?.let { p ->
                with(binding) {
                    try {
                        // 只有真的登录、且作者名与当前用户匹配才高亮。
                        // 原实现忘了 isLoggedIn 兜底，未登录时 username=""，会把所有 author="" 的匿名回复全染色。
                        if (isLoggedIn && username.isNotEmpty() && p.author == username) {
                            replyCard.strokeColor = ContextCompat.getColor(root.context, R.color.orange)
                            replyCard.strokeWidth = dp2px(root.context, 1f).toInt()
                        } else {
                            replyCard.strokeWidth = 0
                        }
                        replyPosition.text = "#$position"
                        replyAuthorTextView.text = p.author
                        replyDateTextView.text = getDateDiff(p.date)
                        replyAvatar.setOnClickListener { _ ->
                            mFragment.runCatching { parentFragmentManager }
                                .getOrNull()
                                ?.let {
                                    val location = IntArray(2)
                                    replyAvatar.getLocationInWindow(location)
                                    val popUpInfo = PopUpProfileInfo(
                                        location[0],
                                        location[1],
                                        p.avatar,
                                        p.author,
                                        p.extraInfo,
                                        p.followInfo,
                                        p.profileUrl
                                    )
                                    PopUpProfileDialog.newInstance(popUpInfo).show(
                                        it,
                                        PopUpProfileDialog::class.java.simpleName
                                    )
                                } ?: root.context.startActivity(
                                Intent(root.context, ProfileActivity::class.java).also {
                                    it.putExtra(KEY_PROFILE_URL, p.profileUrl)
                                })
                        }
                        replyAvatar.setImageDrawable(null)
                        replyAvatar.load(p.avatar) {
                            crossfade(false)
                            transformations(RoundedCornersTransformation(6.dpToPx(root.context)))
                            placeholder(PlaceholderDrawable)
                            error(getDefaultAvatar(p.avatar))
                        }
                        replyContentTextView.run {
                            if (AppConfig.articleHandlePreTag) {
                                setClickablePreCodeSpan(ClickablePreCodeSpanImpl())
                                val drawTableLinkSpan = DrawPreCodeSpan()
                                    .also {
                                        it.tableLinkText = context.getString(R.string.tap_for_code)
                                    }
                                setDrawPreCodeSpan(drawTableLinkSpan)
                            } else {
                                setClickablePreCodeSpan(null)
                                setDrawPreCodeSpan(null)
                            }
                            setImageTagClickListener { _: Context, imageUrl: String, _: Int ->
                                EventBus.getDefault().post(OpenLargeImageViewEvent(imageUrl))
                            }
                            setOnLinkTagClickListener { c: Context?, url: String ->
                                if (url.startsWith(SERVER_BASE_URL + "thread")) {
                                    EventBus.getDefault()
                                        .post(OpenArticleEvent(url.substring(SERVER_BASE_URL.length)))
                                } else {
                                    WebViewActivity.startWebViewWith(url, c)
                                }
                            }
                            setHtml(p.content, htmlHttpImageGetter)
                        }
                        if (isLoggedIn) {
                            replyBtnGroup.visibility = View.VISIBLE
                            if (TextUtils.isEmpty(p.replyUrl)) {
                                replyBtnReply.visibility = View.GONE
                            } else {
                                replyBtnReply.visibility = View.VISIBLE
                                replyBtnReply.setOnClickListener(this@ReplyViewHolder)
                            }
                            if (TextUtils.isEmpty(p.replyAddUrl)) {
                                replyBtnThumbUp.visibility = View.GONE
                            } else {
                                replyBtnThumbUp.visibility = View.VISIBLE
                                replyBtnThumbUp.setOnClickListener(this@ReplyViewHolder)
                            }
                            replyBtnCopy.setOnClickListener(this@ReplyViewHolder)
                        } else {
                            replyBtnGroup.visibility = View.GONE
                        }
                        // fix issue occurs on some manufactures os, like MIUI
                        replyContentTextView.setOnLongClickListener { true }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }

        override fun onClick(v: View) {
            post?.let { p ->
                when (v.id) {
                    R.id.reply_btn_reply -> p.replyUrl?.let {
                        EventBus.getDefault().post(ReplyPostEvent(it, p.author))
                    }
                    R.id.reply_btn_copy -> if (copyContent(p.content, p.author)) {
                        showMessage(R.string.copy_succeed)
                    } else {
                        showMessage(R.string.copy_failed)
                    }
                    R.id.reply_btn_thumb_up -> p.replyAddUrl?.let {
                        mListener.replyAdd(it)
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    fun getDateDiff(date: String): String {
        val tips = "发表于 "
        return date.replace(tips, "").toTimeStamp()?.let {
            val diff = System.currentTimeMillis() - it
            when (val minutesDiff = diff / 1000 / 60) {
                in 0..1 -> return "${tips}1 分钟前"
                in 1..60 -> return "${tips}${minutesDiff} 分钟前"
                in 60..24 * 60 -> {
                    val hoursDiff = diff / 1000 / 60 / 60
                    return "${tips}${hoursDiff} 小时前"
                }
                in 24 * 60..7 * 24 * 60 -> {
                    val dayDiff = diff / 1000 / 60 / 60 / 24
                    return "${tips}${dayDiff} 天前"
                }
                else -> {
                    date
                }
            }
        } ?: date
    }

    class PostEmptyViewHolder internal constructor(view: View?) :
        BaseViewHolder(view) {
        override fun onBind(position: Int) {}
    }

    inner class LoadMoreViewHolder internal constructor(private val binding: ItemLoadMoreViewBinding) :
        BaseViewHolder(binding.root) {

        override fun onBind(position: Int) {
            // 这里只触发拉取；显示/隐藏由 Adapter.setHasMore 根据 ViewModel.hasMorePages 控制，
            // 不再依赖回调里的 fetchResult（异步未返回前永远是 false，会让 LoadMore 永远 VISIBLE）。
            binding.root.visibility = View.VISIBLE
            mListener.fetchArticlePost(ArticleAdapterListener.FETCH_POST_MORE, null)
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = 0
        private const val VIEW_TYPE_REPLY = 1
        private const val VIEW_TYPE_POST = 2
        private const val VIEW_TYPE_LOAD_MORE = 3
    }
}