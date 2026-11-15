package com.example.madomireader.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.HistoryEntry
import com.example.madomireader.domain.model.Manga
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Lịch sử đọc + truyện đã lưu + danh sách chương đã đọc,
 * lưu dưới dạng JSON trong DataStore Preferences (gọn, không cần Room).
 */
class HistoryRepository(
    private val context: Context,
    private val gson: Gson,
) {

    private val dataStore = context.madomiDataStore

    private val preferences = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val history: Flow<List<HistoryEntry>> = preferences.map { prefs ->
        decode(prefs[Keys.HISTORY], emptyList())
    }

    val bookmarks: Flow<List<Manga>> = preferences.map { prefs ->
        decode(prefs[Keys.BOOKMARKS], emptyList())
    }

    val readChapterKeys: Flow<Set<String>> = preferences.map { prefs ->
        decode(prefs[Keys.READ_CHAPTERS], emptyList<String>()).toSet()
    }

    /** Ghi lại vị trí đang đọc; mục cũ của cùng bộ truyện sẽ được thay thế. */
    suspend fun recordProgress(
        manga: Manga,
        chapter: Chapter,
        pageIndex: Int,
        pageCount: Int,
    ) {
        val entry = HistoryEntry(
            manga = manga,
            chapter = chapter,
            pageIndex = pageIndex,
            pageCount = pageCount,
            updatedAt = System.currentTimeMillis(),
        )
        val current: List<HistoryEntry> = history.first()
        val updated = buildList {
            add(entry)
            addAll(current.filter { it.manga.key != manga.key })
        }.take(MAX_HISTORY_ITEMS)

        dataStore.edit { prefs -> prefs[Keys.HISTORY] = gson.toJson(updated) }
        markChapterRead(chapter.key)
    }

    suspend fun markChapterRead(chapterKey: String) {
        val current: Set<String> = readChapterKeys.first()
        if (current.contains(chapterKey)) return
        val updated = (current + chapterKey).toList().takeLast(MAX_READ_CHAPTER_KEYS)
        dataStore.edit { prefs -> prefs[Keys.READ_CHAPTERS] = gson.toJson(updated) }
    }

    suspend fun lastReadFor(mangaKey: String): HistoryEntry? =
        history.first().firstOrNull { it.manga.key == mangaKey }

    suspend fun toggleBookmark(manga: Manga) {
        val current: List<Manga> = bookmarks.first()
        val exists = current.any { it.key == manga.key }
        val updated = if (exists) {
            current.filterNot { it.key == manga.key }
        } else {
            listOf(manga) + current.filterNot { it.key == manga.key }
        }
        dataStore.edit { prefs -> prefs[Keys.BOOKMARKS] = gson.toJson(updated) }
    }

    suspend fun isBookmarked(mangaKey: String): Boolean =
        bookmarks.first().any { it.key == mangaKey }

    suspend fun removeHistory(mangaKey: String) {
        val current: List<HistoryEntry> = history.first()
        val updated = current.filterNot { it.manga.key == mangaKey }
        dataStore.edit { prefs -> prefs[Keys.HISTORY] = gson.toJson(updated) }
    }

    suspend fun clearHistory() = dataStore.edit { prefs -> prefs.remove(Keys.HISTORY) }

    suspend fun clearAll() = dataStore.edit { prefs ->
        prefs.remove(Keys.HISTORY)
        prefs.remove(Keys.BOOKMARKS)
        prefs.remove(Keys.READ_CHAPTERS)
    }

    private inline fun <reified T> decode(json: String?, fallback: List<T>): List<T> {
        if (json.isNullOrBlank()) return fallback
        return runCatching {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(json, type) ?: fallback
        }.getOrElse { fallback }
    }

    private object Keys {
        val HISTORY = stringPreferencesKey("reading_history_json")
        val BOOKMARKS = stringPreferencesKey("bookmarks_json")
        val READ_CHAPTERS = stringPreferencesKey("read_chapters_json")
    }

    private companion object {
        const val MAX_HISTORY_ITEMS = 120
        const val MAX_READ_CHAPTER_KEYS = 3000
    }
}
