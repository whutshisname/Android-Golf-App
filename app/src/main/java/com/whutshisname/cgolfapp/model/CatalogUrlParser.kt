package com.whutshisname.cgolfapp.model

// Centralised home for parsing a Callaway Preowned product URL into a club entry,
// and for the pid → display-name naming rules. Keeping it here lets the naming
// heuristics evolve in one place; the Admin screen also lets the user correct the
// generated name before saving, so imperfect output is never fatal.

data class ParsedClub(
    val cgid: String,
    val pid: String,
    val displayValue: String
)

// Singular/plural label appended to a generated display name, keyed by cgid.
// Unknown cgids fall back to a title-cased form of the cgid itself.
private val CATEGORY_LABELS = mapOf(
    "drivers" to "Driver",
    "mini-drivers" to "Mini Driver",
    "fairway-woods" to "Fairway Wood",
    "hybrids" to "Hybrid",
    "iron-sets" to "Irons",
    "single-irons" to "Irons",
    "wedges" to "Wedges"
)

private val YEAR_TOKEN = Regex("""^\d{4}$""")

// Tokens that read as initialisms on this site and should be fully upper-cased
// rather than title-cased (e.g. "Opus SP Chrome"). Note "ti" is deliberately NOT
// here — it title-cases to "Ti" (titanium). Extend as new models appear.
private val KNOWN_INITIALISMS = setOf("sp", "xr", "rtx", "cb", "mb", "ph", "ai")

/**
 * Parses a Callaway Preowned product URL such as
 * `https://www.callawaygolfpreowned.com/hybrids/hybrids-2026-apex-ti-super.html`
 * into its `cgid` (first path segment), `pid` (last segment, `.html` stripped),
 * and a generated [displayValue]. Returns null if the URL has no usable path.
 */
fun parseCallawayUrl(url: String): ParsedClub? {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return null

    // Strip scheme/host/query/fragment, keep the path segments.
    val path = trimmed
        .substringAfter("://", trimmed)
        .substringBefore('?')
        .substringBefore('#')
        .substringAfter('/', "")          // drop host
    val segments = path.split('/').map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.size < 2) return null

    val cgid = segments.first()
    val pid = segments.last().removeSuffix(".html")
    if (cgid.isEmpty() || pid.isEmpty()) return null

    return ParsedClub(cgid = cgid, pid = pid, displayValue = displayNameFromPid(pid, cgid))
}

/**
 * Builds a human display name from a pid + cgid: drops the leading category token
 * and any 4-digit year token, converts hyphens to spaces, title-cases each word,
 * then appends the category label. e.g.
 *   ("hybrids-2026-apex-ti-super", "hybrids")     → "Apex Ti Super Hybrid"
 *   ("wedges-2025-opus-sp-chrome", "wedges")      → "Opus SP Chrome Wedges"
 */
fun displayNameFromPid(pid: String, cgid: String): String {
    val tokens = pid.split('-').filter { it.isNotEmpty() }

    // Drop the leading category prefix (the cgid's own tokens) if present.
    val cgidTokens = cgid.split('-').filter { it.isNotEmpty() }
    var rest = tokens
    if (cgidTokens.isNotEmpty() && rest.take(cgidTokens.size) == cgidTokens) {
        rest = rest.drop(cgidTokens.size)
    } else if (rest.isNotEmpty() && rest.first() == cgid) {
        rest = rest.drop(1)
    }

    // Drop year tokens anywhere in the remainder.
    rest = rest.filterNot { YEAR_TOKEN.matches(it) }

    val name = rest.joinToString(" ") { titleCaseWord(it) }
    val label = CATEGORY_LABELS[cgid] ?: cgid.split('-').joinToString(" ") { titleCaseWord(it) }

    return when {
        name.isBlank() -> label
        else           -> "$name $label"
    }
}

// Title-case a token, except known initialisms which are fully upper-cased.
private fun titleCaseWord(word: String): String = when {
    word.isEmpty()                     -> word
    word.lowercase() in KNOWN_INITIALISMS -> word.uppercase()
    else                               -> word.replaceFirstChar { it.uppercase() }
}
