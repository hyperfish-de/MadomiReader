package com.example.madomireader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.madomireader.core.util.BitmapUtils
import com.example.madomireader.data.remote.gemini.MissingApiKeyException
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.AppSettings
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.OverlayStyle
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.PageTranslation
import com.example.madomireader.domain.model.ReaderMode
import com.example.madomireader.domain.model.SourceLanguage
import com.example.madomireader.domain.model.TranslatedBlock
import com.example.madomireader.domain.model.TranslationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val manga: Manga? = null,
    val chapter: Chapter? = null,
    val chapters: List<Chapter> = emptyList(),
    val pages: List<Page> = emptyList(),
    val currentPage: Int = 0,
    val startPage: Int = 0,
    val mode: ReaderMode = ReaderMode.VERTICAL,
    val overlayStyle: OverlayStyle = OverlayStyle.AUTO_SAMPLED,
    val sourceLanguage: SourceLanguage = SourceLanguage.AUTO,
    val fontSizeScale: Float = 1.0f,
    val autoTranslate: Boolean = false,
    val showTranslationLayer: Boolean = true,
    val controlsVisible: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
    val hasApiKey: Boolean = false,
    val isTranslatingAll: Boolean = false,
    val translatedCount: Int = 0,
    val translateTotal: Int = 0,
    val previousChapterId: String? = null,
    val nextChapterId: String? = null,
) {
    val pageCount: Int get() = pages.size
    val canGoPrevious: Boolean get() = previousChapterId != null
    val canGoNext: Boolean get() = nextChapterId != null
}

class ReaderViewModel(
    private val container: AppContainer,
    private val sourceId: String,
    private val mangaId: String,
    initialChapterId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _translations = MutableStateFlow<Map<Int, PageTranslation>>(emptyMap())
    val translations: StateFlow<Map<Int, PageTranslation>> = _translations.asStateFlow()

    private val mangaKey: String = "$sourceId:$mangaId"

    private var currentChapterId: String = initialChapterId
    private var settings: AppSettings = AppSettings()
    private var progressJob: Job? = null
    private var translateAllJob: Job? = null
    private val inFlightPages = mutableSetOf<Int>()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { updated ->
                settings = updated
                _state.update { current ->
                    current.copy(
                        overlayStyle = updated.overlayStyle,
                        sourceLanguage = updated.sourceLanguage,
                        fontSizeScale = updated.fontSizeScale,
                        autoTranslate = updated.autoTranslate,
                        hasApiKey = updated.hasApiKey,
                    )
                }
            }
        }

        viewModelScope.launch {
            settings = container.settingsRepository.snapshot()
            _state.update { it.copy(mode = settings.defaultReaderMode) }
            loadChapter(currentChapterId)
        }
    }

    // ---------------------------------------------------------------- điều hướng

    fun toggleControls() = _state.update { it.copy(controlsVisible = !it.controlsVisible) }

    fun setControlsVisible(visible: Boolean) = _state.update { it.copy(controlsVisible = visible) }

    fun setMode(mode: ReaderMode) = _state.update { it.copy(mode = mode) }

    fun toggleMode() = _state.update {
        it.copy(mode = if (it.mode == ReaderMode.VERTICAL) ReaderMode.HORIZONTAL else ReaderMode.VERTICAL)
    }

    fun toggleTranslationLayer() =
        _state.update { it.copy(showTranslationLayer = !it.showTranslationLayer) }

    fun setAutoTranslate(enabled: Boolean) {
        _state.update { it.copy(autoTranslate = enabled) }
        viewModelScope.launch { container.settingsRepository.setAutoTranslate(enabled) }
    }

    fun openChapter(chapterId: String) {
        if (chapterId == currentChapterId) return
        cancelTranslateAll()
        viewModelScope.launch { loadChapter(chapterId) }
    }

    /** Tải lại chương hiện tại (dùng khi lần tải trước thất bại). */
    fun reload() {
        viewModelScope.launch { loadChapter(currentChapterId) }
    }

    fun goToPreviousChapter() {
        val id = _state.value.previousChapterId ?: return
        openChapter(id)
    }

    fun goToNextChapter() {
        val id = _state.value.nextChapterId ?: return
        openChapter(id)
    }

    fun onPageVisible(index: Int) {
        val snapshot = _state.value
        if (snapshot.pages.isEmpty()) return
        val clamped = index.coerceIn(0, snapshot.pages.lastIndex)

        if (clamped != snapshot.currentPage) {
            _state.update { it.copy(currentPage = clamped) }
            scheduleProgressSave(clamped)
        }

        if (snapshot.autoTranslate && snapshot.hasApiKey) {
            translatePage(clamped)
        }
    }

    // ------------------------------------------------------------------- dịch

    fun translatePage(index: Int, force: Boolean = false) {
        viewModelScope.launch { translatePageAwait(index, force) }
    }

    fun translateCurrentPage(force: Boolean = false) {
        translatePage(_state.value.currentPage, force)
    }

    fun translateAllPages() {
        if (_state.value.isTranslatingAll || _state.value.pages.isEmpty()) return
        if (!_state.value.hasApiKey) {
            _state.update { it.copy(message = MISSING_KEY_MESSAGE) }
            return
        }

        translateAllJob = viewModelScope.launch {
            val total = _state.value.pages.size
            _state.update { it.copy(isTranslatingAll = true, translatedCount = 0, translateTotal = total) }

            for (index in 0 until total) {
                translatePageAwait(index)
                _state.update { it.copy(translatedCount = index + 1) }
            }

            _state.update { it.copy(isTranslatingAll = false) }
        }
    }

    fun cancelTranslateAll() {
        translateAllJob?.cancel()
        translateAllJob = null
        _state.update { it.copy(isTranslatingAll = false) }
    }

    fun clearTranslations() {
        _translations.value = emptyMap()
        inFlightPages.clear()
    }

    fun clearPageTranslation(index: Int) {
        _translations.update { it - index }
    }

    private suspend fun translatePageAwait(index: Int, force: Boolean = false) {
        val page = _state.value.pages.getOrNull(index) ?: return
        val existing = _translations.value[index]
        if (!force && (existing?.isDone == true || existing?.isRunning == true)) return
        if (!inFlightPages.add(index)) return

        _translations.update { it + (index to PageTranslation(status = TranslationStatus.RUNNING)) }

        val result = runCatching { translatePageInternal(page) }

        _translations.update { current ->
            current + (
                index to result.getOrElse { error ->
                    PageTranslation(
                        status = TranslationStatus.ERROR,
                        errorMessage = error.message ?: "Dịch thất bại",
                    )
                }
                )
        }

        inFlightPages.remove(index)

        result.exceptionOrNull()?.let { error ->
            _state.update { it.copy(message = error.message ?: "Dịch thất bại") }
        }
    }

    private suspend fun translatePageInternal(page: Page): PageTranslation {
        val current = container.settingsRepository.snapshot()
        if (!current.hasApiKey) throw MissingApiKeyException()

        val bitmap = container.pageBitmapLoader.load(page)
        val blocks = container.ocrEngine.recognizeBlocks(bitmap, current.sourceLanguage)

        if (blocks.isEmpty()) {
            return PageTranslation(
                status = TranslationStatus.DONE,
                blocks = emptyList(),
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
            )
        }

        val translated = container.translationService
            .translateBlocks(blocks = blocks, storyContext = buildStoryContext(), settings = current)
            .getOrThrow()

        val translatedBlocks = blocks.mapIndexed { index, block ->
            val background = BitmapUtils.resolveBackgroundColor(bitmap, block.box, current.overlayStyle)
            TranslatedBlock(
                box = block.box,
                sourceText = block.text,
                translatedText = translated.getOrElse(index) { block.text },
                backgroundColor = background,
                textColor = BitmapUtils.contrastTextColor(background),
            )
        }

        return PageTranslation(
            status = TranslationStatus.DONE,
            blocks = translatedBlocks,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
        )
    }

    private fun buildStoryContext(): String {
        val manga = _state.value.manga
        val chapter = _state.value.chapter
        val genres = manga?.genres.orEmpty()
        return buildString {
            append("Truyện: ")
            append(manga?.title ?: "không rõ")
            append(". Tác giả: ")
            append(manga?.author ?: "không rõ")
            if (genres.isNotEmpty()) {
                append(". Thể loại: ")
                append(genres.joinToString(", "))
            }
            if (chapter != null) {
                append(". Chương đang đọc: ")
                append(chapter.name)
            }
            append(". Hãy dịch thoại của các nhân vật trong trang truyện này.")
        }
    }

    // ----------------------------------------------------------------- tải dữ liệu

    private suspend fun loadChapter(chapterId: String) {
        currentChapterId = chapterId
        cancelTranslateAll()
        _translations.value = emptyMap()
        inFlightPages.clear()

        _state.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                pages = emptyList(),
                currentPage = 0,
                startPage = 0,
                controlsVisible = true,
            )
        }

        val details = container.mangaRepository.details(sourceId, mangaId).getOrNull()
        val chapters = container.mangaRepository.chapters(sourceId, mangaId)
            .getOrNull()
            .orEmpty()
            .sortedBy { it.number }

        val pagesResult = container.mangaRepository.pages(sourceId, chapterId)

        val chapter = chapters.firstOrNull { it.id == chapterId }
            ?: details?.chapters?.firstOrNull { it.id == chapterId }

        val index = chapters.indexOfFirst { it.id == chapterId }
        val history = container.historyRepository.lastReadFor(mangaKey)
        val startPage = if (history?.chapter?.id == chapterId) {
            history.pageIndex.coerceAtLeast(0)
        } else {
            0
        }

        val pages = pagesResult.getOrNull().orEmpty()

        _state.update { current ->
            current.copy(
                manga = details?.manga ?: current.manga,
                chapter = chapter,
                chapters = chapters,
                pages = pages,
                startPage = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
                currentPage = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
                isLoading = false,
                errorMessage = pagesResult.exceptionOrNull()?.message,
                previousChapterId = if (index > 0) chapters[index - 1].id else null,
                nextChapterId = if (index in 0 until chapters.size - 1) chapters[index + 1].id else null,
            )
        }

        if (pages.isNotEmpty() && _state.value.autoTranslate && _state.value.hasApiKey) {
            translatePage(_state.value.currentPage)
        }
    }

    private fun scheduleProgressSave(index: Int) {
        val manga = _state.value.manga ?: return
        val chapter = _state.value.chapter ?: return
        val pageCount = _state.value.pages.size

        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(500L)
            container.historyRepository.recordProgress(
                manga = manga,
                chapter = chapter,
                pageIndex = index,
                pageCount = pageCount,
            )
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        super.onCleared()
        translateAllJob?.cancel()
        progressJob?.cancel()
    }

    companion object {
        const val MISSING_KEY_MESSAGE =
            "Chưa có Gemini API Key. Mở Cài đặt để nhập key trước khi dịch."

        fun factory(
            container: AppContainer,
            sourceId: String,
            mangaId: String,
            chapterId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReaderViewModel(container, sourceId, mangaId, chapterId) }
        }
    }
}
