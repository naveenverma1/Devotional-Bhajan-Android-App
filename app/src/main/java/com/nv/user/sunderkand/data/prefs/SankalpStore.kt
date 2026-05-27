package com.nv.user.sunderkand.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sankalpDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sankalp"
)

/**
 * Tracks the user's *sankalp* — a self-imposed commitment to read a
 * specific chalisa N times. Common Hindu devotional counts: 11, 21,
 * 41, 108.
 *
 * The state is intentionally tiny: a chosen chalisa id, a goal count,
 * how many paths are complete, and when the current goal was started.
 * One sankalp at a time; setting a new one replaces the old.
 */
class SankalpStore(private val appContext: Context) {

    private val store get() = appContext.sankalpDataStore

    val state: Flow<SankalpState> = store.data.map { prefs ->
        SankalpState(
            chalisaId = prefs[KEY_CHALISA_ID],
            goal = prefs[KEY_GOAL] ?: 0,
            done = prefs[KEY_DONE] ?: 0,
            startedAtEpochMs = prefs[KEY_STARTED_AT] ?: 0L,
        )
    }

    suspend fun startSankalp(chalisaId: String, goal: Int) {
        store.edit {
            it[KEY_CHALISA_ID] = chalisaId
            it[KEY_GOAL] = goal.coerceAtLeast(1)
            it[KEY_DONE] = 0
            it[KEY_STARTED_AT] = System.currentTimeMillis()
        }
    }

    /** Mark one more reading complete. Caps at the goal value. */
    suspend fun incrementDone() {
        store.edit {
            val goal = it[KEY_GOAL] ?: 0
            val done = it[KEY_DONE] ?: 0
            if (goal > 0) {
                it[KEY_DONE] = (done + 1).coerceAtMost(goal)
            }
        }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    companion object {
        private val KEY_CHALISA_ID = stringPreferencesKey("chalisa_id")
        private val KEY_GOAL = intPreferencesKey("goal")
        private val KEY_DONE = intPreferencesKey("done")
        private val KEY_STARTED_AT = longPreferencesKey("started_at")
    }
}

data class SankalpState(
    val chalisaId: String? = null,
    val goal: Int = 0,
    val done: Int = 0,
    val startedAtEpochMs: Long = 0L,
) {
    val isActive: Boolean get() = chalisaId != null && goal > 0
    val isComplete: Boolean get() = isActive && done >= goal
    val progress: Float
        get() = if (goal > 0) (done.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
}
