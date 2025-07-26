package top.easelink.framework.recyclerview

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

@Suppress("unused")
open class BaseItemClickListener(
    private val listener: OnItemClickListener
) : RecyclerView.SimpleOnItemTouchListener() {

    private var gestureDetector: GestureDetector? = null

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (gestureDetector == null) {
            initGestureDetector(rv)
        }
        return gestureDetector?.onTouchEvent(e) ?: false
    }

    private fun initGestureDetector(recyclerView: RecyclerView) {
        gestureDetector = GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                recyclerView.findChildViewUnder(e.x, e.y)?.let { child ->
                    listener.onItemClick(child, recyclerView.getChildAdapterPosition(child))
                    return true
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                recyclerView.findChildViewUnder(e.x, e.y)?.let { child ->
                    listener.onItemLongClick(child, recyclerView.getChildAdapterPosition(child))
                }
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP) {
                    recyclerView.findChildViewUnder(e.x, e.y)?.let { child ->
                        return listener.onItemDoubleClick(child, recyclerView.getChildAdapterPosition(child))
                    }
                }
                return false
            }
        })
    }
}