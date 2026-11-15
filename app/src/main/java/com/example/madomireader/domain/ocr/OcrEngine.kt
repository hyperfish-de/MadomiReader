package com.example.madomireader.domain.ocr

import android.graphics.Bitmap
import android.util.Log
import com.example.madomireader.core.util.awaitTask
import com.example.madomireader.domain.model.BoundingBox
import com.example.madomireader.domain.model.OcrBlock
import com.example.madomireader.domain.model.OcrLine
import com.example.madomireader.domain.model.SourceLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bọc ML Kit Text Recognition v2 (bản nhúng model, chạy offline).
 *
 * - Latin dùng cho tiếng Anh / tiếng Việt.
 * - Chinese / Japanese / Korean dùng model chuyên biệt, nhận được cả chữ ngang lẫn chữ dọc.
 * - Chế độ AUTO sẽ chạy song song cả bốn bộ rồi loại bỏ kết quả trùng nhau.
 */
class OcrEngine {

    private val latinRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val chineseRecognizer: TextRecognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    private val japaneseRecognizer: TextRecognizer =
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    private val koreanRecognizer: TextRecognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /**
     * Nhận diện toàn bộ chữ trên [bitmap] và gom thành từng khung thoại.
     */
    suspend fun recognizeBlocks(bitmap: Bitmap, language: SourceLanguage): List<OcrBlock> {
        val lines = recognizeLines(bitmap, language)
        return TextBlockGrouper.group(lines)
    }

    /**
     * Nhận diện và trả về từng dòng chữ kèm hộp bao.
     */
    suspend fun recognizeLines(bitmap: Bitmap, language: SourceLanguage): List<OcrLine> =
        withContext(Dispatchers.Default) {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@withContext emptyList()

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizers = recognizersFor(language)

            val collected = ArrayList<OcrLine>(64)
            for (recognizer in recognizers) {
                val result = runCatching { recognizer.process(image).awaitTask() }
                    .onFailure { error -> Log.w(TAG, "OCR thất bại với ${recognizer.javaClass.simpleName}", error) }
                    .getOrNull() ?: continue

                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val text = line.text.trim()
                        if (text.isEmpty()) continue
                        collected.add(
                            OcrLine(
                                text = text,
                                box = BoundingBox(
                                    left = box.left.toFloat(),
                                    top = box.top.toFloat(),
                                    right = box.right.toFloat(),
                                    bottom = box.bottom.toFloat(),
                                ),
                            ),
                        )
                    }
                }
            }

            val clamped = TextBlockGrouper.clampToImage(collected, bitmap.width, bitmap.height)
            if (recognizers.size == 1) {
                clamped.sortedWith(compareBy({ it.box.top }, { it.box.left }))
            } else {
                TextBlockGrouper.deduplicate(clamped)
            }
        }

    private fun recognizersFor(language: SourceLanguage): List<TextRecognizer> = when (language) {
        SourceLanguage.ENGLISH -> listOf(latinRecognizer)
        SourceLanguage.CHINESE -> listOf(chineseRecognizer, latinRecognizer)
        SourceLanguage.JAPANESE -> listOf(japaneseRecognizer, latinRecognizer)
        SourceLanguage.KOREAN -> listOf(koreanRecognizer, latinRecognizer)
        SourceLanguage.AUTO -> listOf(
            latinRecognizer,
            chineseRecognizer,
            japaneseRecognizer,
            koreanRecognizer,
        )
    }

    fun close() {
        latinRecognizer.close()
        chineseRecognizer.close()
        japaneseRecognizer.close()
        koreanRecognizer.close()
    }

    private companion object {
        const val TAG = "OcrEngine"
    }
}
