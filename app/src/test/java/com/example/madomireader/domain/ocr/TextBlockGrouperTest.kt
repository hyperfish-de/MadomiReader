package com.example.madomireader.domain.ocr

import com.example.madomireader.domain.model.BoundingBox
import com.example.madomireader.domain.model.OcrBlock
import com.example.madomireader.domain.model.OcrLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho thuật toán gom dòng chữ thành khung thoại (thuần JVM, không cần Android).
 */
class TextBlockGrouperTest {

    private fun line(text: String, left: Float, top: Float, right: Float, bottom: Float) =
        OcrLine(text = text, box = BoundingBox(left = left, top = top, right = right, bottom = bottom))

    private fun block(vararg lines: OcrLine) = OcrBlock(lines.toList())

    // ------------------------------------------------------------------ group

    @Test
    fun `group gop hai dong sat nhau thanh mot khung thoai`() {
        val lines = listOf(
            line("Hello", 10f, 10f, 110f, 40f),
            line("world", 10f, 50f, 110f, 80f),
        )

        val blocks = TextBlockGrouper.group(lines)

        assertEquals(1, blocks.size)
        assertEquals(2, blocks[0].lineCount)
        assertEquals("Hello world", blocks[0].text)
        assertEquals(BoundingBox(10f, 10f, 110f, 80f), blocks[0].box)
    }

    @Test
    fun `group tach hai khung thoai nam xa nhau`() {
        val lines = listOf(
            line("A", 10f, 10f, 110f, 40f),
            line("B", 10f, 50f, 110f, 80f),
            line("C", 10f, 300f, 110f, 330f),
        )

        val blocks = TextBlockGrouper.group(lines)

        assertEquals(2, blocks.size)
        assertEquals(2, blocks[0].lineCount)
        assertEquals("C", blocks[1].text)
    }

    @Test
    fun `group bo qua dong trong`() {
        val lines = listOf(
            line("Hello", 10f, 10f, 110f, 40f),
            line("    ", 10f, 50f, 110f, 80f),
            line("", 10f, 90f, 110f, 120f),
        )

        val blocks = TextBlockGrouper.group(lines)

        assertEquals(1, blocks.size)
        assertEquals("Hello", blocks[0].text)
    }

    @Test
    fun `group tra ve danh sach rong khi khong co dong nao`() {
        assertTrue(TextBlockGrouper.group(emptyList()).isEmpty())
    }

    @Test
    fun `group sap xep cac khung theo thu tu doc tu tren xuong`() {
        val lines = listOf(
            line("Duoi", 10f, 400f, 110f, 430f),
            line("Tren", 10f, 10f, 110f, 40f),
        )

        val blocks = TextBlockGrouper.group(lines)

        assertEquals(2, blocks.size)
        assertEquals("Tren", blocks[0].text)
        assertEquals("Duoi", blocks[1].text)
    }

    // ------------------------------------------------------------ deduplicate

    @Test
    fun `deduplicate loai bo dong trung nhau khi chay nhieu bo nhan dien`() {
        val lines = listOf(
            line("Xin chào", 0f, 0f, 100f, 30f),
            line("Xin chào", 2f, 2f, 102f, 32f),
        )

        val result = TextBlockGrouper.deduplicate(lines)

        assertEquals(1, result.size)
        assertEquals("Xin chào", result[0].text)
    }

    @Test
    fun `deduplicate giu lai hai dong khac noi dung du chong nhau`() {
        val lines = listOf(
            line("Xin chào", 0f, 0f, 100f, 30f),
            line("Tạm biệt", 0f, 0f, 100f, 30f),
        )

        val result = TextBlockGrouper.deduplicate(lines)

        assertEquals(2, result.size)
    }

    // ------------------------------------------------------------ clampToImage

    @Test
    fun `clampToImage kep toa do vao trong bien anh`() {
        val lines = listOf(line("Hello", -20f, -20f, 900f, 1300f))

        val result = TextBlockGrouper.clampToImage(lines, width = 800, height = 1200)

        assertEquals(1, result.size)
        assertEquals(BoundingBox(0f, 0f, 800f, 1200f), result[0].box)
    }

    @Test
    fun `clampToImage loai bo hop qua nho`() {
        val lines = listOf(line("x", 10f, 10f, 11f, 11f))

        val result = TextBlockGrouper.clampToImage(lines, width = 800, height = 1200)

        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------ mergeAdjacent

    @Test
    fun `mergeAdjacent gop hai khung cung cot va sat nhau`() {
        val blocks = listOf(
            block(line("A", 0f, 0f, 100f, 60f)),
            block(line("B", 0f, 70f, 100f, 120f)),
        )

        val merged = TextBlockGrouper.mergeAdjacent(blocks, maxGap = 20f)

        assertEquals(1, merged.size)
        assertEquals("A B", merged[0].text)
    }

    @Test
    fun `mergeAdjacent khong gop hai khung khac cot`() {
        val blocks = listOf(
            block(line("A", 0f, 0f, 100f, 60f)),
            block(line("B", 500f, 70f, 600f, 120f)),
        )

        val merged = TextBlockGrouper.mergeAdjacent(blocks, maxGap = 20f)

        assertEquals(2, merged.size)
    }

    // ------------------------------------------------------------ textCoverage

    @Test
    fun `textCoverage tinh dung ti le dien tich chu`() {
        val blocks = listOf(block(line("Hello", 0f, 0f, 100f, 100f)))

        val coverage = TextBlockGrouper.textCoverage(blocks, imageWidth = 1000, imageHeight = 1000)

        assertEquals(0.01f, coverage, 0.0001f)
    }

    @Test
    fun `textCoverage tra ve 0 khi kich thuoc anh khong hop le`() {
        val blocks = listOf(block(line("Hello", 0f, 0f, 100f, 100f)))

        assertEquals(0f, TextBlockGrouper.textCoverage(blocks, 0, 0), 0.0001f)
    }

    // ------------------------------------------------------------- BoundingBox

    @Test
    fun `BoundingBox union va iou hoat dong dung`() {
        val first = BoundingBox(0f, 0f, 10f, 10f)
        val second = BoundingBox(20f, 20f, 30f, 30f)

        assertEquals(BoundingBox(0f, 0f, 30f, 30f), first.union(second))
        assertEquals(1f, first.intersectionOverUnion(first), 0.0001f)
        assertEquals(0f, first.intersectionOverUnion(second), 0.0001f)
    }

    @Test
    fun `BoundingBox phat hien giao nhau`() {
        val first = BoundingBox(0f, 0f, 100f, 100f)
        val second = BoundingBox(50f, 50f, 150f, 150f)

        assertTrue(first.intersects(second))
        assertEquals(2500f, first.intersectionArea(second), 0.0001f)
    }
}
