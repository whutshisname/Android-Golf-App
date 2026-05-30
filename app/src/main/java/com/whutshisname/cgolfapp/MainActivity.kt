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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                AppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-switch to Results tab when fetch completes
    LaunchedEffect(uiState.variantRows.size) {
        if (uiState.variantRows.isNotEmpty()) pagerState.animateScrollToPage(1)
    }

    // Show error messages via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, actionLabel = "Dismiss")
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Callaway Preowned",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // 1dp — keeps the WebView in the tree so the Cloudflare session stays alive
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

            // Session-establishing banner — disappears once session is ready
            if (!uiState.sessionReady) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Establishing secure session…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

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
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (cgid, clubs) ->
                item(key = cgid) {
                    ClubCategoryGroup(
                        clubs = clubs.sortedBy { it.displayValue },
                        selectedKeys = uiState.selectedKeys,
                        favoritePids = uiState.favoritePids,
                        onToggle = viewModel::toggleSelection,
                        onSelectAll = { selectAll -> viewModel.selectAllInCategory(cgid, selectAll) },
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                }
            }
        }

        // Fetch button with inline progress — replaces the separate LinearProgressIndicator
        Button(
            onClick = viewModel::fetchSelected,
            enabled = uiState.sessionReady && !uiState.isLoading && uiState.selectedKeys.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(uiState.fetchProgress.ifEmpty { "Fetching…" })
            } else {
                Text(
                    when {
                        uiState.selectedKeys.isEmpty() -> "Select clubs above"
                        else -> "Fetch ${uiState.selectedKeys.size} selected"
                    }
                )
            }
        }
    }
}
