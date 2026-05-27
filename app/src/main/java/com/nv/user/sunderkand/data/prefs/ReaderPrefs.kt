package com.nv.user.sunderkand.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_prefs"
)

/**
 * User-controllable reader settings + per-chalisa reading position,
 * backed by DataStore Preferences. Exposed as cold Flows so the
 * Compose screens can collect with `collectAsState`.
 */
class ReaderPrefs(private val appContext: Context) {

    private val store get() = appContext.readerDataStore

    /** Body text scale factor (1.0 == design default of 18sp). */
    val fontScale: Flow<Float> = store.data.map { prefs ->
        prefs[KEY_FONT_SCALE] ?: DEFAULT_FONT_SCALE
    }

    suspend fun setFontScale(value: Float) {
        val clamped = value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        store.edit { it[KEY_FONT_SCALE] = clamped }
    }

    /** Theme override: "system" / "light" / "sepia" / "dark". */
    val themePref: Flow<String> = store.data.map { prefs ->
        prefs[KEY_THEME] ?: DEFAULT_THEME
    }

    suspend fun setThemePref(value: String) {
        store.edit { it[KEY_THEME] = value }
    }

    /** Last-read verse index for the given chalisa (0-based, flattened). */
    fun lastReadVerse(chalisaId: String): Flow<Int> = store.data.map { prefs ->
        prefs[keyLastVerse(chalisaId)] ?: 0
    }

    suspend fun setLastReadVerse(chalisaId: String, index: Int) {
        store.edit { it[keyLastVerse(chalisaId)] = index.coerceAtLeast(0) }
    }

    /** Saved scroll offset (px) for the given chalisa reader screen. */
    fun scrollOffset(chalisaId: String): Flow<Int> = store.data.map { prefs ->
        prefs[keyScrollOffset(chalisaId)] ?: 0
    }

    suspend fun setScrollOffset(chalisaId: String, offsetPx: Int) {
        store.edit { it[keyScrollOffset(chalisaId)] = offsetPx.coerceAtLeast(0) }
    }

    /**
     * The chalisa id most recently opened in the reader, surfaced on
     * the home screen as a "Resume reading" card. Null when the user
     * has never opened anything (clean-install state).
     */
    val lastOpenedChalisa: Flow<String?> = store.data.map { prefs ->
        prefs[KEY_LAST_OPENED_CHALISA]
    }

    suspend fun setLastOpenedChalisa(chalisaId: String) {
        store.edit { it[KEY_LAST_OPENED_CHALISA] = chalisaId }
    }

    companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 2.0f
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_THEME = "system"

        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        private val KEY_THEME = stringPreferencesKey("theme_pref")
        private val KEY_LAST_OPENED_CHALISA = stringPreferencesKey("last_opened_chalisa")
        private fun keyLastVerse(id: String) = intPreferencesKey("last_verse_$id")
        private fun keyScrollOffset(id: String) = intPreferencesKey("scroll_offset_$id")
    }
}
