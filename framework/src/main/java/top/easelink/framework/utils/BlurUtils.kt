@file:Suppress("unused")

package top.easelink.framework.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BlurUtils {
    private const val BITMAP_SCALE = 0.4f // 平衡性能和质量
    private const val MAX_RADIUS = 25f // 最大模糊半径限制

    fun blur(source: Bitmap, radius: Int, useRenderEffect: Boolean = true): Bitmap? {
        if (radius < 1) return source

        val safeRadius = min(radius, MAX_RADIUS.toInt())
        return try {
            when {
                useRenderEffect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    renderEffectBlur(source, safeRadius.toFloat())
                else -> optimizedFastBlur(source, safeRadius)
            }
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun renderEffectBlur(source: Bitmap, radius: Float): Bitmap {
        return createBitmap(source.width, source.height).applyCanvas {
            val paint = Paint().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        radius, radius,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    try {
                        javaClass.getMethod(
                            "setRenderEffect",
                            android.graphics.RenderEffect::class.java
                        ).invoke(this, renderEffect)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            drawBitmap(source, 0f, 0f, paint)
        }
    }

    private fun optimizedFastBlur(original: Bitmap, radius: Int): Bitmap {
        val scaledWidth = max((original.width * BITMAP_SCALE).roundToInt(), 1)
        val scaledHeight = max((original.height * BITMAP_SCALE).roundToInt(), 1)
        val input = original.scale(scaledWidth, scaledHeight, true)

        val pixels = IntArray(scaledWidth * scaledHeight).apply {
            input.getPixels(this, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)
        }

        BlurProcessor(scaledWidth, scaledHeight, radius).process(pixels)

        return createBitmap(scaledWidth, scaledHeight).apply {
            setPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)
        }.scale(original.width, original.height, true)
    }

    private class BlurProcessor(
        private val width: Int,
        private val height: Int,
        private val radius: Int
    ) {
        private val div = radius * 2 + 1
        private val divSum = div * div
        private val dv = IntArray(256 * divSum).apply {
            for (i in indices) this[i] = i / divSum
        }

        fun process(pixels: IntArray) {
            val r = IntArray(pixels.size)
            val g = IntArray(pixels.size)
            val b = IntArray(pixels.size)

            horizontalBlur(pixels, r, g, b)
            verticalBlur(r, g, b, pixels)
        }

        private fun horizontalBlur(src: IntArray, r: IntArray, g: IntArray, b: IntArray) {
            for (y in 0 until height) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (i in -radius..radius) {
                    val pixel = src[clampX(i) + y * width]
                    rSum += Color.red(pixel)
                    gSum += Color.green(pixel)
                    bSum += Color.blue(pixel)
                }

                for (x in 0 until width) {
                    r[x + y * width] = dv[rSum]
                    g[x + y * width] = dv[gSum]
                    b[x + y * width] = dv[bSum]

                    val leftPixel = src[clampX(x - radius) + y * width]
                    val rightPixel = src[clampX(x + radius + 1) + y * width]

                    rSum += Color.red(rightPixel) - Color.red(leftPixel)
                    gSum += Color.green(rightPixel) - Color.green(leftPixel)
                    bSum += Color.blue(rightPixel) - Color.blue(leftPixel)
                }
            }
        }

        private fun verticalBlur(r: IntArray, g: IntArray, b: IntArray, dst: IntArray) {
            for (x in 0 until width) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (i in -radius..radius) {
                    val row = clampY(i)
                    rSum += r[x + row * width]
                    gSum += g[x + row * width]
                    bSum += b[x + row * width]
                }

                for (y in 0 until height) {
                    dst[x + y * width] = Color.rgb(dv[rSum], dv[gSum], dv[bSum])

                    val topIndex = x + clampY(y - radius) * width
                    val bottomIndex = x + clampY(y + radius + 1) * width

                    rSum += r[bottomIndex] - r[topIndex]
                    gSum += g[bottomIndex] - g[topIndex]
                    bSum += b[bottomIndex] - b[topIndex]
                }
            }
        }

        private fun clampX(x: Int) = max(0, min(width - 1, x))
        private fun clampY(y: Int) = max(0, min(height - 1, y))
    }
}