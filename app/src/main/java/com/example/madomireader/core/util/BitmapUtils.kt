package com.example.madomireader.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.madomireader.domain.model.BoundingBox
import com.example.madomireader.domain.model.OverlayStyle
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Các tiện ích xử lý bitmap: giải mã có tiết chế bộ nhớ, thu nhỏ ảnh,
 * và lấy màu nền quanh một khung thoại để vẽ lớp dịch.
 */
object BitmapUtils {

    /** Giải mã mảng byte thành bitmap, tự chọn `inSampleSize` để không vượt [maxDimension]. */
    fun decodeScaled(bytes: ByteArray, maxDimension: Int = 1600): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalStateException("Không giải mã được ảnh (dữ liệu không hợp lệ)")

        return if (max(bounds.outWidth, bounds.outHeight) > maxDimension) {
            scaleDown(decoded, maxDimension)
        } else {
            decoded
        }
    }

    /** Thu nhỏ bitmap sao cho cạnh dài nhất bằng [maxDimension]. */
    fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest.toFloat()
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        if (width <= 0 || height <= 0) return sampleSize
        var longest = max(width, height)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Nén bitmap thành JPEG (dùng khi cần gửi ảnh lên dịch vụ khác). */
    fun toJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    /**
     * Chọn màu nền để che chữ gốc của một khung thoại.
     */
    fun resolveBackgroundColor(bitmap: Bitmap, box: BoundingBox, style: OverlayStyle): Int =
        when (style) {
            OverlayStyle.WHITE_BOX -> Color.WHITE
            OverlayStyle.BLACK_BOX -> Color.BLACK
            OverlayStyle.AUTO_SAMPLED -> sampleDominantColor(bitmap, box)
        }

    /** Màu chữ tương phản với màu nền. */
    fun contrastTextColor(backgroundColor: Int): Int =
        if (luminance(backgroundColor) > 150) Color.BLACK else Color.WHITE

    fun luminance(color: Int): Double =
        0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)

    /**
     * Lấy màu phổ biến nhất trong vùng viền quanh khung thoại (cả phía trong lẫn phía ngoài),
     * sau đó làm sạch về trắng/đen nếu đủ gần — cách này ổn định hơn lấy trung bình cộng.
     */
    fun sampleDominantColor(bitmap: Bitmap, box: BoundingBox): Int {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return Color.WHITE

        val shortSide = min(box.width, box.height).coerceAtLeast(4f)
        val inset = (shortSide * 0.10f).coerceAtLeast(2f)
        val outset = (shortSide * 0.08f).coerceAtLeast(2f)
        val step = (shortSide / 8f).coerceAtLeast(1f)

        val buckets = HashMap<Int, IntArray>(16)
        val samples = ArrayList<Int>(96)

        fun take(x: Float, y: Float) {
            val px = x.toInt()
            val py = y.toInt()
            if (px < 0 || py < 0 || px >= bitmap.width || py >= bitmap.height) return
            val color = bitmap.getPixel(px, py)
            samples.add(color)
            val key = ((Color.red(color) shr 4) shl 8) or ((Color.green(color) shr 4) shl 4) or (Color.blue(color) shr 4)
            val slot = buckets[key]
            if (slot == null) {
                buckets[key] = intArrayOf(Color.red(color), Color.green(color), Color.blue(color), 1)
            } else {
                slot[0] += Color.red(color)
                slot[1] += Color.green(color)
                slot[2] += Color.blue(color)
                slot[3] += 1
            }
        }

        // Vòng trong (thường là nền của bong bóng thoại).
        var x = box.left + inset
        while (x <= box.right - inset) {
            take(x, box.top + inset)
            take(x, box.bottom - inset)
            x += step
        }
        var y = box.top + inset
        while (y <= box.bottom - inset) {
            take(box.left + inset, y)
            take(box.right - inset, y)
            y += step
        }

        // Vòng ngoài (nền giấy / nền khung truyện).
        x = box.left - outset
        while (x <= box.right + outset) {
            take(x, box.top - outset)
            take(x, box.bottom + outset)
            x += step
        }
        y = box.top - outset
        while (y <= box.bottom + outset) {
            take(box.left - outset, y)
            take(box.right + outset, y)
            y += step
        }

        if (samples.isEmpty()) return Color.WHITE

        val dominant = buckets.values.maxByOrNull { it[3] } ?: return Color.WHITE
        val count = dominant[3].coerceAtLeast(1)
        val r = dominant[0] / count
        val g = dominant[1] / count
        val b = dominant[2] / count

        val color = Color.rgb(r, g, b)
        val lum = luminance(color)
        return when {
            lum >= 225.0 -> Color.WHITE
            lum <= 30.0 -> Color.BLACK
            else -> color
        }
    }

    /** Cắt một vùng ảnh theo [box] (đã kẹp trong biên bitmap). */
    fun crop(bitmap: Bitmap, box: BoundingBox): Bitmap {
        val left = box.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = box.top.toInt().coerceIn(0, bitmap.height - 1)
        val right = box.right.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    /** Vẽ một bitmap đã cắt lên canvas (dùng cho ảnh chèn trong lớp dịch). */
    fun drawCropped(canvas: Canvas, bitmap: Bitmap, box: BoundingBox, paint: Paint) {
        val destination = Rect(
            box.left.toInt(),
            box.top.toInt(),
            box.right.toInt(),
            box.bottom.toInt(),
        )
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), destination, paint)
    }
}
