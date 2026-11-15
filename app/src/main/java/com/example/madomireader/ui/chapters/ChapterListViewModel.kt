package com.example.madomireader.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChapterListUiState(
    val manga: Manga? = null,
    val chapters: List<Chapter> = emptyList(),
    val readChapterKeys: Set<String> = emptySet(),
    val isBookmarked: Boolean = false,
    val lastReadChapterId: String? = null,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val ascending: Boolean = false,
    val message: String? = null,
) {
    /** Danh sách chương đã sắp xếp theo lựa chọn của người dùng. */
    val displayChapters: List<Chapter>
        get() = if (ascending) {
            chapters.sortedBy { it.number }
        } else {
            chapters.sortedByDescending { it.number }
        }
}

class ChapterListViewModel(
    private val container: AppContainer,
    private val sourceId: String,
    private val mangaId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterListUiState())
    val state: StateFlow<ChapterListUiState> = _state.asStateFlow()

    private val mangaKey: String = "$sourceId:$mangaId"

    init {
        load()

        viewModelScope.launch {
            container.historyRepository.readChapterKeys.collect { keys ->
                _state.update { it.copy(readChapterKeys = keys) }
            }
        }

        viewModelScope.launch {
            container.historyRepository.bookmarks.collect { saved ->
                _state.update { it.copy(isBookmarked = saved.any { manga -> manga.key == mangaKey }) }
            }
        }

        viewModelScope.launch {
            container.historyRepository.lastReadFor(mangaKey)?.let { entry ->
                _state.update {
                    it.copy(
                        lastReadChapterId = entry.chapter.id,
                        lastReadPageIndex = entry.pageIndex,
                    )
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val detailsResult = container.mangaRepository.details(sourceId, mangaId)
            val manga = detailsResult.getOrNull()?.manga

            val chaptersResult = container.mangaRepository.chapters(sourceId, mangaId)

            _state.update { current ->
                current.copy(
                    isLoading = false,
                    manga = manga ?: current.manga,
                    chapters = chaptersResult.getOrNull() ?: current.chapters,
                    errorMessage = chaptersResult.exceptionOrNull()?.message
                        ?: detailsResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun toggleSort() = _state.update { it.copy(ascending = !it.ascending) }

    fun toggleBookmark() {
        val manga = _state.value.manga ?: return
        viewModelScope.launch {
            container.historyRepository.toggleBookmark(manga)
            val nowSaved = container.historyRepository.isBookmarked(manga.key)
            _state.update {
                it.copy(
                    isBookmarked = nowSaved,
                    message = if (nowSaved) "Đã lưu truyện" else "Đã bỏ lưu truyện",
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    companion object {
        fun factory(
            container: AppContainer,
            sourceId: String,
            mangaId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChapterListViewModel(container, sourceId, mangaId) }
        }
    }
}
