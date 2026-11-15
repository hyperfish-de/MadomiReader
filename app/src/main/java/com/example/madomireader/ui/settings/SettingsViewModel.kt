package com.example.madomireader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.madomireader.data.local.SettingsRepository
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.AppSettings
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.OverlayStyle
import com.example.madomireader.domain.model.ReaderMode
import com.example.madomireader.domain.model.SourceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val apiKeyInput: String = "",
    val apiKeyVisible: Boolean = false,
    val isTesting: Boolean = false,
    val testSucceeded: Boolean = false,
    val testResult: String? = null,
    val message: String? = null,
    val isLoaded: Boolean = false,
)

/**
 * Quản lý màn hình Cài đặt: đọc/ghi [AppSettings] vào DataStore và kiểm tra
 * Gemini API key bằng một câu dịch thử.
 */
class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = settings,
                        // Chỉ nạp key vào ô nhập ở lần đọc đầu tiên, để không
                        // ghi đè khi người dùng đang sửa.
                        apiKeyInput = if (current.isLoaded) current.apiKeyInput else settings.apiKey,
                        isLoaded = true,
                    )
                }
            }
        }
    }

    fun setApiKeyInput(value: String) = _state.update { it.copy(apiKeyInput = value) }

    fun toggleApiKeyVisibility() = _state.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }

    fun saveApiKey() {
        val key = _state.value.apiKeyInput.trim()
        viewModelScope.launch {
            container.settingsRepository.setApiKey(key)
            _state.update {
                it.copy(
                    apiKeyInput = key,
                    message = if (key.isEmpty()) "Đã xoá API key" else "Đã lưu API key",
                )
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            container.settingsRepository.clearApiKey()
            _state.update {
                it.copy(apiKeyInput = "", testResult = null, message = "Đã xoá API key")
            }
        }
    }

    fun testApiKey() {
        val key = _state.value.apiKeyInput.trim()
        if (key.isEmpty()) {
            _state.update {
                it.copy(
                    testSucceeded = false,
                    testResult = "Hãy nhập API key trước khi kiểm tra.",
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, testResult = null) }

            // Lưu key luôn, để nếu kiểm tra thành công thì dùng được ngay.
            container.settingsRepository.setApiKey(key)

            val model = _state.value.settings.model
            val result = container.translationService.testApiKey(key, model)

            _state.update { current ->
                current.copy(
                    isTesting = false,
                    testSucceeded = result.isSuccess,
                    testResult = result.fold(
                        onSuccess = { translated ->
                            if (translated.isBlank()) {
                                "Kết nối thành công (Gemini không trả về nội dung thử)."
                            } else {
                                "Kết nối thành công! Bản dịch thử: $translated"
                            }
                        },
                        onFailure = { error ->
                            "Lỗi: ${error.message ?: error.javaClass.simpleName}"
                        },
                    ),
                )
            }
        }
    }

    fun setModel(model: GeminiModel) = updateSettings { repository ->
        repository.setModel(model)
    }

    fun setSourceLanguage(language: SourceLanguage) = updateSettings { repository ->
        repository.setSourceLanguage(language)
    }

    fun setDefaultReaderMode(mode: ReaderMode) = updateSettings { repository ->
        repository.setDefaultReaderMode(mode)
    }

    fun setOverlayStyle(style: OverlayStyle) = updateSettings { repository ->
        repository.setOverlayStyle(style)
    }

    fun setAutoTranslate(enabled: Boolean) = updateSettings { repository ->
        repository.setAutoTranslate(enabled)
    }

    fun setGridColumns(columns: Int) = updateSettings { repository ->
        repository.setGridColumns(columns)
    }

    fun setFontSizeScale(scale: Float) = updateSettings { repository ->
        repository.setFontSizeScale(scale)
    }

    fun clearReadingData() {
        viewModelScope.launch {
            container.historyRepository.clearAll()
            _state.update { it.copy(message = "Đã xoá lịch sử đọc và danh sách truyện đã lưu") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun consumeTestResult() = _state.update { it.copy(testResult = null) }

    private fun updateSettings(
        block: suspend (SettingsRepository) -> Unit,
    ) {
        val repository = container.settingsRepository
        viewModelScope.launch { block(repository) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
