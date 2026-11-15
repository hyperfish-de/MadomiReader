package com.example.madomireader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.madomireader.domain.model.PageTranslation
import kotlin.math.min

/**
 * Lớp dịch vẽ đè lên ảnh trang truyện.
 *
 * Mỗi khung thoại đã nhận diện được:
 *  1. che chữ gốc bằng một hình chữ nhật bo góc (màu lấy từ chính ảnh gốc),
 *  2. vẽ bản dịch tiếng Việt vào giữa khung, tự thu nhỏ cỡ chữ cho vừa.
 *
 * Toạ độ trong [PageTranslation.blocks] nằm trong không gian bitmap gốc
 * ([PageTranslation.sourceWidth] × [PageTranslation.sourceHeight]), nên được
 * quy đổi theo kích thước hiển thị thực tế ở đây.
 */
@Composable
fun TranslationOverlay(
    translation: PageTranslation,
    fontSizeScale: Float,
    modifier: Modifier = Modifier,
) {
    val sourceWidth = translation.sourceWidth
    val sourceHeight = translation.sourceHeight

    if (translation.blocks.isEmpty() || sourceWidth <= 0 || sourceHeight <= 0) return

    val measurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val scaleX = size.width / sourceWidth.toFloat()
        val scaleY = size.height / sourceHeight.toFloat()
        if (scaleX <= 0f || scaleY <= 0f) return@Canvas

        translation.blocks.forEach { block ->
            val box = block.box
            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width * scaleX
            val height = box.height * scaleY
            if (width <= 2f || height <= 2f) return@forEach

            val radius = min(width, height) * 0.18f
            drawRoundRect(
                color = Color(block.backgroundColor),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(radius, radius),
            )

            val text = block.translatedText.trim()
            if (text.isEmpty()) return@forEach

            val layout = fitTextLayout(
                measurer = measurer,
                text = text,
                maxWidthPx = width * 0.90f,
                maxHeightPx = height * 0.90f,
                startSizeSp = height.toSp().value,
                fontSizeScale = fontSizeScale,
                color = Color(block.textColor),
            )

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = left + (width - layout.size.width) / 2f,
                    y = top + (height - layout.size.height) / 2f,
                ),
            )
        }
    }
}

/**
 * Đo chữ rồi thu nhỏ dần cho tới khi vừa khung thoại.
 *
 * Bắt đầu từ cỡ chữ "một dòng cao bằng cả khung" rồi nhân dần với hệ số thu nhỏ
 * tính từ tỉ lệ tràn — nhờ vậy chỉ mất vài lần đo thay vì giảm từng 1sp.
 */
private fun fitTextLayout(
    measurer: TextMeasurer,
    text: String,
    maxWidthPx: Float,
    maxHeightPx: Float,
    startSizeSp: Float,
    fontSizeScale: Float,
    color: Color,
): TextLayoutResult {
    val maxWidth = maxWidthPx.coerceAtLeast(8f).toInt()
    val constraints = Constraints(maxWidth = maxWidth)
    val scale = fontSizeScale.coerceIn(0.5f, 2f)

    fun measureAt(sizeSp: Float): TextLayoutResult = measurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = sizeSp.sp,
            lineHeight = (sizeSp * 1.16f).sp,
            textAlign = TextAlign.Center,
        ),
        constraints = constraints,
    )

    fun fits(layout: TextLayoutResult): Boolean =
        !layout.didOverflowHeight &&
            layout.size.height <= maxHeightPx &&
            layout.size.width <= maxWidthPx

    var sizeSp = (startSizeSp * scale).coerceIn(MIN_FONT_SP, MAX_FONT_SP)
    var layout = measureAt(sizeSp)
    var attempt = 0

    while (attempt < MAX_ATTEMPTS && sizeSp > MIN_FONT_SP && !fits(layout)) {
        val byHeight = if (layout.size.height > maxHeightPx) {
            maxHeightPx / layout.size.height.toFloat()
        } else {
            1f
        }
        val byWidth = if (layout.size.width > maxWidthPx) {
            maxWidthPx / layout.size.width.toFloat()
        } else {
            1f
        }
        val factor = min(byHeight, byWidth).coerceIn(0.45f, 0.94f)
        sizeSp = (sizeSp * factor).coerceAtLeast(MIN_FONT_SP)
        layout = measureAt(sizeSp)
        attempt++
    }

    return layout
}

private const val MIN_FONT_SP = 6f
private const val MAX_FONT_SP = 96f
private const val MAX_ATTEMPTS = 24
