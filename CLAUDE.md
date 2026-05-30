# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

`JAVA_HOME` must point to the bundled Android Studio JDK if not set system-wide:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

```powershell
.\gradlew.bat assembleDebug           # APK → app/build/outputs/apk/debug/
.\gradlew.bat installDebug            # build + install to connected device/emulator
.\gradlew.bat test                    # unit tests
.\gradlew.bat connectedAndroidTest    # instrumented tests (requires device/emulator)
.\gradlew.bat lint
```

- minSdk 28, targetSdk 36, Kotlin 2.2.10, Compose BOM 2026.02.01, AGP 9.2.1
- SDK location in `local.properties` (auto-generated, not in VCS)

## Architecture

Single-activity Jetpack Compose app. Currently a POC in one file (`MainActivity.kt`).
Planned full port from `D:\dev\golf-scraper` (Next.js) will add club selection UI,
multi-fetch, a filterable results table, and a raw JSON viewer — see that repo for
the reference feature set and data shapes.

### The Cloudflare Trick — critical, do not change

The WebView is the HTTP client, not OkHttp. The Callaway preowned site uses
Cloudflare Bot Management which validates the TLS fingerprint of the client.
OkHttp presents a different TLS fingerprint than Chrome, so requests made directly
from OkHttp are rejected (HTTP 429) even with correct cookies.

The solution: a WebView loads `callawaygolfpreowned.com` at startup. Cloudflare's
JS challenge runs inside Chromium and sets a `cf_clearance` cookie bound to that
specific TLS fingerprint. All API calls are then made by injecting a JavaScript
`fetch()` into that same WebView via `evaluateJavascript()`. The result is returned
to Kotlin through `@JavascriptInterface` on `JsBridge`.

Do not replace WebView fetches with OkHttp/Retrofit/Ktor — the `cf_clearance`
cookie is cryptographically tied to the Chromium TLS fingerprint and will not work
from a different HTTP stack.

### evaluateJavascript() sequencing

The WebView has one JS context. Concurrent `evaluateJavascript()` calls interleave
unpredictably. When fetching multiple clubs, use a `Mutex` or `Channel` in the
ViewModel to ensure requests are sequential.

### JsBridge thread safety

`JsBridge.postResult()` is called by the JS engine on a background thread.
Always use `Handler(Looper.getMainLooper()).post { ... }` inside it before
updating any Compose state.

## Key Constants (MainActivity.kt)

- `SITE_URL`   = `https://www.callawaygolfpreowned.com/`
- `API_URL`    = `https://www.callawaygolfpreowned.com/on/demandware.store/Sites-CGPO5-Site/default/Product-VariantData`
- `USER_AGENT` = mobile Chrome on Pixel 8 — must match what the WebView presents to Cloudflare

## Callaway API

GET `{API_URL}?pid={pid}&cgid={cgid}&format=json`

Each club is identified by a `pid` (product ID) and `cgid` (category ID). The
reference list of all known clubs lives in `D:\dev\golf-scraper\src\data\clubTypes.json`
(31 clubs across 6 categories: drivers, fairway-woods, hybrids, wedges, iron-sets,
single-irons). This will be copied to `app/src/main/assets/club_types.json` when
the full port is implemented.

Each response has a `variants` array. Each variant is an array of `{label, value}`
objects. Price fields (Outlet, Like New, Very Good, Good, Average) have
`value = [sku, "$XX.XX", stock, url]` or the string `"-"`.
