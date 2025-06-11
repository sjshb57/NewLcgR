package top.easelink.lcg.ui.main.message.view

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
import top.easelink.lcg.ui.main.message.viewmodel.NotificationViewModel
import top.easelink.lcg.databinding.FragmentNotificationBinding

class NotificationFragment : TopFragment() {

    private lateinit var notificationViewModel: NotificationViewModel
    private var _binding: FragmentNotificationBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        setupRecyclerView()
        notificationViewModel.fetchNotifications()
    }

    private fun setupRecyclerView() {
        binding.notificationRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            itemAnimator = DefaultItemAnimator()
            adapter =
                NotificationsAdapter(
                    notificationViewModel, this@NotificationFragment
                )
            notificationViewModel.apply {
                isLoading.observe(viewLifecycleOwner, Observer {
                    if (it) {
                        binding.loading.visibility = View.VISIBLE
                        binding.notificationRecyclerView.visibility = View.GONE
                    } else {
                        binding.loading.visibility = View.GONE
                        binding.notificationRecyclerView.visibility = View.VISIBLE
                    }
                })
                notifications
                    .observe(viewLifecycleOwner, Observer { model ->
                        (adapter as NotificationsAdapter).run {
                            if (itemCount > 1) {
                                appendItems(model.notifications)
                            } else {
                                clearItems()
                                addItems(model.notifications)
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