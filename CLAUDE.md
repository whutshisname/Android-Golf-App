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

Single-activity Jetpack Compose app with a `MainViewModel` holding all state via `StateFlow`.
Two tabs: **Select** (club selection) and **Results** (filterable variant table).

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
unpredictably. Multi-club fetches are serialised with a `Mutex` +
`suspendCancellableCoroutine` in `MainViewModel.fetchOneSuspend()`.

### JsBridge thread safety

`JsBridge.postResult()` is called by the JS engine on a background thread.
`Handler(Looper.getMainLooper()).post { ... }` is used inside it to safely resume
the suspended coroutine on the main thread.

## Key Constants (MainViewModel.kt)

- `SITE_URL`   = `https://www.callawaygolfpreowned.com/`
- `API_URL`    = `https://www.callawaygolfpreowned.com/on/demandware.store/Sites-CGPO5-Site/default/Product-VariantData`
- `USER_AGENT` = mobile Chrome on Pixel 8 — must match what the WebView presents to Cloudflare

## Callaway API

GET `{API_URL}?pid={pid}&cgid={cgid}&format=json`

Each club is identified by a `pid` (product ID) and `cgid` (category ID).
The bundled club list is `app/src/main/assets/club_types.json` (30 clubs, 6 categories).
To add new models, edit that file directly.

Each response has a `variants` array. Each variant is an array of `{label, value}`
objects. Price fields (Outlet, Like New, Very Good, Good, Average) have
`value = [sku, "$XX.XX", stock, url]` or the string `"-"`.

## Planned / Future Work

### Refresh Clubs (Settings menu)
The club list in `assets/club_types.json` is currently updated manually.
The planned feature is a **Settings screen** with a "Refresh Clubs" option that
uses the existing live WebView session to scrape Callaway's category pages and
auto-update the club list in-app — no code change or app update needed.

Implementation notes:
- Cloudflare blocks all standard HTTP clients (confirmed — HTTP 429 on WebFetch).
  The scraper must use the same `evaluateJavascript()` + `JsBridge` pattern as
  variant fetching. Direct OkHttp/Retrofit calls to category pages will not work.
- Category page URLs follow the pattern `callawaygolfpreowned.com/{cgid}`
  (e.g. `/drivers`, `/single-irons`). Product IDs and display names can be
  extracted from the page's JSON-LD or Demandware product tile markup.
- Scraped results should be written to `filesDir/club_types_scraped.json` and
  loaded in preference to the bundled asset on subsequent launches.
- `loadClubTypes()` in `MainViewModel` is the right place to add the
  scraped-file-first loading logic. See the `// TODO` comment there.
