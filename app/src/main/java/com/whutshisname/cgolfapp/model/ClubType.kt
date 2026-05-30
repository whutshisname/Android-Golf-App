package com.whutshisname.cgolfapp.model

data class ClubType(
    val cgid: String,
    val displayValue: String,
    val pid: String
) {
    val selectionKey: String get() = "$pid|$cgid"
    val categoryLabel: String get() = cgid
        .split('-')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
