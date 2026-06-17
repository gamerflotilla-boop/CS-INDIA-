package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Document
import java.io.File
import java.io.FileOutputStream

object ExportManager {

    /**
     * Generates a standard high-quality PDF of a document, drawing the enhanced visual scaled to fit A4 dimensions.
     */
    fun generatePdf(context: Context, document: Document, bitmap: Bitmap): File {
        val pdfDocument = PdfDocument()
        
        // Standard A4 dimensions (72 points per inch) = 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val pageNumber = 1
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Background canvas fill (white)
        canvas.drawColor(Color.WHITE)
        
        // Draw document title and date header
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }
        
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(document.timestamp))
            
        canvas.drawText(document.title.uppercase(), 36f, 40f, headerPaint)
        canvas.drawText("Generated on: $dateString  •  Category: ${document.category}  •  Priority: ${document.priorityLabel}", 36f, 54f, subPaint)
        
        // Scale and draw image to fill the main page area (maintaining aspect ratio)
        val rectLeft = 36f
        val rectTop = 64f
        val rectRight = pageScaleBoundary(pageWidth - 36f)
        val rectBottom = pageScaleBoundary(pageHeight - 64f)
        
        val maxImgWidth = rectRight - rectLeft
        val maxImgHeight = rectBottom - rectTop
        
        val imgWidth = bitmap.width.toFloat()
        val imgHeight = bitmap.height.toFloat()
        
        val scale = Math.min(maxImgWidth / imgWidth, maxImgHeight / imgHeight)
        
        val finalWidth = imgWidth * scale
        val finalHeight = imgHeight * scale
        
        // Center-align the scaled document in the remaining layout space
        val drawLeft = rectLeft + (maxImgWidth - finalWidth) / 2
        val drawTop = rectTop + (maxImgHeight - finalHeight) / 2
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth.toInt().coerceAtLeast(1), finalHeight.toInt().coerceAtLeast(1), true)
        canvas.drawBitmap(scaledBitmap, drawLeft, drawTop, Paint())
        
        // Bottom logo / footer watermark (resembling professional scan watermarks)
        val footerPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 9f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scanned with Doc Scanner Pro", (pageWidth / 2).toFloat(), (pageHeight - 24).toFloat(), footerPaint)
        
        pdfDocument.finishPage(page)
        
        // Save to cache/exports directory
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "${document.title.replace(" ", "_")}.pdf")
        
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        
        pdfDocument.close()
        return pdfFile
    }

    /**
     * Generates a multi-page PDF document combining all enhanced scan pages (A4 page dimensions).
     */
    fun generateMultiPagePdf(context: Context, document: Document, bitmaps: List<Bitmap>): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            // Draw clean background
            canvas.drawColor(Color.WHITE)
            
            // Draw header info
            val headerPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 14f
                isAntiAlias = true
            }
            val subPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            }
            
            val dateString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(document.timestamp))
                
            canvas.drawText("${document.title.uppercase()} - Page ${index + 1}", 36f, 40f, headerPaint)
            canvas.drawText("Generated on: $dateString  •  Category: ${document.category}  •  Priority: ${document.priorityLabel}", 36f, 54f, subPaint)
            
            // Scale image to page bounds
            val rectLeft = 36f
            val rectTop = 64f
            val rectRight = (pageWidth - 36f)
            val rectBottom = (pageHeight - 64f)
            
            val maxImgWidth = rectRight - rectLeft
            val maxImgHeight = rectBottom - rectTop
            
            val imgWidth = bitmap.width.toFloat()
            val imgHeight = bitmap.height.toFloat()
            
            val scale = Math.min(maxImgWidth / imgWidth, maxImgHeight / imgHeight)
            val finalWidth = imgWidth * scale
            val finalHeight = imgHeight * scale
            
            val drawLeft = rectLeft + (maxImgWidth - finalWidth) / 2
            val drawTop = rectTop + (maxImgHeight - finalHeight) / 2
            
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth.toInt().coerceAtLeast(1), finalHeight.toInt().coerceAtLeast(1), true)
            canvas.drawBitmap(scaledBitmap, drawLeft, drawTop, Paint())
            
            // Bottom identifier / watermark
            val footerPaint = Paint().apply {
                color = Color.LTGRAY
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Page ${index + 1} of ${bitmaps.size}  |  Scanned with Doc Scanner Pro", (pageWidth / 2).toFloat(), (pageHeight - 24).toFloat(), footerPaint)
            
            pdfDocument.finishPage(page)
        }

        // Save combined PDF in exports cache
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "${document.title.replace(" ", "_")}_MultiPage.pdf")
        
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        
        pdfDocument.close()
        return pdfFile
    }

    private fun pageScaleBoundary(value: Float): Float {
        return if (value < 1f) 1f else value
    }

    /**
     * Generates a clean plaintext file containing only the extracted OCR text of the document.
     */
    fun generateTxt(context: Context, document: Document): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val txtFile = File(exportDir, "${document.title.replace(" ", "_")}.txt")
        
        FileOutputStream(txtFile).use { out ->
             val contentBytes = """
             === DOC SCANNER PROFESSIONAL EXTRACTED TEXT ===
             Document Title : ${document.title}
             Folder ID      : ${document.folderId ?: "None (General)"}
             Category       : ${document.category}
             Date Created   : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(document.timestamp))}
             ================================================
             
             ${document.extractedText}
             """.trimIndent().toByteArray()
             out.write(contentBytes)
        }
        return txtFile
    }

    /**
     * Spawns an Android share sheet payload targeting either PDF, plain text, or image mime types.
     */
    fun shareFile(context: Context, file: File, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Doc Scanner Professional: ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Sending scanned document: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share Document via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
