package com.whutshisname.cgolfapp

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

const val SITE_URL = "https://www.callawaygolfpreowned.com/"
const val API_URL =
    "https://www.callawaygolfpreowned.com/on/demandware.store/Sites-CGPO5-Site/default/Product-VariantData"
const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36"

data class UiState(
    val sessionReady: Boolean = false,
    val isLoading: Boolean = false,
    val responseText: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var webView: WebView? = null

    fun attachWebView(webView: WebView) {
        this.webView = webView
    }

    fun onSessionReady() {
        _uiState.update { it.copy(sessionReady = true) }
    }

    fun onFetchResult(result: String) {
        _uiState.update { it.copy(responseText = result, isLoading = false) }
    }

    fun fetch(pid: String, cgid: String) {
        _uiState.update { it.copy(isLoading = true, responseText = "") }
        val url = "$API_URL?pid=$pid&cgid=$cgid&format=json"
        webView?.evaluateJavascript(buildFetchJs(url), null)
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
