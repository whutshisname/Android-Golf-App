# Callaway Golf Preowned — Android App

An Android app for researching and comparing pre-owned Callaway golf clubs available on [callawaygolfpreowned.com](https://www.callawaygolfpreowned.com). Select one or more club models, fetch live variant data (loft, shaft type, shaft flex, condition, pricing), and filter results to find the right club at the right price.

---

## Features

- **Category-grouped club selection** — Drivers, Fairway Woods, Hybrids, Iron Sets, Single Irons, Wedges with Select All per category and a live selection count badge
- **Multi-club fetch** — retrieves all selected clubs in sequence with inline progress ("Fetching 2 of 5…")
- **Filterable results table** — horizontally scrollable table with 5 filter dropdowns (Club/Set, Club, Loft, Shaft Type, Shaft Flex)
- **Live price links** — tap any price cell to open that exact listing on the Callaway site
- **Raw JSON viewer** — collapsible section with one-tap clipboard copy
- **Error handling** — friendly Snackbar messages for rate limits, network failures, and partial errors
- **Golf-themed Material 3 UI** — green color scheme, proper empty states, accessible touch targets

---

## Architecture

### The Cloudflare Constraint

The Callaway site is protected by Cloudflare Bot Management. Standard HTTP clients (OkHttp, Retrofit) are blocked with HTTP 429 because Cloudflare validates the TLS fingerprint of the client — and the `cf_clearance` cookie it issues is cryptographically bound to that fingerprint. A cookie extracted from one HTTP stack cannot be reused in another.

**Solution:** An embedded `WebView` (real Chromium) loads `callawaygolfpreowned.com` at startup. Cloudflare's JavaScript challenge runs inside the browser engine, establishing a valid session. All API calls are then made by injecting JavaScript `fetch()` into that same WebView via `evaluateJavascript()`. Results are returned to Kotlin through a `@JavascriptInterface`. The entire HTTP layer stays inside Chromium — Cloudflare sees no difference from a normal user.

Because `evaluateJavascript()` is not re-entrant (single JS context), multi-club fetches are sequential, coordinated by a `Mutex` + `suspendCancellableCoroutine`.

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| State | `AndroidViewModel` + `StateFlow` |
| Networking | Android `WebView` (Chromium) |
| JSON parsing | `org.json` (built-in Android) |
| Club data | Bundled `assets/club_types.json` (30 clubs, 6 categories) |

- **Min SDK:** 28 (Android 9)
- **Target SDK:** 36
- **Build system:** Gradle 9.4.1 (Kotlin DSL), AGP 9.2.1

### Key Files

```
app/src/main/java/com/whutshisname/cgolfapp/
├── MainActivity.kt            — App shell: TopAppBar, tabs, session banner, Snackbar
├── MainViewModel.kt           — All state, fetch orchestration, JSON parsing, error handling
├── JsBridge.kt                — @JavascriptInterface that routes JS results to ViewModel
├── model/
│   ├── ClubType.kt            — Club identity (pid, cgid, displayValue)
│   └── VariantRow.kt          — Flat display row parsed from API response
└── ui/
    ├── ClubCategoryGroup.kt   — Category header + TriStateCheckbox + club list
    ├── ResultsScreen.kt       — Filter chips + horizontally scrollable table + empty states
    └── JsonViewerSection.kt   — Collapsible raw JSON viewer with clipboard copy

app/src/main/assets/
└── club_types.json            — Bundled club list (update here to add new models)
```

---

## Building

`JAVA_HOME` must point to the Android Studio JDK if not set system-wide:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

---

## Known Limitations

- **Club list is static** — `club_types.json` is bundled in assets; new Callaway models require a code update
- **No persistence** — results are not saved between sessions
- **Sequential fetching** — clubs are fetched one at a time (WebView constraint); 10 clubs takes roughly 10× a single fetch
- **Session resets on rotation** — the WebView re-establishes its Cloudflare session after a screen rotation
