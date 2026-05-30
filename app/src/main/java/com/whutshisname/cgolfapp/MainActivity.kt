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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var pid by remember { mutableStateOf("irons-2024-apex-cb-chrome") }
    var cgid by remember { mutableStateOf("single-irons") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1dp keeps the WebView in the composition tree so the Cloudflare session
        // stays alive, but it takes no visible space.
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

        Text("Callaway Variant API POC", style = MaterialTheme.typography.headlineSmall)

        Text(
            text = if (uiState.sessionReady) "Session: Ready" else "Session: Establishing...",
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.sessionReady) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
        )

        OutlinedTextField(
            value = pid,
            onValueChange = { pid = it },
            label = { Text("pid") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cgid,
            onValueChange = { cgid = it },
            label = { Text("cgid") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.fetch(pid.trim(), cgid.trim()) },
            enabled = uiState.sessionReady && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoading) "Loading..." else "Fetch Variant Data")
        }

        if (uiState.responseText.isNotEmpty()) {
            Text(
                text = uiState.responseText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
