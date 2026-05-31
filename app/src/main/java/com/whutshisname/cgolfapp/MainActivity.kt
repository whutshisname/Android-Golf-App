package com.whutshisname.cgolfapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whutshisname.cgolfapp.ui.theme.BrandOrange
import com.whutshisname.cgolfapp.ui.theme.HeaderBlack
import com.whutshisname.cgolfapp.ui.theme.HeaderOnDark
import com.whutshisname.cgolfapp.ui.SaveWatchSetDialog
import com.whutshisname.cgolfapp.ui.WatchSetsBar
import com.whutshisname.cgolfapp.ui.clubCategorySection
import com.whutshisname.cgolfapp.ui.ResultsScreen
import com.whutshisname.cgolfapp.ui.theme.MyApplicationTheme

// Stable Select-tab category order. Subcategories sit directly after their parent.
private val CATEGORY_ORDER = listOf(
    "drivers", "mini-drivers", "fairway-woods", "hybrids", "iron-sets", "single-irons", "wedges"
)

// Maps a UI subcategory to its parent category label (for nested presentation).
private val SUBCATEGORY_PARENTS = mapOf(
    "mini-drivers" to "Drivers"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force light status-bar icons — the brand header is always near-black.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MyApplicationTheme {
                AppScreen()
            }
        }
    }
}

// Premium black & orange brand header: logo at left, name + motto to its right,
// on a near-black surface with a thin orange accent line beneath.
@Composable
private fun BrandHeader() {
    Column {
        Surface(color = HeaderBlack, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.teeithigher_logo),
                    contentDescription = "TeeItHigher logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HeaderOnDark
                    )
                    Text(
                        text = stringResource(R.string.app_motto).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = BrandOrange
                    )
                }
            }
        }
        HorizontalDivider(thickness = 2.dp, color = BrandOrange)
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
        topBar = { BrandHeader() },
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
    val grouped = remember(uiState.clubTypes) {
        uiState.clubTypes
            .groupBy { it.category }
            .entries
            .sortedBy { (category, _) -> CATEGORY_ORDER.indexOf(category).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    // Per-category expansion state (ephemeral UI state). Smart default applied once
    // clubs load: expand categories that contain selected clubs; if none selected,
    // expand only the first category. Users can freely toggle afterwards.
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    var defaultsApplied by remember { mutableStateOf(false) }
    LaunchedEffect(grouped) {
        if (!defaultsApplied && grouped.isNotEmpty()) {
            val withSelections = grouped.filter { (_, clubs) ->
                clubs.any { it.selectionKey in uiState.selectedKeys }
            }
            if (withSelections.isNotEmpty()) {
                withSelections.forEach { (category, _) -> expandedCategories[category] = true }
            } else {
                expandedCategories[grouped.first().key] = true
            }
            defaultsApplied = true
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    if (showSaveDialog) {
        SaveWatchSetDialog(
            selectionCount = uiState.selectedKeys.size,
            onConfirm = { name -> viewModel.saveWatchSet(name); showSaveDialog = false },
            onDismiss = { showSaveDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
    ) {
        WatchSetsBar(
            watchSets = uiState.watchSets,
            canSave = uiState.selectedKeys.isNotEmpty(),
            onLoad = { set ->
                viewModel.loadWatchSet(set)
                // Expand categories that contain any club from the loaded set
                grouped.forEach { (category, clubs) ->
                    if (clubs.any { it.selectionKey in set.selectionKeys }) {
                        expandedCategories[category] = true
                    }
                }
            },
            onDelete = viewModel::deleteWatchSet,
            onSaveClick = { showSaveDialog = true }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            grouped.forEach { (category, clubs) ->
                val parent = SUBCATEGORY_PARENTS[category]
                clubCategorySection(
                    category = category,
                    label = clubs.first().categoryLabel,
                    clubs = clubs.sortedBy { it.displayValue },
                    selectedKeys = uiState.selectedKeys,
                    expanded = expandedCategories[category] ?: false,
                    isSubcategory = parent != null,
                    parentLabel = parent,
                    onToggleExpanded = {
                        expandedCategories[category] = !(expandedCategories[category] ?: false)
                    },
                    onToggle = viewModel::toggleSelection,
                    onSelectAll = { selectAll -> viewModel.selectAllInCategory(category, selectAll) }
                )
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
