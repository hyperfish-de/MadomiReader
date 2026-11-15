package com.example.madomireader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.PageTranslation
import com.example.madomireader.domain.model.ReaderMode
import com.example.madomireader.domain.model.TranslationStatus
import com.example.madomireader.ui.components.EmptyState
import com.example.madomireader.ui.components.ErrorState
import com.example.madomireader.ui.components.LoadingState
import com.example.madomireader.ui.theme.ReaderBackground
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Màn hình đọc truyện.
 *
 * - [ReaderMode.VERTICAL]  : cuộn dọc liên tục (webtoon / manhwa).
 * - [ReaderMode.HORIZONTAL]: lật từng trang (manga).
 * - Chạm giữa màn hình để ẩn/hiện thanh điều khiển; ở chế độ lật trang, chạm
 *   vào 30% bên trái / bên phải để sang trang.
 */
@Composable
fun ReaderScreen(
    container: AppContainer,
    sourceId: String,
    mangaId: String,
    chapterId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.factory(container, sourceId, mangaId, chapterId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val translations by viewModel.translations.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderBackground),
    ) {
        when {
            state.isLoading -> CenterBox {
                LoadingState(label = "Đang tải chương…")
            }

            state.pages.isEmpty() -> CenterBox {
                if (state.errorMessage != null) {
                    ErrorState(
                        message = state.errorMessage ?: "Đã xảy ra lỗi",
                        onRetry = { viewModel.reload() },
                    )
                } else {
                    EmptyState(
                        title = "Chương này chưa có trang nào",
                        description = "Nguồn truyện chưa trả về ảnh cho chương đang chọn.",
                    )
                }
            }

            else -> ReaderContent(
                state = state,
                translations = translations,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                state = state,
                onBack = onBack,
                onToggleMode = { viewModel.toggleMode() },
                onOpenSettings = onOpenSettings,
            )
        }

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(state = state, viewModel = viewModel)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = SNACKBAR_BOTTOM_PADDING),
        )
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    translations: Map<Int, PageTranslation>,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        ReaderMode.VERTICAL -> VerticalReader(state, translations, viewModel, modifier)
        ReaderMode.HORIZONTAL -> HorizontalReader(state, translations, viewModel, modifier)
    }
}

// ------------------------------------------------------------------ cuộn dọc

@Composable
private fun VerticalReader(
    state: ReaderUiState,
    translations: Map<Int, PageTranslation>,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val startIndex = state.currentPage.coerceIn(0, state.pages.lastIndex.coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> viewModel.onPageVisible(index) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.mode) {
                detectTapGestures(onTap = { viewModel.toggleControls() })
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = READER_END_PADDING),
        ) {
            items(items = state.pages, key = { it.index }) { page ->
                PageItem(
                    page = page,
                    translation = translations[page.index],
                    state = state,
                    onTranslate = { viewModel.translatePage(page.index) },
                    onRetranslate = { viewModel.translatePage(page.index, force = true) },
                    onToggleLayer = { viewModel.toggleTranslationLayer() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "chapter_end") {
                ChapterEndFooter(state = state, viewModel = viewModel)
            }
        }
    }
}

// --------------------------------------------------------------- lật từng trang

@Composable
private fun HorizontalReader(
    state: ReaderUiState,
    translations: Map<Int, PageTranslation>,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val startIndex = state.currentPage.coerceIn(0, state.pages.lastIndex.coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = startIndex) { state.pages.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { index -> viewModel.onPageVisible(index) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.mode, state.pages.size) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    when {
                        offset.x < width * TAP_EDGE_FRACTION && pagerState.currentPage > 0 -> {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }

                        offset.x > width * (1f - TAP_EDGE_FRACTION) &&
                            pagerState.currentPage < state.pages.lastIndex -> {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }

                        else -> viewModel.toggleControls()
                    }
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> state.pages[index].index },
            pageSpacing = 4.dp,
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = READER_PAGE_VERTICAL_PADDING),
                contentAlignment = Alignment.Center,
            ) {
                PageItem(
                    page = state.pages[index],
                    translation = translations[index],
                    state = state,
                    onTranslate = { viewModel.translatePage(index) },
                    onRetranslate = { viewModel.translatePage(index, force = true) },
                    onToggleLayer = { viewModel.toggleTranslationLayer() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------- một trang

@Composable
private fun PageItem(
    page: Page,
    translation: PageTranslation?,
    state: ReaderUiState,
    onTranslate: () -> Unit,
    onRetranslate: () -> Unit,
    onToggleLayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ComicPageView(
            page = page,
            translation = translation,
            showTranslationLayer = state.showTranslationLayer,
            fontSizeScale = state.fontSizeScale,
            modifier = Modifier.fillMaxWidth(),
        )

        TranslateStatusChip(
            status = translation?.status,
            onTranslate = onTranslate,
            onRetranslate = onRetranslate,
            onToggleLayer = onToggleLayer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        )
    }
}

/** Chip nhỏ ở góc phải trên mỗi trang: trạng thái dịch + chạm để dịch lại / ẩn hiện. */
@Composable
private fun TranslateStatusChip(
    status: TranslationStatus?,
    onTranslate: () -> Unit,
    onRetranslate: () -> Unit,
    onToggleLayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (status) {
        TranslationStatus.RUNNING -> "Đang dịch…"
        TranslationStatus.DONE -> "Đã dịch"
        TranslationStatus.ERROR -> "Lỗi - thử lại"
        else -> "Dịch trang"
    }

    val onClick: () -> Unit = when (status) {
        TranslationStatus.RUNNING -> ({})
        TranslationStatus.DONE -> onToggleLayer
        TranslationStatus.ERROR -> onRetranslate
        else -> onTranslate
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = CHIP_BACKGROUND,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status == TranslationStatus.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChapterEndFooter(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Hết chương ${state.chapter?.name.orEmpty()}".trim(),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFB9B4C4),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = { viewModel.goToPreviousChapter() },
                enabled = state.canGoPrevious,
            ) {
                Text(text = "Chương trước")
            }
            TextButton(
                onClick = { viewModel.goToNextChapter() },
                enabled = state.canGoNext,
            ) {
                Text(text = "Chương sau")
            }
        }
    }
}

// -------------------------------------------------------------- thanh điều khiển

@Composable
private fun ReaderTopBar(
    state: ReaderUiState,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = BAR_BACKGROUND) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.chapter?.name ?: "Đang tải…",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.manga?.title.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB9B4C4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (state.mode == ReaderMode.VERTICAL) "Cuộn dọc" else "Lật trang",
                    color = Color(0xFFB39DFF),
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = BAR_BACKGROUND) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trang ${state.currentPage + 1}/${state.pageCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = { pageFraction(state) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = Color(0xFFB39DFF),
                    trackColor = Color(0xFF2A2733),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { viewModel.goToPreviousChapter() },
                    enabled = state.canGoPrevious,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Chương trước",
                        tint = if (state.canGoPrevious) Color.White else DISABLED_TINT,
                    )
                }

                TextButton(onClick = { viewModel.translateCurrentPage() }) {
                    Text(text = "Dịch trang")
                }

                TextButton(
                    onClick = { viewModel.translateAllPages() },
                    enabled = !state.isTranslatingAll,
                ) {
                    Text(text = "Dịch tất cả")
                }

                IconButton(
                    onClick = { viewModel.goToNextChapter() },
                    enabled = state.canGoNext,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Chương sau",
                        tint = if (state.canGoNext) Color.White else DISABLED_TINT,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lớp dịch",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB9B4C4),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.showTranslationLayer,
                    onCheckedChange = { viewModel.toggleTranslationLayer() },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tự động dịch",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB9B4C4),
                )
                Switch(
                    checked = state.autoTranslate,
                    onCheckedChange = { viewModel.setAutoTranslate(it) },
                )
            }

            if (state.isTranslatingAll) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Đang dịch ${state.translatedCount}/${state.translateTotal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { allFraction(state) },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = Color(0xFFB39DFF),
                        trackColor = Color(0xFF2A2733),
                    )
                    TextButton(onClick = { viewModel.cancelTranslateAll() }) {
                        Text(text = "Huỷ")
                    }
                }
            }

            if (!state.hasApiKey) {
                Text(
                    text = "Chưa có Gemini API key — mở Cài đặt (biểu tượng bánh răng) để nhập.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFB74D),
                )
            }
        }
    }
}

private fun pageFraction(state: ReaderUiState): Float {
    if (state.pageCount <= 0) return 0f
    return ((state.currentPage + 1).toFloat() / state.pageCount.toFloat()).coerceIn(0f, 1f)
}

private fun allFraction(state: ReaderUiState): Float {
    if (state.translateTotal <= 0) return 0f
    return (state.translatedCount.toFloat() / state.translateTotal.toFloat()).coerceIn(0f, 1f)
}

private val BAR_BACKGROUND = Color(0xF2101014)
private val CHIP_BACKGROUND = Color(0xCC1B1B22)
private val DISABLED_TINT = Color(0xFF6B6773)
private val READER_END_PADDING = 120.dp
private val READER_PAGE_VERTICAL_PADDING = 8.dp
private val SNACKBAR_BOTTOM_PADDING = 180.dp
private const val TAP_EDGE_FRACTION = 0.3f
