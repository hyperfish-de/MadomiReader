package com.example.madomireader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.HistoryEntry
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaStatus
import com.example.madomireader.ui.theme.ChipCancelled
import com.example.madomireader.ui.theme.ChipCompleted
import com.example.madomireader.ui.theme.ChipOngoing
import com.example.madomireader.ui.theme.ChipPaused

/** Ảnh bìa truyện với trạng thái đang tải / lỗi. */
@Composable
fun MangaCoverImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** Nhãn trạng thái phát hành của truyện. */
@Composable
fun StatusBadge(status: MangaStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.92f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

fun statusColor(status: MangaStatus): Color = when (status) {
    MangaStatus.ONGOING -> ChipOngoing
    MangaStatus.COMPLETED -> ChipCompleted
    MangaStatus.PAUSED -> ChipPaused
    MangaStatus.CANCELLED -> ChipCancelled
    MangaStatus.UNKNOWN -> Color(0xFFB0BEC5)
}

/** Thẻ truyện trong lưới danh mục. */
@Composable
fun MangaGridCard(
    manga: Manga,
    sourceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            ) {
                MangaCoverImage(
                    url = manga.coverUrl,
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize(),
                )
                StatusBadge(
                    status = manga.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = manga.author ?: sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Một dòng trong danh sách lịch sử đọc. */
@Composable
fun HistoryRow(
    entry: HistoryEntry,
    sourceName: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCoverImage(
                url = entry.manga.coverUrl,
                contentDescription = entry.manga.title,
                modifier = Modifier
                    .size(width = 56.dp, height = 78.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.manga.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.chapter.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(sourceName)
                        append(" • Trang ")
                        append(entry.pageIndex + 1)
                        if (entry.pageCount > 0) {
                            append('/')
                            append(entry.pageCount)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Xoá khỏi lịch sử",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Một dòng trong danh sách chương. */
@Composable
fun ChapterRow(
    chapter: Chapter,
    isRead: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = when {
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (!chapter.releasedAt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapter.releasedAt.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isRead) {
                Text(
                    text = "Đã đọc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            if (chapter.pageCount > 0) {
                Text(
                    text = "${chapter.pageCount} trang",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Hàng chip lọc (nguồn / thể loại). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipRow(
    options: List<Pair<String, String>>,
    selectedId: String?,
    allLabel: String,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text(allLabel) },
            )
        }
        items(items = options, key = { it.first }) { option ->
            FilterChip(
                selected = selectedId == option.first,
                onClick = { onSelect(if (selectedId == option.first) null else option.first) },
                label = { Text(option.second) },
            )
        }
    }
}

/** Ô tìm kiếm có nút xoá và nút tìm trên bàn phím. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tìm truyện…",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Xoá từ khoá")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "Đang tải…") {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Thử lại")
        }
    }
}
