package com.example.madomireader.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.ui.components.ChipRow
import com.example.madomireader.ui.components.EmptyState
import com.example.madomireader.ui.components.ErrorState
import com.example.madomireader.ui.components.HistoryRow
import com.example.madomireader.ui.components.LoadingState
import com.example.madomireader.ui.components.MangaGridCard
import com.example.madomireader.ui.components.SearchField
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenManga: (sourceId: String, mangaId: String) -> Unit,
    onOpenReader: (sourceId: String, mangaId: String, chapterId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Madomi Reader") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (state.tab == HomeTab.DISCOVER) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    onSearch = { viewModel.refresh() },
                    placeholder = "Tìm truyện theo tên, tác giả…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ChipRow(
                    options = state.sources.map { it.id to it.name },
                    selectedId = state.sourceId,
                    allLabel = "Tất cả nguồn",
                    onSelect = viewModel::setSource,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipRow(
                    options = state.genres.map { it to it },
                    selectedId = state.genre,
                    allLabel = "Mọi thể loại",
                    onSelect = viewModel::setGenre,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                HomeTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(text = tab.label) },
                    )
                }
            }

            when (state.tab) {
                HomeTab.DISCOVER -> DiscoverContent(
                    state = state,
                    viewModel = viewModel,
                    onOpenManga = onOpenManga,
                )

                HomeTab.HISTORY -> HistoryContent(
                    state = state,
                    viewModel = viewModel,
                    onOpenReader = onOpenReader,
                )

                HomeTab.SAVED -> SavedContent(
                    state = state,
                    onOpenManga = onOpenManga,
                )
            }
        }
    }
}

@Composable
private fun DiscoverContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOpenManga: (String, String) -> Unit,
) {
    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(label = "Đang tải danh mục truyện…")
            }
        }

        state.errorMessage != null && state.mangas.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorState(
                    message = state.errorMessage ?: "Đã xảy ra lỗi",
                    onRetry = viewModel::refresh,
                )
            }
        }

        state.mangas.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Không tìm thấy truyện nào",
                    description = "Thử đổi từ khoá hoặc bỏ bộ lọc thể loại.",
                )
            }
        }

        else -> {
            val gridState = rememberLazyGridState()

            LaunchedEffect(gridState) {
                snapshotFlow {
                    val info = gridState.layoutInfo
                    val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    lastIndex to info.totalItemsCount
                }
                    .distinctUntilChanged()
                    .collect { (lastIndex, totalCount) ->
                        if (totalCount > 0 && lastIndex >= totalCount - 3) {
                            viewModel.loadMore()
                        }
                    }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(state.gridColumns.coerceIn(1, 4)),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = state.mangas, key = { it.key }) { manga ->
                    MangaGridCard(
                        manga = manga,
                        sourceName = state.sourceNames[manga.sourceId] ?: manga.sourceId,
                        onClick = { onOpenManga(manga.sourceId, manga.id) },
                    )
                }

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOpenReader: (String, String, String) -> Unit,
) {
    if (state.history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Chưa có lịch sử đọc",
                description = "Mở một chương truyện, vị trí đang đọc sẽ được lưu tại đây.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${state.history.size} truyện đang đọc dở",
                    style = MaterialTheme.typography.titleSmall,
                )
                TextButton(onClick = viewModel::clearHistory) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "Xoá tất cả")
                }
            }
        }

        items(items = state.history, key = { it.manga.key }) { entry ->
            HistoryRow(
                entry = entry,
                sourceName = state.sourceNames[entry.manga.sourceId] ?: entry.manga.sourceId,
                onOpen = {
                    onOpenReader(entry.manga.sourceId, entry.manga.id, entry.chapter.id)
                },
                onRemove = { viewModel.removeHistory(entry.manga.key) },
            )
        }
    }
}

@Composable
private fun SavedContent(
    state: HomeUiState,
    onOpenManga: (String, String) -> Unit,
) {
    if (state.saved.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Chưa lưu truyện nào",
                description = "Nhấn biểu tượng trái tim ở trang danh sách chương để lưu truyện.",
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(state.gridColumns.coerceIn(1, 4)),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = state.saved, key = { it.key }) { manga: Manga ->
            MangaGridCard(
                manga = manga,
                sourceName = state.sourceNames[manga.sourceId] ?: manga.sourceId,
                onClick = { onOpenManga(manga.sourceId, manga.id) },
            )
        }
    }
}
