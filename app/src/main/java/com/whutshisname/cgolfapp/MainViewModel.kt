package com.whutshisname.cgolfapp

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whutshisname.cgolfapp.model.ClubType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import kotlin.coroutines.resume

const val SITE_URL = "https://www.callawaygolfpreowned.com/"
const val API_URL =
    "https://www.callawaygolfpreowned.com/on/demandware.store/Sites-CGPO5-Site/default/Product-VariantData"
const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36"

data class UiState(
    val sessionReady: Boolean = false,
    val isLoading: Boolean = false,
    val fetchProgress: String = "",
    val responseText: String = "",
    val clubTypes: List<ClubType> = emptyList(),
    val selectedKeys: Set<String> = emptySet(),
    val rawJsonResults: List<String> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var webView: WebView? = null

    // Guards evaluateJavascript — the WebView has one JS context; calls cannot overlap.
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

    // Called by JsBridge on the main thread. Resumes the suspended fetchOneSuspend coroutine.
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

        _uiState.update { it.copy(isLoading = true, rawJsonResults = emptyList(), responseText = "") }

        viewModelScope.launch {
            selected.forEachIndexed { i, club ->
                _uiState.update { it.copy(fetchProgress = "Fetching ${i + 1} of ${selected.size}...") }
                val result = fetchOneSuspend(club.pid, club.cgid)
                _uiState.update { it.copy(rawJsonResults = it.rawJsonResults + result) }
            }
            val joined = _uiState.value.rawJsonResults.joinToString("\n\n---\n\n")
            _uiState.update { it.copy(isLoading = false, fetchProgress = "", responseText = joined) }
        }
    }

    // Suspends until JsBridge delivers a result. Mutex ensures only one call is in-flight.
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
                        cgid = obj.getString("cgid"),
                        displayValue = obj.getString("displayValue"),
                        pid = obj.getString("pid")
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
