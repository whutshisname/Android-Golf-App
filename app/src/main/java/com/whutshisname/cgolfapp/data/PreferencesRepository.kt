package com.whutshisname.cgolfapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whutshisname.cgolfapp.ViewMode
import com.whutshisname.cgolfapp.model.CatalogOverride
import com.whutshisname.cgolfapp.model.ClubType
import com.whutshisname.cgolfapp.model.WatchSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "golf_prefs")

class PreferencesRepository(private val context: Context) {

    private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
    private val WATCH_SETS = stringPreferencesKey("watch_sets")
    private val CATALOG_OVERRIDE = stringPreferencesKey("catalog_override")

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

    // ── Catalog overrides (admin) ───────────────────────────────────────────────

    val catalogOverride: Flow<CatalogOverride> = context.dataStore.data
        .map { prefs -> parseOverride(prefs[CATALOG_OVERRIDE]) }

    // Adding a pid that already exists replaces it (and un-hides it).
    suspend fun addClub(club: ClubType) {
        context.dataStore.edit { prefs ->
            val current = parseOverride(prefs[CATALOG_OVERRIDE])
            val added = current.addedClubs.filterNot { it.pid == club.pid } + club
            prefs[CATALOG_OVERRIDE] = serializeOverride(
                current.copy(addedClubs = added, hiddenPids = current.hiddenPids - club.pid)
            )
        }
    }

    suspend fun hideClub(pid: String) {
        context.dataStore.edit { prefs ->
            val current = parseOverride(prefs[CATALOG_OVERRIDE])
            prefs[CATALOG_OVERRIDE] = serializeOverride(
                current.copy(hiddenPids = current.hiddenPids + pid)
            )
        }
    }

    suspend fun restoreClub(pid: String) {
        context.dataStore.edit { prefs ->
            val current = parseOverride(prefs[CATALOG_OVERRIDE])
            prefs[CATALOG_OVERRIDE] = serializeOverride(
                current.copy(hiddenPids = current.hiddenPids - pid)
            )
        }
    }

    private fun serializeOverride(override: CatalogOverride): String {
        val added = JSONArray()
        override.addedClubs.forEach { club ->
            added.put(JSONObject().apply {
                put("cgid", club.cgid)
                put("displayValue", club.displayValue)
                put("pid", club.pid)
                put("category", club.category)
            })
        }
        return JSONObject().apply {
            put("added", added)
            put("hidden", JSONArray(override.hiddenPids.toList()))
        }.toString()
    }

    private fun parseOverride(json: String?): CatalogOverride {
        if (json.isNullOrBlank()) return CatalogOverride()
        return try {
            val obj = JSONObject(json)
            val addedArr = obj.optJSONArray("added") ?: JSONArray()
            val added = (0 until addedArr.length()).map { i ->
                val o = addedArr.getJSONObject(i)
                val cgid = o.getString("cgid")
                ClubType(
                    cgid = cgid,
                    displayValue = o.getString("displayValue"),
                    pid = o.getString("pid"),
                    category = o.optString("category").takeIf { it.isNotBlank() } ?: cgid
                )
            }
            val hiddenArr = obj.optJSONArray("hidden") ?: JSONArray()
            val hidden = (0 until hiddenArr.length()).map { hiddenArr.getString(it) }.toSet()
            CatalogOverride(addedClubs = added, hiddenPids = hidden)
        } catch (_: Exception) {
            CatalogOverride()
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
