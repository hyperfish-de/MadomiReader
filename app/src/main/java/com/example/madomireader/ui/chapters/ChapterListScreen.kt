package com.example.madomireader.ui.chapters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madomireader.di.AppContainer
import com.example.madomireader.ui.components.ChapterRow
import com.example.madomireader.ui.components.EmptyState
import com.example.madomireader.ui.components.ErrorState
import com.example.madomireader.ui.components.LoadingState
import com.example.madomireader.ui.components.MangaCoverImage
import com.example.madomireader.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    container: AppContainer,
    sourceId: String,
    mangaId: String,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
) {
    val viewModel: ChapterListViewModel = viewModel(
        factory = ChapterListViewModel.factory(container, sourceId, mangaId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.manga?.title ?: "Danh sách chương",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSort) {
                        Icon(
                            imageVector = if (state.ascending) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = "Đổi thứ tự chương",
                        )
                    }
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(
                            imageVector = if (state.isBookmarked) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = "Lưu truyện",
                            tint = if (state.isBookmarked) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading && state.chapters.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingState(label = "Đang tải danh sách chương…")
                }
            }

            state.errorMessage != null && state.chapters.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(
                        message = state.errorMessage ?: "Đã xảy ra lỗi",
                        onRetry = viewModel::load,
                    )
                }
            }

            state.chapters.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "Chưa có chương nào",
                        description = "Nguồn truyện này chưa trả về chương nào cho bộ truyện đang chọn.",
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "header") {
                        MangaHeader(
                            state = state,
                            onContinue = {
                                val chapterId = state.lastReadChapterId
                                    ?: state.displayChapters.lastOrNull()?.id
                                if (chapterId != null) onOpenChapter(chapterId)
                            },
                        )
                    }

                    item(key = "divider") {
                        HorizontalDivider()
                    }

                    items(items = state.displayChapters, key = { it.key }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            isRead = state.readChapterKeys.contains(chapter.key),
                            isCurrent = state.lastReadChapterId == chapter.id,
                            onClick = { onOpenChapter(chapter.id) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaHeader(
    state: ChapterListUiState,
    onContinue: () -> Unit,
) {
    val manga = state.manga
    val genres = manga?.genres.orEmpty()
    val description = manga?.description.orEmpty()
    val rating = manga?.rating
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MangaCoverImage(
                url = manga?.coverUrl.orEmpty(),
                contentDescription = manga?.title,
                modifier = Modifier
                    .size(width = 108.dp, height = 152.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga?.title ?: "Đang tải…",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (manga != null) {
                    StatusBadge(status = manga.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tác giả: ${manga?.author ?: "Đang cập nhật"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Số chương: ${state.chapters.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rating != null) {
                    Text(
                        text = "Đánh giá: ${"%.1f".format(rating)}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = genres.joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (state.lastReadChapterId != null) "Đọc tiếp chương đang đọc" else "Bắt đầu đọc")
        }
    }
}
