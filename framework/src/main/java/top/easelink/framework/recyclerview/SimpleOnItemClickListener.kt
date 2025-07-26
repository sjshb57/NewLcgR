package top.easelink.framework.recyclerview

import android.view.View
import timber.log.Timber

open class SimpleOnItemClickListener : OnItemClickListener {

    override fun onItemClick(view: View, position: Int) {
        Timber.d("onItemClick pos=$position")
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        Timber.d("onItemLongClick pos=$position")
        return false
    }

    override fun onItemDoubleClick(view: View, position: Int): Boolean {
        Timber.d("onItemDoubleClick pos=$position")
        return false
    }
}