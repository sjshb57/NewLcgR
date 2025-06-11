package top.easelink.lcg.ui.main.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.easelink.lcg.databinding.FragmentForumsNavigationBinding
import top.easelink.lcg.ui.main.discover.model.ForumListModel
import top.easelink.lcg.ui.main.discover.model.ForumNavigationModel


class ForumNavigationBinder : BaseNavigationBinder<ForumListModel, ForumNavigationVH>() {

    override fun onBindViewHolder(holder: ForumNavigationVH, item: ForumListModel) {
        holder.onBind(item, null)
    }

    override fun onBindViewHolder(
        holder: ForumNavigationVH,
        item: ForumListModel,
        payloads: List<Any>
    ) {
        holder.onBind(item, payloads)
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ForumNavigationVH {
        return ForumNavigationVH(inflater, parent)
    }
}

class ForumNavigationVH(inflater: LayoutInflater, parentView: ViewGroup) : BaseNavigationViewHolder(
    FragmentForumsNavigationBinding.inflate(inflater, parentView, false).root
) {
    private val binding = FragmentForumsNavigationBinding.bind(itemView)

    fun onBind(item: ForumListModel, payloads: List<Any>?) {
        setUp(item.forumList)
    }

    private fun setUp(listModel: List<ForumNavigationModel>) {
        binding.apply {
            forumTips.setOnClickListener {
                navigationGrid.smoothScrollToPosition(
                    (navigationGrid.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() + 1
                )
            }
            navigationGrid.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    orientation = RecyclerView.HORIZONTAL
                }
                adapter = ForumNavigationAdapter().also {
                    it.addItems(listModel)
                }
            }
        }
    }
}