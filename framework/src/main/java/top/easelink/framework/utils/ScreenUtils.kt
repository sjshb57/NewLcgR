package top.easelink.framework.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import timber.log.Timber

fun Int.dpToPx(context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
)

fun Float.dpToPx(context: Context) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

fun dp2px(context: Context, dp: Float): Float {
    return dp * context.resources.displayMetrics.density
}

fun convertViewToBitmap(view: View?, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return null
    }
    view?.apply {
        try {
            return createBitmap(width, height, config).apply {
                val canvas = Canvas(this)
                layout(left, top, right, bottom)
                draw(canvas)
            }
        } catch (re: RuntimeException) {
            Timber.e(re)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.R)
fun getStatusBarHeight(view: View): Int {
    return view.rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
}