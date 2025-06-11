package top.easelink.lcg.ui.main.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.easelink.lcg.databinding.FragmentRankListBinding
import top.easelink.lcg.ui.main.discover.model.RankListModel

class RankListBinder : BaseNavigationBinder<RankListModel, RankListVH>() {

    override fun onBindViewHolder(holder: RankListVH, item: RankListModel) {
        holder.onBind(item, null)
    }

    override fun onBindViewHolder(
        holder: RankListVH,
        item: RankListModel,
        payloads: List<Any>
    ) {
        holder.onBind(item, payloads)
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): RankListVH {
        return RankListVH(
            inflater,
            parent
        )
    }
}

class RankListVH(inflater: LayoutInflater, parentView: ViewGroup) : BaseNavigationViewHolder(
    FragmentRankListBinding.inflate(inflater, parentView, false).root
) {
    private val binding = FragmentRankListBinding.bind(itemView)

    fun onBind(item: RankListModel, payloads: List<Any>?) {
        setUp(item)
    }

    private fun setUp(rankModel: RankListModel) {
        binding.apply {
            rankTips.text = rankModel.notice
            rankRecyclerView.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    orientation = RecyclerView.VERTICAL
                }
                adapter = RankListAdapter()
                    .also {
                        it.addItems(rankModel.listModel)
                    }
            }
        }
    }
}