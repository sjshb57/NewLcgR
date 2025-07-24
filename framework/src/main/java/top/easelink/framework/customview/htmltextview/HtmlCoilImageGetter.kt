package top.easelink.framework.customview.htmltextview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HtmlCoilImageGetter(
    private val context: Context,
    private val textView: TextView,
    private val lifecycleOwner: LifecycleOwner? = null
) : Html.ImageGetter {

    private var currentJob: Job? = null

    override fun getDrawable(url: String): Drawable {
        val holder = BitmapDrawablePlaceholder()

        currentJob?.cancel()

        val scope = lifecycleOwner?.lifecycleScope ?: run {
            val scope = CoroutineScope(Dispatchers.IO)
            textView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    currentJob?.cancel()
                    textView.removeOnAttachStateChangeListener(this)
                }
            })
            scope
        }

        currentJob = scope.launch {
            try {
                val drawable = loadImage(url)
                withContext(Dispatchers.Main) {
                    holder.setDrawable(drawable)
                }
            } catch (e: Exception) {
                // 处理加载失败
            }
        }

        return holder
    }

    private suspend fun loadImage(url: String): Drawable {
        return Coil.imageLoader(context).execute(
            ImageRequest.Builder(context)
                .data(url)
                .apply { lifecycleOwner?.let { lifecycle(it) } }
                .build()
        ).drawable!!
    }

    private inner class BitmapDrawablePlaceholder : BitmapDrawable(
        context.resources,
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    ) {
        private var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }

        fun setDrawable(drawable: Drawable) {
            this.drawable = drawable
            var drawableWidth = drawable.intrinsicWidth
            var drawableHeight = drawable.intrinsicHeight

            val maxWidth = textView.measuredWidth.takeIf { it > 0 } ?: 300
            if (drawableWidth > maxWidth) {
                drawableHeight = (maxWidth * drawableHeight) / drawableWidth
                drawableWidth = maxWidth
            }

            drawable.setBounds(0, 0, drawableWidth, drawableHeight)
            setBounds(0, 0, drawableWidth, drawableHeight)
            textView.text = textView.text
        }
    }
}