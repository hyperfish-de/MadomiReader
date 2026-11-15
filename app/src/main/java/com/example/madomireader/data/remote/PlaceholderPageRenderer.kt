package com.example.madomireader.data.remote

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.madomireader.domain.model.Page
import kotlin.math.max
import kotlin.math.min

/**
 * Vẽ một trang truyện "giả" ngay trên máy (nguồn offline demo).
 *
 * Trang được chia thành các khung truyện, mỗi khung có một bong bóng thoại chứa
 * hội thoại mẫu. Chữ được vẽ to, tương phản cao để ML Kit nhận diện chính xác —
 * nhờ vậy có thể thử toàn bộ luồng OCR → dịch mà không cần tải ảnh từ mạng.
 */
object PlaceholderPageRenderer {

    private const val DEFAULT_WIDTH = 800
    private const val DEFAULT_HEIGHT = 1200

    private val paperColor = Color.rgb(248, 246, 242)
    private val panelColor = Color.rgb(255, 255, 255)
    private val panelBorderColor = Color.rgb(28, 28, 34)
    private val textColor = Color.rgb(18, 18, 22)
    private val accentColor = Color.rgb(124, 77, 255)

    fun render(page: Page): Bitmap {
        val width = if (page.width > 0) page.width else DEFAULT_WIDTH
        val height = if (page.height > 0) page.height else DEFAULT_HEIGHT

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(paperColor)

        val lines = page.placeholderLines.ifEmpty { listOf("…") }
        val margin = width * 0.06f
        val panelHeight = (height - margin * (lines.size + 1)) / lines.size

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(3f, width * 0.006f)
            color = panelBorderColor
        }
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = panelColor
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = accentColor
        }

        val textSize = min(width, height) * 0.045f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        lines.forEachIndexed { index, line ->
            val top = margin + index * (panelHeight + margin)
            val panelRect = RectF(margin, top, width - margin, top + panelHeight)

            canvas.drawRoundRect(panelRect, width * 0.02f, width * 0.02f, panelPaint)
            canvas.drawRoundRect(panelRect, width * 0.02f, width * 0.02f, borderPaint)

            drawSpeechBubble(
                canvas = canvas,
                text = line,
                panelRect = panelRect,
                textPaint = textPaint,
                accentPaint = accentPaint,
                borderPaint = borderPaint,
            )
        }

        drawFooter(canvas, width, height, page.index, textPaint)

        return bitmap
    }

    private fun drawSpeechBubble(
        canvas: Canvas,
        text: String,
        panelRect: RectF,
        textPaint: Paint,
        accentPaint: Paint,
        borderPaint: Paint,
    ) {
        val horizontalPadding = panelRect.width() * 0.06f
        val verticalPadding = panelRect.height() * 0.10f
        val maxTextWidth = panelRect.width() - horizontalPadding * 2f

        val wrapped = wrapText(text, textPaint, maxTextWidth)
        val lineHeight = textPaint.textSize * 1.25f
        val textBlockHeight = lineHeight * wrapped.size
        val textBlockWidth = wrapped.maxOf { textPaint.measureText(it) }

        val bubbleWidth = min(
            panelRect.width() - horizontalPadding,
            textBlockWidth + horizontalPadding * 2f,
        )
        val bubbleHeight = min(
            panelRect.height() - verticalPadding,
            textBlockHeight + verticalPadding * 2f,
        )

        val left = panelRect.left + (panelRect.width() - bubbleWidth) / 2f
        val top = panelRect.top + (panelRect.height() - bubbleHeight) / 2f
        val bubbleRect = RectF(left, top, left + bubbleWidth, top + bubbleHeight)

        val bubbleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val bubbleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderPaint.strokeWidth
            color = Color.rgb(40, 40, 48)
        }
        val radius = min(bubbleWidth, bubbleHeight) * 0.18f
        canvas.drawRoundRect(bubbleRect, radius, radius, bubbleFill)
        canvas.drawRoundRect(bubbleRect, radius, radius, bubbleBorder)

        // Đuôi bong bóng thoại cho giống truyện thật.
        val tail = android.graphics.Path().apply {
            moveTo(left + bubbleWidth * 0.22f, bubbleRect.bottom - 2f)
            lineTo(left + bubbleWidth * 0.16f, bubbleRect.bottom + bubbleHeight * 0.16f)
            lineTo(left + bubbleWidth * 0.34f, bubbleRect.bottom - 2f)
            close()
        }
        canvas.drawPath(tail, bubbleFill)
        canvas.drawPath(tail, bubbleBorder)

        // Chấm accent nhỏ ở góc bong bóng.
        canvas.drawCircle(
            bubbleRect.right - bubbleWidth * 0.08f,
            bubbleRect.top + bubbleHeight * 0.08f,
            max(3f, bubbleWidth * 0.012f),
            accentPaint,
        )

        val textStartY = bubbleRect.top + (bubbleHeight - textBlockHeight) / 2f + textPaint.textSize
        wrapped.forEachIndexed { index, line ->
            val lineWidth = textPaint.measureText(line)
            val x = bubbleRect.left + (bubbleWidth - lineWidth) / 2f
            val y = textStartY + index * lineHeight
            canvas.drawText(line, x, y, textPaint)
        }
    }

    private fun drawFooter(canvas: Canvas, width: Int, height: Int, pageIndex: Int, textPaint: Paint) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 130)
            textSize = width * 0.03f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val label = "Madomi Reader • Trang ${pageIndex + 1}"
        canvas.drawText(label, width * 0.06f, height - height * 0.02f, footerPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.trim().split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return listOf("")
        if (maxWidth <= 0f) return listOf(text)

        val lines = ArrayList<String>(4)
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }
}
