package com.whutshisname.cgolfapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whutshisname.cgolfapp.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "golf_prefs")

class PreferencesRepository(private val context: Context) {

    private val FAVORITE_PIDS = stringSetPreferencesKey("favorite_pids")
    private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")

    val favoritePids: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[FAVORITE_PIDS] ?: emptySet() }

    val viewMode: Flow<ViewMode> = context.dataStore.data
        .map { prefs ->
            ViewMode.entries.firstOrNull { it.name == prefs[VIEW_MODE_KEY] } ?: ViewMode.TABLE
        }

    suspend fun toggleFavorite(pid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_PIDS] ?: emptySet()
            prefs[FAVORITE_PIDS] = if (pid in current) current - pid else current + pid
        }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { prefs -> prefs[VIEW_MODE_KEY] = mode.name }
    }
}
