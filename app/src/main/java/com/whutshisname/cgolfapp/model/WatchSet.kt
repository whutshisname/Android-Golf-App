package com.whutshisname.cgolfapp.model

/**
 * A reusable, named collection of club selections. [selectionKeys] holds each
 * club's selectionKey ("pid|cgid"), so loading a set restores selection state
 * exactly. Persisted via PreferencesRepository.
 */
data class WatchSet(
    val name: String,
    val selectionKeys: Set<String>
)
