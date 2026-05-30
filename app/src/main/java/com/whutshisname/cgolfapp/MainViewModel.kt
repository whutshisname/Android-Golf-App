package com.whutshisname.cgolfapp

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whutshisname.cgolfapp.model.ClubType
import com.whutshisname.cgolfapp.model.VariantRow
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

const val SITE_URL = "https://www.callawaygolfpreowned.com/"
const val API_URL =
    "https://www.callawaygolfpreowned.com/on/demandware.store/Sites-CGPO5-Site/default/Product-VariantData"
const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36"

data class FetchedResult(val club: ClubType, val rawJson: String)

data class VariantFilters(
    val clubSet: String? = null,
    val club: String? = null,
    val loft: String? = null,
    val shaftType: String? = null,
    val shaftFlex: String? = null
) {
    val isActive get() = clubSet != null || club != null || loft != null ||
                         shaftType != null || shaftFlex != null
}

data class UiState(
    val sessionReady: Boolean = false,
    val isLoading: Boolean = false,
    val fetchProgress: String = "",
    val clubTypes: List<ClubType> = emptyList(),
    val selectedKeys: Set<String> = emptySet(),
    val fetchedResults: List<FetchedResult> = emptyList(),
    val variantRows: List<VariantRow> = emptyList(),
    val filteredRows: List<VariantRow> = emptyList(),
    val filters: VariantFilters = VariantFilters(),
    val jsonExpanded: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var webView: WebView? = null
    private val fetchMutex = Mutex()
    private var pendingContinuation: CancellableContinuation<String>? = null

    init {
        loadClubTypes()
    }

    fun attachWebView(webView: WebView) {
        this.webView = webView
    }

    fun onSessionReady() {
        _uiState.update { it.copy(sessionReady = true) }
    }

    fun onFetchResult(result: String) {
        val cont = pendingContinuation
        if (cont != null) {
            pendingContinuation = null
            cont.resume(result)
        }
    }

    fun toggleSelection(key: String) {
        _uiState.update { state ->
            val newKeys = if (key in state.selectedKeys) state.selectedKeys - key
                          else state.selectedKeys + key
            state.copy(selectedKeys = newKeys)
        }
    }

    fun selectAllInCategory(cgid: String, selectAll: Boolean) {
        _uiState.update { state ->
            val categoryKeys = state.clubTypes.filter { it.cgid == cgid }
                .map { it.selectionKey }.toSet()
            val newKeys = if (selectAll) state.selectedKeys + categoryKeys
                          else state.selectedKeys - categoryKeys
            state.copy(selectedKeys = newKeys)
        }
    }

    fun fetchSelected() {
        val state = _uiState.value
        val selected = state.selectedKeys.mapNotNull { key ->
            state.clubTypes.find { it.selectionKey == key }
        }
        if (selected.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, fetchedResults = emptyList(), variantRows = emptyList(), filteredRows = emptyList(), filters = VariantFilters()) }

        viewModelScope.launch {
            selected.forEachIndexed { i, club ->
                _uiState.update { it.copy(fetchProgress = "Fetching ${i + 1} of ${selected.size}...") }
                val raw = fetchOneSuspend(club.pid, club.cgid)
                _uiState.update { it.copy(fetchedResults = it.fetchedResults + FetchedResult(club, raw)) }
            }
            val results = _uiState.value.fetchedResults
            val rows = parseVariantRows(results)
            val errorMsg = buildErrorMessage(results, rows, selected.size)
            _uiState.update { it.copy(isLoading = false, fetchProgress = "", variantRows = rows, filteredRows = rows, errorMessage = errorMsg) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun buildErrorMessage(results: List<FetchedResult>, rows: List<VariantRow>, totalRequested: Int): String? {
        val failures = results.count { r ->
            !r.rawJson.startsWith("HTTP 200")
        }
        val firstFailRaw = results.firstOrNull { !it.rawJson.startsWith("HTTP 200") }?.rawJson ?: ""
        return when {
            failures == totalRequested -> when {
                firstFailRaw.startsWith("HTTP 429") ->
                    "Callaway is rate-limiting requests. Please wait a moment and try again."
                firstFailRaw.startsWith("JS Error") || firstFailRaw.startsWith("Error:") ->
                    "Network error. Please check your connection and try again."
                else -> "Failed to retrieve club data. Please try again."
            }
            failures > 0 -> "$failures of $totalRequested clubs failed to load."
            rows.isEmpty() -> "No variant data found for the selected clubs."
            else -> null
        }
    }

    fun setFilter(field: String, value: String?) {
        _uiState.update { state ->
            val f = state.filters
            val newFilters = when (field) {
                "clubSet"   -> f.copy(clubSet = value)
                "club"      -> f.copy(club = value)
                "loft"      -> f.copy(loft = value)
                "shaftType" -> f.copy(shaftType = value)
                "shaftFlex" -> f.copy(shaftFlex = value)
                else -> f
            }
            state.copy(filters = newFilters, filteredRows = applyFilters(state.variantRows, newFilters))
        }
    }

    fun clearFilters() {
        _uiState.update { state ->
            state.copy(filters = VariantFilters(), filteredRows = state.variantRows)
        }
    }

    fun toggleJsonExpanded() {
        _uiState.update { it.copy(jsonExpanded = !it.jsonExpanded) }
    }

    private suspend fun fetchOneSuspend(pid: String, cgid: String): String =
        fetchMutex.withLock {
            suspendCancellableCoroutine { cont ->
                pendingContinuation = cont
                cont.invokeOnCancellation { pendingContinuation = null }
                val url = "$API_URL?pid=$pid&cgid=$cgid&format=json"
                webView?.evaluateJavascript(buildFetchJs(url), null)
                    ?: cont.resume("Error: WebView not ready")
            }
        }

    private fun applyFilters(rows: List<VariantRow>, f: VariantFilters) = rows.filter { row ->
        (f.clubSet   == null || row.clubSet   == f.clubSet) &&
        (f.club      == null || row.club      == f.club) &&
        (f.loft      == null || row.loft      == f.loft) &&
        (f.shaftType == null || row.shaftType == f.shaftType) &&
        (f.shaftFlex == null || row.shaftFlex == f.shaftFlex)
    }

    private fun parseVariantRows(results: List<FetchedResult>): List<VariantRow> {
        val rows = mutableListOf<VariantRow>()
        for (result in results) {
            val body = result.rawJson.substringAfter("\n\n", missingDelimiterValue = "")
                .trim()
            if (body.isEmpty()) continue

            val variants: JSONArray = try {
                if (body.startsWith("[")) JSONArray(body)
                else JSONObject(body).optJSONArray("variants") ?: continue
            } catch (_: Exception) { continue }

            for (i in 0 until variants.length()) {
                val variant = variants.optJSONArray(i) ?: continue
                val cells = buildMap<String, Any?> {
                    for (j in 0 until variant.length()) {
                        val cell = variant.optJSONObject(j) ?: continue
                        put(cell.optString("label"), cell.opt("value"))
                    }
                }

                fun str(label: String) = (cells[label] as? String).orEmpty()

                fun price(label: String): Pair<String, String?> {
                    return when (val v = cells[label]) {
                        is JSONArray -> {
                            val p = v.optString(1).takeIf { it.isNotBlank() && it != "null" } ?: "-"
                            val u = v.optString(3).takeIf { it.isNotBlank() && it != "null" && it.startsWith("http") }
                            Pair(p, u)
                        }
                        is String -> Pair(v, null)
                        else -> Pair("-", null)
                    }
                }

                val (outP, outU)  = price("Outlet")
                val (lnP,  lnU)   = price("Like New")
                val (vgP,  vgU)   = price("Very Good")
                val (gdP,  gdU)   = price("Good")
                val (avP,  avU)   = price("Average")

                rows.add(VariantRow(
                    id             = "${result.club.pid}-$i",
                    productName    = result.club.displayValue,
                    clubSet        = str("Club/Set"),
                    club           = str("Club"),
                    loft           = str("Loft"),
                    shaftType      = str("Shaft Type"),
                    shaftFlex      = str("Shaft Flex"),
                    length         = str("Length"),
                    outletPrice    = outP, outletUrl   = outU,
                    likeNewPrice   = lnP,  likeNewUrl  = lnU,
                    veryGoodPrice  = vgP,  veryGoodUrl = vgU,
                    goodPrice      = gdP,  goodUrl     = gdU,
                    averagePrice   = avP,  averageUrl  = avU
                ))
            }
        }
        return rows
    }

    // TODO: Settings screen — "Refresh Clubs" option
    //   Use evaluateJavascript() + JsBridge to scrape Callaway category pages
    //   (same pattern as variant fetching — Cloudflare blocks all other HTTP clients).
    //   Write scraped results to filesDir/club_types_scraped.json and prefer that
    //   file over the bundled asset on subsequent launches. Load order:
    //   1. filesDir/club_types_scraped.json  (from last successful refresh)
    //   2. assets/club_types.json            (bundled fallback)
    private fun loadClubTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = getApplication<Application>()
                    .assets.open("club_types.json")
                    .bufferedReader()
                    .use { it.readText() }
                val array = JSONArray(json)
                val clubs = (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    ClubType(
                        cgid         = obj.getString("cgid"),
                        displayValue = obj.getString("displayValue"),
                        pid          = obj.getString("pid")
                    )
                }
                _uiState.update { it.copy(clubTypes = clubs) }
            } catch (_: Exception) { }
        }
    }
}

internal fun buildFetchJs(url: String) = """
    fetch('$url', {
        headers: {
            'Accept': 'application/json, text/javascript, */*; q=0.01',
            'X-Requested-With': 'XMLHttpRequest',
            'Referer': '$SITE_URL'
        }
    })
    .then(function(r) {
        return r.text().then(function(t) {
            AndroidBridge.postResult('HTTP ' + r.status + '\n\n' + t);
        });
    })
    .catch(function(e) { AndroidBridge.postResult('JS Error: ' + e); });
""".trimIndent()
