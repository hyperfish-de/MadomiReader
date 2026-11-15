package com.example.madomireader.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.madomireader.domain.model.AppSettings
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.OverlayStyle
import com.example.madomireader.domain.model.ReaderMode
import com.example.madomireader.domain.model.SourceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Một DataStore dùng chung cho cả cài đặt lẫn lịch sử đọc. */
val Context.madomiDataStore: DataStore<Preferences> by preferencesDataStore(name = "madomi_settings")

/**
 * Lưu và đọc cài đặt người dùng (API key, model, ngôn ngữ gốc, chế độ đọc…).
 */
class SettingsRepository(private val context: Context) {

    private val dataStore = context.madomiDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences.toAppSettings() }

    suspend fun snapshot(): AppSettings = settings.first()

    suspend fun setApiKey(value: String) = edit { it[Keys.API_KEY] = value.trim() }

    suspend fun setModel(model: GeminiModel) = edit { it[Keys.MODEL] = model.id }

    suspend fun setSourceLanguage(language: SourceLanguage) = edit { it[Keys.SOURCE_LANGUAGE] = language.id }

    suspend fun setDefaultReaderMode(mode: ReaderMode) = edit { it[Keys.READER_MODE] = mode.id }

    suspend fun setOverlayStyle(style: OverlayStyle) = edit { it[Keys.OVERLAY_STYLE] = style.id }

    suspend fun setAutoTranslate(enabled: Boolean) = edit { it[Keys.AUTO_TRANSLATE] = enabled }

    suspend fun setGridColumns(columns: Int) =
        edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(1, 4) }

    suspend fun setFontSizeScale(scale: Float) =
        edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.6f, 1.8f) }

    suspend fun clearApiKey() = edit { it.remove(Keys.API_KEY) }

    private suspend fun edit(block: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        apiKey = this[Keys.API_KEY].orEmpty(),
        model = GeminiModel.fromId(this[Keys.MODEL]),
        sourceLanguage = SourceLanguage.fromId(this[Keys.SOURCE_LANGUAGE]),
        defaultReaderMode = ReaderMode.fromId(this[Keys.READER_MODE]),
        overlayStyle = OverlayStyle.fromId(this[Keys.OVERLAY_STYLE]),
        autoTranslate = this[Keys.AUTO_TRANSLATE] ?: false,
        gridColumns = this[Keys.GRID_COLUMNS] ?: 2,
        fontSizeScale = this[Keys.FONT_SCALE] ?: 1.0f,
    )

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODEL = stringPreferencesKey("gemini_model")
        val SOURCE_LANGUAGE = stringPreferencesKey("source_language")
        val READER_MODE = stringPreferencesKey("reader_mode")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }
}
