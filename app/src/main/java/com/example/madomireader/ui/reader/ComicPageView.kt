package com.example.madomireader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.PageTranslation

/**
 * Một trang truyện hoàn chỉnh: ảnh gốc + (tuỳ chọn) lớp dịch phủ lên trên.
 *
 * Ảnh được tải bằng Coil (cache bộ nhớ + đĩa), có trạng thái đang tải / lỗi + nút thử lại.
 * Header riêng của nguồn (Referer, Cookie…) được gửi kèm qua [Page.headers].
 */
@Composable
fun ComicPageView(
    page: Page,
    translation: PageTranslation?,
    showTranslationLayer: Boolean,
    fontSizeScale: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        RemotePageImage(
            page = page,
            modifier = Modifier.fillMaxWidth(),
        )

        if (showTranslationLayer &&
            translation != null &&
            translation.isDone &&
            translation.blocks.isNotEmpty()
        ) {
            TranslationOverlay(
                translation = translation,
                fontSizeScale = fontSizeScale,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Trang tải từ mạng bằng Coil (có cache đĩa + bộ nhớ của ứng dụng). */
@Composable
private fun RemotePageImage(
    page: Page,
    modifier: Modifier = Modifier,
) {
    var retryKey by remember(page) { mutableIntStateOf(0) }
    val context = LocalContext.current

    val request = remember(page, retryKey) {
        ImageRequest.Builder(context)
            .data(page.imageUrl)
            .apply { page.headers.forEach { (name, value) -> addHeader(name, value) } }
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier.heightIn(min = MIN_PAGE_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        key(retryKey) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = "Trang ${page.index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                loading = { PageMessage(label = "Đang tải trang…", showSpinner = true) },
                error = {
                    PageMessage(
                        label = "Không tải được trang này.",
                        showSpinner = false,
                        onRetry = { retryKey++ },
                    )
                },
            )
        }
    }
}

/** Khối báo trạng thái cho một trang chưa hiển thị được. */
@Composable
private fun PageMessage(
    label: String,
    showSpinner: Boolean,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MIN_PAGE_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showSpinner) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onRetry) { Text(text = "Thử lại") }
            }
        }
    }
}

private val MIN_PAGE_HEIGHT = 320.dp
