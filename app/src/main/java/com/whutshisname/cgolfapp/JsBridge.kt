package com.whutshisname.cgolfapp

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class JsBridge(private val onResult: (String) -> Unit) {
    @JavascriptInterface
    fun postResult(result: String) {
        Handler(Looper.getMainLooper()).post { onResult(result) }
    }
}
