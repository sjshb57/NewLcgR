package top.easelink.framework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.content.res.AppCompatResources
import top.easelink.framework.R

class ELIconButton : AppCompatButton {
    private var resourceId = 0
    private var stringId = 0
    private var drawable: Drawable? = null
    private var text: String? = null

    private val defValue = 40f

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        isClickable = true
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ELIconButton)
        try {
            resourceId = typedArray.getResourceId(R.styleable.ELIconButton_el_drawable, 0)
            if (resourceId != 0) {
                drawable = AppCompatResources.getDrawable(context, resourceId)
            }
            stringId = typedArray.getResourceId(R.styleable.ELIconButton_el_text, 0)
            if (stringId != 0) {
                text = context.getString(stringId)
            }
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawable?.let {
            // 图片水平居中显示
            val left = (width - defValue) / 2
            it.setBounds(
                left.toInt(),
                0,
                (left + defValue).toInt(),
                defValue.toInt()
            )
            it.draw(canvas)
        }
    }
}