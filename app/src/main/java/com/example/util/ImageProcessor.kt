package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

object ImageProcessor {

    /**
     * Applies perspective warp (deskewing) using android's native Matrix polyToPoly API.
     * Takes a Bitmap and 4 corner points in normalized coordinates [0.0, 1.0].
     * Coordinates sequence: [Top-Left, Top-Right, Bottom-Right, Bottom-Left]
     */
    fun cropAndDeskew(
        srcBitmap: Bitmap,
        topLeft: PointF,
        topRight: PointF,
        bottomRight: PointF,
        bottomLeft: PointF
    ): Bitmap {
        val w = srcBitmap.width.toFloat()
        val h = srcBitmap.height.toFloat()

        // Source points in native pixels
        val srcPoints = floatArrayOf(
            topLeft.x * w, topLeft.y * h,
            topRight.x * w, topRight.y * h,
            bottomRight.x * w, bottomRight.y * h,
            bottomLeft.x * w, bottomLeft.y * h
        )

        // Determine destination width/height based on distances between points
        val widthTop = hypot((topRight.x - topLeft.x) * w, (topRight.y - topLeft.y) * h)
        val widthBottom = hypot((bottomRight.x - bottomLeft.x) * w, (bottomRight.y - bottomLeft.y) * h)
        val targetWidth = max(widthTop, widthBottom).toInt().coerceIn(100, 4000)

        val heightLeft = hypot((bottomLeft.x - topLeft.x) * w, (bottomLeft.y - topLeft.y) * h)
        val heightRight = hypot((bottomRight.x - topRight.x) * w, (bottomRight.y - topRight.y) * h)
        val targetHeight = max(heightLeft, heightRight).toInt().coerceIn(100, 4000)

        // Destination points forming standard rectangle
        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        // polyToPoly is native 4 point projective transformation helper
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        return if (success) {
            val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(srcBitmap, matrix, paint)
            resultBitmap
        } else {
            srcBitmap // fallback
        }
    }

    /**
     * Applies requested document filters (Magic Color, B&W / Grayscale, Original)
     */
    fun applyFilter(bitmap: Bitmap, filterName: String): Bitmap {
        return when (filterName) {
            "Magic Color" -> {
                // Magic Color boosts contrast and white balance for document scanning pop
                val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint()

                // High contrast matrix (levels and highlights booster)
                val contrast = 1.4f
                val brightness = 20f
                val saturation = 1.1f

                val colorMatrix = ColorMatrix().apply {
                    // scale contrast
                    set(floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                
                val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
                colorMatrix.postConcat(satMatrix)

                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                result
            }
            "B&W / Grayscale" -> {
                // High contrast Grayscale for clean textual scanning output
                val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint()

                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                
                // Boost contrast dramatically to create clean black text and white page background
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    1.7f, 0f, 0f, 0f, -40f,
                    0f, 1.7f, 0f, 0f, -40f,
                    0f, 0f, 1.7f, 0f, -40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                grayMatrix.postConcat(contrastMatrix)

                paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                result
            }
            else -> bitmap // "Original"
        }
    }

    /**
     * Save bitmap to internal cache directory and return standard local File path
     */
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String): String {
        val directory = File(context.cacheDir, "scanned_docs").apply { mkdirs() }
        val file = File(directory, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
