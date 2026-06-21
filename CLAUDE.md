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

# single test class / method:
.\gradlew.bat test --tests "com.whutshisname.cgolfapp.ExampleUnitTest"
.\gradlew.bat test --tests "com.whutshisname.cgolfapp.ExampleUnitTest.addition_isCorrect"
```

`CatalogUrlParserTest` covers the URL → club parsing/naming logic; the generated
`ExampleUnitTest` / `ExampleInstrumentedTest` stubs are otherwise the only tests.

- minSdk 28, targetSdk 36, Kotlin 2.2.10, Compose BOM 2026.02.01, AGP 9.2.1
- SDK location in `local.properties` (auto-generated, not in VCS)

## Architecture

Single-activity Jetpack Compose app with a `MainViewModel` holding all state via `StateFlow`.
Two tabs: **Select** (club selection) and **Results** (filterable variant table).

`MainActivity` owns the (offscreen) `WebView` and wires it to the ViewModel via
`attachWebView()`; the ViewModel never touches Compose. All UI reads a single
`UiState` snapshot from `uiState: StateFlow<UiState>`.

Package layout under `com.whutshisname.cgolfapp`:
- root — `MainActivity`, `MainViewModel`, `JsBridge`
- `model/` — `ClubType`, `VariantRow` (+ `bestPrice()`/`availableConditionCount()` extensions), `WatchSet`, `CatalogOverride`, `CatalogUrlParser`
- `data/` — `PreferencesRepository` (DataStore)
- `ui/` — Compose screens/components incl. `AdminCatalogScreen`; `ui/theme/` — colors, type, theme

### State & data flow

`UiState` is the single source of truth. The pipeline is:
`fetchSelected()` → per-club `fetchOneSuspend()` → `parseVariantRows()` produces
`variantRows` → `applyFiltersAndSort()` derives `filteredRows` (what the table/cards
render). Any change to filters, `SortOrder`, or `searchQuery` re-derives
`filteredRows` from the immutable `variantRows` — fetched data is never re-requested
to filter or sort.

`buildErrorMessage()` distinguishes total failure (HTTP 429 rate-limit vs. network)
from partial success (some clubs failed, or fetched OK but returned zero
inventory) and surfaces a single user-facing string.

On the Results screen, a filter dropdown (Club/Set, Club, Loft, Shaft Type, Flex)
is only rendered when its option list (already blank-filtered and deduped) has
**more than one** value — a filter with zero or one real choice adds no value and
is hidden. Hidden filters keep their state (it lives in `UiState.filters`); the
whole filter row is omitted when none qualify so the layout reflows cleanly.

### Catalog: bundled asset + admin overrides

The club catalog is layered, never mutated in place:

```
effective = (bundled − hiddenPids) + addedClubs   // then deduped by selectionKey
```

- **Bundled** clubs load from `assets/club_types.json` into a private
  `bundledClubs` flow (`loadClubTypes()`).
- **Overrides** (`CatalogOverride` = `addedClubs` + `hiddenPids`) are persisted in
  DataStore and managed at runtime via the hidden **Admin** screen
  (`AdminCatalogScreen`), reached by **long-pressing the brand header**. Admin can
  add a club by pasting a Callaway product URL (parsed by `CatalogUrlParser`) and
  hide/restore bundled clubs.
- `collectCatalog()` `combine`s `bundledClubs` with the override flow into
  `uiState.clubTypes`. It **must `distinctBy { it.selectionKey }`** — an admin-added
  club can collide with a bundled one (e.g. the same club later shipped in
  `club_types.json`); duplicate `selectionKey`s crash the Select `LazyColumn`
  (which keys items by `selectionKey`). Bundled entries are listed first, so they
  win on collision.

The bundled JSON stays authoritative, so app updates that ship a larger
`club_types.json` keep working while admin additions/hides survive.

### Persistence (DataStore)

`PreferencesRepository` persists three things in the `golf_prefs` Preferences
DataStore, each serialized as a JSON string: the selected `ViewMode` (TABLE/CARDS),
the user's **Watch Sets** (named bundles of `ClubType.selectionKey`s, i.e.
`"$pid|$cgid"`), and the **catalog override** (`catalog_override` key — added clubs
+ hidden pids). The ViewModel `collect`s these flows in `init` and mirrors them
into `UiState`. Saving a watch set with an existing name replaces it
(case-insensitive); adding a club with an existing pid likewise replaces it.

### OkHttp is a dependency but NOT the HTTP client

`okhttp` is in `build.gradle.kts`, but all Callaway requests go through the WebView
(see below). Do not route club/variant fetches through OkHttp.

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
The bundled club list is `app/src/main/assets/club_types.json` (~35 clubs, 6 categories).
A club entry may also carry an optional `category` field to override the UI grouping
(e.g. mini-drivers grouped under Drivers). To add models permanently, edit that file
directly; for ad-hoc/local additions use the in-app Admin screen (see "Catalog"
above) — no rebuild needed.

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
