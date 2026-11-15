package com.example.madomireader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.HistoryEntry
import com.example.madomireader.domain.model.Manga
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab(val label: String) {
    DISCOVER("Khám phá"),
    HISTORY("Lịch sử"),
    SAVED("Đã lưu"),
}

data class SourceOption(val id: String, val name: String)

data class HomeUiState(
    val tab: HomeTab = HomeTab.DISCOVER,
    val query: String = "",
    val sourceId: String? = null,
    val genre: String? = null,
    val sources: List<SourceOption> = emptyList(),
    val sourceNames: Map<String, String> = emptyMap(),
    val sourcesReady: Boolean = false,
    val genres: List<String> = emptyList(),
    val mangas: List<Manga> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val saved: List<Manga> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val gridColumns: Int = 2,
    val hasApiKey: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val sourceFlow = MutableStateFlow<String?>(null)
    private val genreFlow = MutableStateFlow<String?>(null)

    private var currentPage = 0

    init {
        // Danh sách nguồn thay đổi lúc chạy (cài/gỡ extension), nên UI quan sát nó.
        viewModelScope.launch {
            container.sourceRegistry.sources.collect { sources ->
                val options = sources.map { SourceOption(it.id, it.name) }
                val selected = sourceFlow.value
                val keepSelection = selected != null && options.any { it.id == selected }

                _state.update { current ->
                    current.copy(
                        sources = options,
                        sourceNames = options.associate { it.id to it.name },
                        sourceId = if (keepSelection) selected else null,
                        genres = container.mangaRepository.genres(),
                    )
                }

                if (selected != null && !keepSelection) sourceFlow.value = null
            }
        }

        viewModelScope.launch {
            container.sourceRegistry.initialized.collect { ready ->
                _state.update { it.copy(sourcesReady = ready) }
            }
        }

        // Bất kỳ thay đổi nào (từ khoá, nguồn, thể loại, danh sách nguồn) đều tải lại trang đầu.
        viewModelScope.launch {
            combine(
                queryFlow.debounce(300L).distinctUntilChanged(),
                sourceFlow,
                genreFlow,
                container.sourceRegistry.sources,
            ) { query, sourceId, genre, sources ->
                BrowseKey(
                    query = query,
                    sourceId = sourceId,
                    genre = genre,
                    sourceIds = sources.map { it.id },
                )
            }
                .distinctUntilChanged()
                .collectLatest { key ->
                    loadFirstPage(key.query, key.sourceId, key.genre)
                }
        }

        viewModelScope.launch {
            container.historyRepository.history.collect { entries ->
                _state.update { it.copy(history = entries) }
            }
        }

        viewModelScope.launch {
            container.historyRepository.bookmarks.collect { saved ->
                _state.update { it.copy(saved = saved) }
            }
        }

        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _state.update { it.copy(gridColumns = settings.gridColumns, hasApiKey = settings.hasApiKey) }
            }
        }
    }

    fun setTab(tab: HomeTab) = _state.update { it.copy(tab = tab) }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun setSource(sourceId: String?) {
        _state.update { it.copy(sourceId = sourceId) }
        sourceFlow.value = sourceId
    }

    fun setGenre(genre: String?) {
        _state.update { it.copy(genre = genre) }
        genreFlow.value = genre
    }

    fun refresh() {
        viewModelScope.launch {
            loadFirstPage(queryFlow.value, sourceFlow.value, genreFlow.value)
        }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.canLoadMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val result = fetchPage(
                page = currentPage + 1,
                query = queryFlow.value,
                sourceId = sourceFlow.value,
                genre = genreFlow.value,
            )
            currentPage += 1
            _state.update { current ->
                current.copy(
                    isLoadingMore = false,
                    mangas = (current.mangas + result.mangas).distinctBy { it.key },
                    canLoadMore = result.hasMore,
                    errorMessage = if (result.mangas.isEmpty()) result.errorMessage else current.errorMessage,
                )
            }
        }
    }

    fun removeHistory(mangaKey: String) {
        viewModelScope.launch { container.historyRepository.removeHistory(mangaKey) }
    }

    fun clearHistory() {
        viewModelScope.launch { container.historyRepository.clearHistory() }
    }

    fun toggleBookmark(manga: Manga) {
        viewModelScope.launch { container.historyRepository.toggleBookmark(manga) }
    }

    private suspend fun loadFirstPage(query: String, sourceId: String?, genre: String?) {
        currentPage = 0
        _state.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                canLoadMore = true,
            )
        }
        val result = fetchPage(page = 0, query = query, sourceId = sourceId, genre = genre)
        _state.update {
            it.copy(
                isLoading = false,
                mangas = result.mangas,
                errorMessage = result.errorMessage,
                canLoadMore = result.hasMore,
            )
        }
    }

    private suspend fun fetchPage(
        page: Int,
        query: String,
        sourceId: String?,
        genre: String?,
    ): FetchResult {
        val targetIds = if (sourceId == null) {
            container.mangaRepository.sources().map { it.id }
        } else {
            listOf(sourceId)
        }

        if (targetIds.isEmpty()) {
            return FetchResult(mangas = emptyList(), hasMore = false, errorMessage = null)
        }

        val collected = mutableListOf<Manga>()
        var hasMore = false
        var error: String? = null

        for (id in targetIds) {
            container.mangaRepository.browse(sourceId = id, page = page, query = query, genre = genre)
                .onSuccess { result ->
                    collected += result.mangas
                    if (result.hasNextPage) hasMore = true
                }
                .onFailure { throwable ->
                    error = throwable.message ?: "Không tải được dữ liệu"
                }
        }

        return FetchResult(
            mangas = collected.distinctBy { it.key },
            hasMore = hasMore,
            errorMessage = if (collected.isEmpty()) error else null,
        )
    }

    /** Khoá gộp mọi thứ ảnh hưởng tới danh sách truyện đang hiển thị. */
    private data class BrowseKey(
        val query: String,
        val sourceId: String?,
        val genre: String?,
        val sourceIds: List<String>,
    )

    private data class FetchResult(
        val mangas: List<Manga>,
        val hasMore: Boolean,
        val errorMessage: String?,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container) }
        }
    }
}
