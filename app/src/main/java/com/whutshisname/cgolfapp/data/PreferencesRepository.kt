package com.whutshisname.cgolfapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whutshisname.cgolfapp.ViewMode
import com.whutshisname.cgolfapp.model.WatchSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "golf_prefs")

class PreferencesRepository(private val context: Context) {

    private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
    private val WATCH_SETS = stringPreferencesKey("watch_sets")

    val viewMode: Flow<ViewMode> = context.dataStore.data
        .map { prefs ->
            ViewMode.entries.firstOrNull { it.name == prefs[VIEW_MODE_KEY] } ?: ViewMode.TABLE
        }

    val watchSets: Flow<List<WatchSet>> = context.dataStore.data
        .map { prefs -> parseWatchSets(prefs[WATCH_SETS]) }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { prefs -> prefs[VIEW_MODE_KEY] = mode.name }
    }

    // Saving a set with an existing name replaces it (case-insensitive).
    suspend fun saveWatchSet(name: String, selectionKeys: Set<String>) {
        context.dataStore.edit { prefs ->
            val current = parseWatchSets(prefs[WATCH_SETS]).toMutableList()
            current.removeAll { it.name.equals(name, ignoreCase = true) }
            current.add(WatchSet(name, selectionKeys))
            prefs[WATCH_SETS] = serializeWatchSets(current)
        }
    }

    suspend fun deleteWatchSet(name: String) {
        context.dataStore.edit { prefs ->
            val current = parseWatchSets(prefs[WATCH_SETS]).toMutableList()
            current.removeAll { it.name == name }
            prefs[WATCH_SETS] = serializeWatchSets(current)
        }
    }

    private fun serializeWatchSets(sets: List<WatchSet>): String {
        val arr = JSONArray()
        sets.forEach { set ->
            arr.put(JSONObject().apply {
                put("name", set.name)
                put("keys", JSONArray(set.selectionKeys.toList()))
            })
        }
        return arr.toString()
    }

    private fun parseWatchSets(json: String?): List<WatchSet> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val keysArr = obj.getJSONArray("keys")
                val keys = (0 until keysArr.length()).map { keysArr.getString(it) }.toSet()
                WatchSet(obj.getString("name"), keys)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
