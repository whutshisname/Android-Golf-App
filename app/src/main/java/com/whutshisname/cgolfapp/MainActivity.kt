package com.whutshisname.cgolfapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whutshisname.cgolfapp.ui.ClubCategoryGroup
import com.whutshisname.cgolfapp.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ApiTestScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ApiTestScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Group clubs by category, preserving a stable category order.
    val categoryOrder = listOf("drivers", "fairway-woods", "hybrids", "iron-sets", "single-irons", "wedges")
    val grouped = remember(uiState.clubTypes) {
        uiState.clubTypes
            .groupBy { it.cgid }
            .entries
            .sortedBy { (cgid, _) -> categoryOrder.indexOf(cgid).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1dp — keeps the WebView in the tree so the Cloudflare session stays alive.
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = USER_AGENT
                    addJavascriptInterface(
                        JsBridge { result -> viewModel.onFetchResult(result) },
                        "AndroidBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (url?.contains("callawaygolfpreowned.com") == true)
                                viewModel.onSessionReady()
                        }
                    }
                    loadUrl(SITE_URL)
                }.also { viewModel.attachWebView(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        )

        Text("Callaway Golf", style = MaterialTheme.typography.headlineSmall)

        Text(
            text = if (uiState.sessionReady) "Session: Ready" else "Session: Establishing...",
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.sessionReady) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            grouped.forEach { (cgid, clubs) ->
                item(key = cgid) {
                    ClubCategoryGroup(
                        clubs = clubs.sortedBy { it.displayValue },
                        selectedKeys = uiState.selectedKeys,
                        onToggle = viewModel::toggleSelection,
                        onSelectAll = { selectAll -> viewModel.selectAllInCategory(cgid, selectAll) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Button(
            onClick = viewModel::fetchSelected,
            enabled = uiState.sessionReady && !uiState.isLoading && uiState.selectedKeys.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            val label = when {
                uiState.isLoading -> "Loading..."
                uiState.selectedKeys.isEmpty() -> "Select clubs to fetch"
                else -> "Fetch ${uiState.selectedKeys.size} selected"
            }
            Text(label)
        }

        if (uiState.responseText.isNotEmpty()) {
            Text(
                text = uiState.responseText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
