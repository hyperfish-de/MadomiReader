package com.example.madomireader.domain.model

/**
 * Hộp bao (toạ độ trong không gian ảnh bitmap gốc) — thuần JVM, không phụ thuộc Android,
 * nên có thể unit-test trực tiếp.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = (width.coerceAtLeast(0f)) * (height.coerceAtLeast(0f))

    fun union(other: BoundingBox): BoundingBox = BoundingBox(
        left = minOf(left, other.left),
        top = minOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )

    fun intersects(other: BoundingBox): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun intersectionArea(other: BoundingBox): Float {
        val w = minOf(right, other.right) - maxOf(left, other.left)
        val h = minOf(bottom, other.bottom) - maxOf(top, other.top)
        if (w <= 0f || h <= 0f) return 0f
        return w * h
    }

    fun intersectionOverUnion(other: BoundingBox): Float {
        val inter = intersectionArea(other)
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }
}

/**
 * Một dòng chữ do OCR nhận được.
 */
data class OcrLine(
    val text: String,
    val box: BoundingBox,
)

/**
 * Nhiều [OcrLine] được gom lại thành một khung thoại / ô chú thích.
 */
data class OcrBlock(
    val lines: List<OcrLine>,
) {
    val text: String = lines
        .joinToString(" ") { it.text.trim() }
        .replace(Regex("\\s+"), " ")
        .trim()

    val box: BoundingBox = lines
        .map { it.box }
        .reduce { acc, box -> acc.union(box) }

    val lineCount: Int get() = lines.size
}

enum class TranslationStatus { IDLE, RUNNING, DONE, ERROR }

/**
 * Một khung thoại đã dịch, kèm màu nền/màu chữ để vẽ đè lên ảnh gốc.
 */
data class TranslatedBlock(
    val box: BoundingBox,
    val sourceText: String,
    val translatedText: String,
    val backgroundColor: Int,
    val textColor: Int,
)

/**
 * Trạng thái dịch của một trang truyện.
 *
 * [sourceWidth] / [sourceHeight] là kích thước bitmap đã đưa vào OCR, dùng để
 * quy đổi toạ độ khung thoại sang kích thước hiển thị thực tế.
 */
data class PageTranslation(
    val status: TranslationStatus = TranslationStatus.IDLE,
    val blocks: List<TranslatedBlock> = emptyList(),
    val errorMessage: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
) {
    val isDone: Boolean get() = status == TranslationStatus.DONE
    val isRunning: Boolean get() = status == TranslationStatus.RUNNING
}
