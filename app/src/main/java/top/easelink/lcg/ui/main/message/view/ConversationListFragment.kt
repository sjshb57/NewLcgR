package top.easelink.lcg.ui.main.message.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.easelink.framework.topbase.TopFragment
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentConversationListBinding
import top.easelink.lcg.ui.main.message.viewmodel.ConversationListViewModel

class ConversationListFragment : TopFragment() {

    private lateinit var mConversationVM: ConversationListViewModel
    private var _binding: FragmentConversationListBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mConversationVM = ViewModelProvider(this)[ConversationListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRV()
        mConversationVM.fetchConversations()
    }

    private fun setUpRV() {
        binding.conversationList.apply {
            layoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            adapter =
                ConversationListAdapter(
                    mConversationVM
                )
            mConversationVM.apply {
                conversations.observe(viewLifecycleOwner, Observer {
                    (adapter as ConversationListAdapter).run {
                        if (itemCount > 1) {
                            appendItems(it)
                        } else {
                            clearItems()
                            addItems(it)
                        }
                    }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}