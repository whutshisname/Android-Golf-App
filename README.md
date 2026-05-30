# Callaway Golf Preowned — Android App

An Android app for researching and comparing pre-owned Callaway golf clubs available on [callawaygolfpreowned.com](https://www.callawaygolfpreowned.com). Select one or more club models, fetch live variant data (loft, shaft type, shaft flex, condition, pricing), and filter results to find the right club at the right price.

---

## What It Does

Callaway Preowned lists each club model in multiple configurations — different lofts, shaft types, shaft flexes, and condition tiers (Outlet, Like New, Very Good, Good, Average) each with their own price and inventory. Browsing this manually is slow. This app lets you:

- **Select clubs** from a categorised list (Drivers, Fairway Woods, Hybrids, Iron Sets, Single Irons, Wedges) with Select All per category
- **Fetch live variant data** for all selected clubs in one tap
- **Filter results** by Club/Set, Club, Loft, Shaft Type, and Shaft Flex
- **Tap any price** to open that specific listing on the Callaway site
- **View raw JSON** from the API with a one-tap clipboard copy

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
├── MainActivity.kt            — Two-tab shell (Select / Results) with hidden WebView
├── MainViewModel.kt           — All state, fetch orchestration, JSON parsing
├── JsBridge.kt                — @JavascriptInterface that routes JS results to ViewModel
├── model/
│   ├── ClubType.kt            — Club identity (pid, cgid, displayValue)
│   └── VariantRow.kt          — Flat display row parsed from API response
└── ui/
    ├── ClubCategoryGroup.kt   — Category header + TriStateCheckbox + club list
    ├── ResultsScreen.kt       — Filter chips + horizontally scrollable table
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

## Current Limitations

- **Club list is static** — `club_types.json` is bundled in assets; new Callaway models require a code update
- **No persistence** — results are not saved between sessions
- **Sequential fetching** — clubs are fetched one at a time (WebView constraint); 10 clubs takes roughly 10× a single fetch
- **Session resets on rotation** — the WebView re-establishes its Cloudflare session after a screen rotation
- **No error recovery UI** — network failures surface as raw error text

---

## AI Planning Prompt

> The following prompt is a self-contained brief for an AI coding agent to analyse this project and propose a roadmap toward a more polished, professional Android app.

---

**Context for the agent:**

You are helping improve an Android hobby app called **Callaway Golf Preowned**. The app fetches live pricing and variant data for pre-owned Callaway golf clubs and lets the user filter and compare results. The GitHub repository is at `https://github.com/whutshisname/Android-Golf-App`.

**What the app does today:**
- Loads a bundled list of 30 Callaway club models (6 categories)
- User selects clubs via category-grouped checkboxes
- Tapping Fetch retrieves variant data (loft, shaft, flex, condition, price) for all selected clubs via a live API call
- Results displayed in a horizontally scrollable table with 5 filter dropdowns
- Price cells are tappable links to the Callaway product page
- Raw JSON viewer with clipboard copy

**Critical architectural constraint — do not design around this:**
The Callaway site uses Cloudflare Bot Management. All API calls must be made from inside an Android `WebView` (via `evaluateJavascript()` + `@JavascriptInterface`) because Cloudflare validates the TLS fingerprint and `cf_clearance` cookie. Standard HTTP clients (OkHttp, Retrofit, Ktor) are blocked. This constraint must be preserved in any proposed architecture.

**Tech stack:** Kotlin, Jetpack Compose, Material3, AndroidViewModel + StateFlow, org.json, Gradle 9.4.1 / AGP 9.2.1, minSdk 28.

**Goal:**
The owner wants to evolve this from a functional hobby POC into a **polished, professional-quality Android app**. The priority is **UX/UI quality** — the app should look and feel like a well-crafted native Android application. There is no specific feature wishlist; the owner wants the agent to analyse the current state and propose the most impactful improvements.

**Your task:**
1. Review the repository (linked above) to understand the current implementation in detail
2. Identify the most impactful UX/UI improvements — things a typical Android user would notice immediately
3. Identify any structural or architectural improvements that would support a higher-quality app (without violating the WebView constraint)
4. Propose a prioritised, incremental roadmap of improvements — each step independently deployable, building on the previous
5. For each proposed step, describe: what changes, which files are affected, and what the user experience improvement looks like

Focus on: visual polish, interaction quality, loading states, empty states, error handling, and any missing features that would make this genuinely useful as a daily-use tool for a golfer shopping on Callaway Preowned.
