package com.landsofjail.assistant.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Prepares a second OCR pass for small or low-contrast game UI text.
 *
 * The original screenshot is always kept as the primary source. The enhanced
 * bitmap is an OCR aid only: all returned coordinates are mapped back to the
 * original screenshot before they are merged with the primary OCR result.
 */
object OcrImagePreprocessor {
    const val UPSCALE_FACTOR = 1.5f

    fun enhanceForOcr(source: Bitmap): Bitmap {
        require(source.width > 0 && source.height > 0)
        val width = ceil(source.width * UPSCALE_FACTOR).toInt().coerceAtLeast(1)
        val height = ceil(source.height * UPSCALE_FACTOR).toInt().coerceAtLeast(1)
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val grayscale = ColorMatrix().apply { setSaturation(0f) }
        val contrast = 1.18f
        val offset = 128f * (1f - contrast)
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        grayscale.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(grayscale)
        }
        Canvas(enhanced).drawBitmap(
            source,
            null,
            Rect(0, 0, width, height),
            paint
        )
        return enhanced
    }

    /** Convert OCR boxes from the enlarged bitmap back to source-image pixels. */
    fun mapBoxesToOriginal(
        boxes: List<OcrTextBox>,
        scaleFactor: Float = UPSCALE_FACTOR
    ): List<OcrTextBox> {
        require(scaleFactor > 0f && scaleFactor.isFinite())
        return boxes.map { box ->
            OcrTextBox(
                text = box.text,
                left = (box.left / scaleFactor).roundToInt(),
                top = (box.top / scaleFactor).roundToInt(),
                right = (box.right / scaleFactor).roundToInt(),
                bottom = (box.bottom / scaleFactor).roundToInt()
            )
        }
    }
}
