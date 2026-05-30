# Callaway Golf Preowned — Android App

An Android app for researching and comparing pre-owned Callaway golf clubs available on [callawaygolfpreowned.com](https://www.callawaygolfpreowned.com). Select one or more club models, fetch live variant data (loft, shaft type, shaft flex, condition, pricing), and filter results to find the right club at the right price.

---

## Features

### Select tab
- **Card-based club selection** — each club is a selectable Material 3 card with a clear selection indicator (filled check vs. empty ring) that doesn't rely on color alone
- **Collapsible categories with sticky headers** — Drivers, Fairway Woods, Hybrids, Iron Sets, Single Irons, Wedges; the current category header stays pinned while scrolling
- **Smart default expansion** — categories containing selected clubs start expanded; otherwise only the first category opens
- **Category select-all** — tri-state control per category with a "N of M selected" subtitle
- **Watch Sets** — save the current selection as a named, reusable set; tap to reload it later (replaces selection and expands the relevant categories). Persists across app restarts

### Results tab
- **Two views** — switch between a dense scrollable **table** (power users) and phone-friendly **cards**; preference persists
- **Filtering** — five filter dropdowns (Club/Set, Club, Loft, Shaft Type, Shaft Flex) plus a Favorites filter, with active-filter chips and clear-all
- **Sorting** — Lowest/Highest Price, Club Name, Loft, Most Inventory
- **Favorites** — star clubs (persisted); filter results to favorites only
- **Product detail sheet** — tap any row or card for a full detail bottom sheet with all conditions and links
- **Live price links** — tap any price to open that exact listing on the Callaway site
- **Raw JSON viewer** — collapsible section with one-tap clipboard copy

### Throughout
- **Multi-club fetch** — retrieves all selected clubs in sequence with inline progress ("Fetching 2 of 5…")
- **Error handling** — friendly Snackbar messages for rate limits, network failures, and partial errors
- **Golf-themed Material 3 UI** — green color scheme, light/dark mode, empty states, accessible touch targets

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
| Persistence | DataStore Preferences (favorites, view mode, Watch Sets) |
| Club data | Bundled `assets/club_types.json` (30 clubs, 6 categories) |

- **Min SDK:** 28 (Android 9)
- **Target SDK:** 36
- **Build system:** Gradle 9.4.1 (Kotlin DSL), AGP 9.2.1

### Key Files

```
app/src/main/java/com/whutshisname/cgolfapp/
├── MainActivity.kt            — App shell: TopAppBar, tabs, session banner, Snackbar, Select tab
├── MainViewModel.kt           — All state, fetch orchestration, JSON parsing, filtering/sorting, error handling
├── JsBridge.kt                — @JavascriptInterface that routes JS results to ViewModel
├── data/
│   └── PreferencesRepository.kt — DataStore wrapper: favorites, view mode, Watch Sets
├── model/
│   ├── ClubType.kt            — Club identity (pid, cgid, displayValue)
│   ├── VariantRow.kt          — Flat display row + price/inventory helpers
│   └── WatchSet.kt            — Named, reusable selection of clubs
└── ui/
    ├── ClubCategoryGroup.kt   — Collapsible category section (sticky header) + club cards
    ├── WatchSetsBar.kt        — Watch Sets bar + save dialog
    ├── ResultsScreen.kt       — Filter/sort controls, table & card views, detail sheet, empty states
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

- **Club list is static** — `club_types.json` is bundled in assets; new Callaway models require a code update (a "Refresh Clubs" scraper is a planned feature; see `CLAUDE.md`)
- **Fetched results aren't cached** — favorites, view mode, and Watch Sets persist, but the fetched variant data itself is not saved between sessions
- **Sequential fetching** — clubs are fetched one at a time (WebView constraint); 10 clubs takes roughly 10× a single fetch
- **Session resets on rotation** — the WebView re-establishes its Cloudflare session after a screen rotation
