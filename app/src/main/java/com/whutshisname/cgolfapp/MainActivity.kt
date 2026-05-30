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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whutshisname.cgolfapp.ui.ClubCategoryGroup
import com.whutshisname.cgolfapp.ui.ResultsScreen
import com.whutshisname.cgolfapp.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Auto-switch to Results tab when fetch completes.
    LaunchedEffect(uiState.variantRows.size) {
        if (uiState.variantRows.isNotEmpty()) pagerState.animateScrollToPage(1)
    }

    Column(modifier = modifier.fillMaxSize()) {
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
            modifier = Modifier.fillMaxWidth().height(1.dp)
        )

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Select") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    val count = uiState.variantRows.size
                    Text(if (count > 0) "Results ($count)" else "Results")
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> SelectTab(uiState, viewModel)
                1 -> ResultsScreen(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun SelectTab(uiState: UiState, viewModel: MainViewModel) {
    val categoryOrder = listOf("drivers", "fairway-woods", "hybrids", "iron-sets", "single-irons", "wedges")
    val grouped = remember(uiState.clubTypes) {
        uiState.clubTypes
            .groupBy { it.cgid }
            .entries
            .sortedBy { (cgid, _) -> categoryOrder.indexOf(cgid).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (uiState.sessionReady) "Session: Ready" else "Session: Establishing...",
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.sessionReady) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
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

        if (uiState.isLoading) {
            Text(
                text = uiState.fetchProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = viewModel::fetchSelected,
            enabled = uiState.sessionReady && !uiState.isLoading && uiState.selectedKeys.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(
                when {
                    uiState.isLoading -> "Fetching..."
                    uiState.selectedKeys.isEmpty() -> "Select clubs to fetch"
                    else -> "Fetch ${uiState.selectedKeys.size} selected"
                }
            )
        }
    }
}
