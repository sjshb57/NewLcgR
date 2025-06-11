package top.easelink.lcg.ui.main.forumnav.view

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import org.greenrobot.eventbus.EventBus
import top.easelink.framework.customview.linkagerv.adapter.viewholder.LinkageSecondaryFooterViewHolder
import top.easelink.framework.customview.linkagerv.adapter.viewholder.LinkageSecondaryHeaderViewHolder
import top.easelink.framework.customview.linkagerv.adapter.viewholder.LinkageSecondaryViewHolder
import top.easelink.framework.customview.linkagerv.bean.BaseGroupedItem
import top.easelink.framework.customview.linkagerv.contract.ILinkageSecondaryAdapterConfig
import top.easelink.lcg.R
import top.easelink.lcg.databinding.ItemForumSecondaryHeaderBinding
import top.easelink.lcg.databinding.ItemForumSecondaryLinearBinding
import top.easelink.lcg.ui.main.forumnav.model.ChildForumItemInfo
import top.easelink.lcg.ui.main.forumnav.model.ForumGroupedItem
import top.easelink.lcg.ui.main.model.OpenForumEvent
import top.easelink.lcg.ui.webview.view.WebViewActivity

internal class ForumsSecondaryAdapterConfig :
    ILinkageSecondaryAdapterConfig<ForumGroupedItem.ItemInfo> {
    private lateinit var mContext: Context
    override fun setContext(context: Context) {
        mContext = context
    }

    override fun getGridLayoutId(): Int {
        return 0
    }

    override fun getLinearLayoutId(): Int {
        return R.layout.item_forum_secondary_linear
    }

    override fun getHeaderLayoutId(): Int {
        return R.layout.item_forum_secondary_header
    }

    override fun getFooterLayoutId(): Int {
        return R.layout.item_forum_secondary_footer
    }

    override fun getHeaderTextViewId(): Int {
        return R.id.secondary_header
    }

    override fun getSpanCountOfGridMode(): Int {
        return 0
    }

    override fun onBindViewHolder(
        holder: LinkageSecondaryViewHolder,
        item: BaseGroupedItem<ForumGroupedItem.ItemInfo>
    ) {
        val binding = ItemForumSecondaryLinearBinding.bind(holder.itemView)
        binding.root.setOnClickListener {
            if (item.info.pageUrl.startsWith("forum")) {
                EventBus.getDefault()
                    .post(OpenForumEvent(item.info.title, item.info.pageUrl, true))
            } else {
                WebViewActivity.startWebViewWith(item.info.pageUrl, mContext)
            }
        }
        item.info.let {
            binding.title.text = it.title
            if (it.desc.isNullOrBlank()) {
                binding.desc.visibility = View.GONE
            } else {
                binding.desc.visibility = View.VISIBLE
                binding.desc.text = it.desc
            }
        }

        bindChildren(binding.childrenGridContainer, item.info.children)
    }

    private fun bindChildren(gridView: GridView, children: List<ChildForumItemInfo>?) {
        gridView.apply {
            if (children.isNullOrEmpty()) {
                visibility = View.GONE
                return
            } else {
                visibility = View.VISIBLE
            }
            adapter = ChildGridViewAdapter(
                mContext,
                R.layout.item_forums_children_grid
            ).also {
                it.addAll(children)
                it.notifyDataSetChanged()
            }
            onItemClickListener =
                AdapterView.OnItemClickListener { parent: AdapterView<*>,
                                                  v: View,
                                                  position: Int,
                                                  _: Long ->
                    (parent.adapter as ChildGridViewAdapter).getItem(position)?.let {
                        if (it.pageUrl.startsWith("forum")) {
                            EventBus.getDefault()
                                .post(OpenForumEvent(it.title.orEmpty(), it.pageUrl, true))
                        } else {
                            WebViewActivity.startWebViewWith(it.pageUrl, v.context)
                        }
                    }
                }
        }
    }

    override fun onBindHeaderViewHolder(
        holder: LinkageSecondaryHeaderViewHolder,
        item: BaseGroupedItem<ForumGroupedItem.ItemInfo>
    ) {
        val binding = ItemForumSecondaryHeaderBinding.bind(holder.itemView)
        binding.secondaryHeader.text = item.header
    }

    override fun onBindFooterViewHolder(
        holder: LinkageSecondaryFooterViewHolder,
        item: BaseGroupedItem<ForumGroupedItem.ItemInfo>
    ) {
        // Footer 视图没有具体的视图操作，所以保持不变
    }
}