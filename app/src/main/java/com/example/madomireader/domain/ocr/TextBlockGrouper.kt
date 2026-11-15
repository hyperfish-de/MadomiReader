package com.example.madomireader.domain.ocr

import com.example.madomireader.domain.model.BoundingBox
import com.example.madomireader.domain.model.OcrBlock
import com.example.madomireader.domain.model.OcrLine
import kotlin.math.max
import kotlin.math.min

/**
 * Gom các dòng chữ do OCR trả về thành từng khung thoại / ô chú thích.
 *
 * Ý tưởng: hai dòng thuộc cùng một bong bóng thoại thì khoảng cách dọc giữa chúng
 * nhỏ (so với chiều cao chữ) và chúng nằm chồng lên nhau theo trục ngang.
 * Thuật toán là greedy, thuần JVM nên unit-test được.
 */
object TextBlockGrouper {

    private const val DEFAULT_VERTICAL_GAP_FACTOR = 1.1f
    private const val DEFAULT_HORIZONTAL_GAP_FACTOR = 1.4f

    fun group(
        lines: List<OcrLine>,
        verticalGapFactor: Float = DEFAULT_VERTICAL_GAP_FACTOR,
        horizontalGapFactor: Float = DEFAULT_HORIZONTAL_GAP_FACTOR,
    ): List<OcrBlock> {
        val usable = lines
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy({ it.box.top }, { it.box.left }))

        if (usable.isEmpty()) return emptyList()

        val clusters = ArrayList<MutableList<OcrLine>>()

        for (line in usable) {
            var bestIndex = -1
            var bestScore = Float.MAX_VALUE

            for (index in clusters.indices) {
                val clusterBox = clusters[index]
                    .map { it.box }
                    .reduce { acc, box -> acc.union(box) }

                val reference = min(clusterBox.height, line.box.height).coerceAtLeast(1f)

                val verticalGap = when {
                    line.box.top >= clusterBox.bottom -> line.box.top - clusterBox.bottom
                    clusterBox.top >= line.box.bottom -> clusterBox.top - line.box.bottom
                    else -> 0f
                }

                val horizontalGap = when {
                    line.box.left >= clusterBox.right -> line.box.left - clusterBox.right
                    clusterBox.left >= line.box.right -> clusterBox.left - line.box.right
                    else -> 0f
                }

                val fitsVertically = verticalGap <= reference * verticalGapFactor
                val fitsHorizontally = horizontalGap <= reference * horizontalGapFactor

                if (fitsVertically && fitsHorizontally) {
                    val score = verticalGap + horizontalGap * 0.35f
                    if (score < bestScore) {
                        bestScore = score
                        bestIndex = index
                    }
                }
            }

            if (bestIndex >= 0) {
                clusters[bestIndex].add(line)
            } else {
                clusters.add(mutableListOf(line))
            }
        }

        return clusters
            .map { cluster ->
                OcrBlock(cluster.sortedWith(compareBy({ it.box.top }, { it.box.left })))
            }
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy({ it.box.top }, { it.box.left }))
    }

    /**
     * Loại bỏ các dòng trùng lặp — chuyện thường gặp khi chạy nhiều bộ nhận diện
     * (Latin + Trung + Nhật + Hàn) trên cùng một ảnh.
     */
    fun deduplicate(lines: List<OcrLine>, iouThreshold: Float = 0.25f): List<OcrLine> {
        val result = ArrayList<OcrLine>(lines.size)
        for (candidate in lines.sortedByDescending { it.text.length }) {
            val duplicated = result.any { kept ->
                val iou = kept.box.intersectionOverUnion(candidate.box)
                val sameText = kept.text.equals(candidate.text, ignoreCase = true) ||
                    kept.text.contains(candidate.text, ignoreCase = true) ||
                    candidate.text.contains(kept.text, ignoreCase = true)
                iou >= iouThreshold && sameText
            }
            if (!duplicated) result.add(candidate)
        }
        return result.sortedWith(compareBy({ it.box.top }, { it.box.left }))
    }

    /** Kẹp mọi toạ độ vào trong biên của ảnh. */
    fun clampToImage(lines: List<OcrLine>, width: Int, height: Int): List<OcrLine> {
        if (width <= 0 || height <= 0) return lines
        return lines.map { line ->
            val box: BoundingBox = line.box
            line.copy(
                box = BoundingBox(
                    left = box.left.coerceIn(0f, width.toFloat()),
                    top = box.top.coerceIn(0f, height.toFloat()),
                    right = box.right.coerceIn(0f, width.toFloat()),
                    bottom = box.bottom.coerceIn(0f, height.toFloat()),
                ),
            )
        }.filter { it.box.width > 1f && it.box.height > 1f }
    }

    /** Ước lượng số dòng tối đa nên gửi đi dịch trong một khung. */
    fun mergeAdjacent(blocks: List<OcrBlock>, maxGap: Float): List<OcrBlock> {
        if (blocks.size < 2) return blocks
        val sorted = blocks.sortedBy { it.box.top }
        val merged = ArrayList<OcrBlock>()
        var current = sorted.first()
        for (index in 1 until sorted.size) {
            val next = sorted[index]
            val gap = next.box.top - current.box.bottom
            val horizontalOverlap = current.box.left < next.box.right && next.box.left < current.box.right
            if (gap in 0f..maxGap && horizontalOverlap) {
                current = OcrBlock(current.lines + next.lines)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /** Tỉ lệ diện tích chữ so với toàn ảnh — dùng để bỏ qua trang ít chữ. */
    fun textCoverage(blocks: List<OcrBlock>, imageWidth: Int, imageHeight: Int): Float {
        if (imageWidth <= 0 || imageHeight <= 0) return 0f
        val total = blocks.fold(0f) { acc, block -> acc + block.box.area }
        return (total / (imageWidth.toFloat() * imageHeight.toFloat())).coerceIn(0f, 1f)
    }

    /** Khoảng cách dọc lớn nhất cho phép khi gộp hai khung đã nhận diện. */
    fun maxMergeGap(height: Float): Float = max(4f, height * 0.02f)
}
