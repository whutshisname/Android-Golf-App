package com.whutshisname.cgolfapp.model

data class ClubType(
    val cgid: String,
    val displayValue: String,
    val pid: String,
    // UI grouping/label. Defaults to cgid, but can differ when a club's site
    // category isn't how we want it grouped in the app (e.g. mini drivers, whose
    // real cgid varies but which we group under one "Mini Drivers" section).
    val category: String = cgid
) {
    val selectionKey: String get() = "$pid|$cgid"
    val categoryLabel: String get() = category
        .split('-')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
