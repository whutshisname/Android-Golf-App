package com.whutshisname.cgolfapp.model

// Local admin layer applied on top of the bundled club_types.json catalog.
// The bundled asset stays the source of truth; these overrides are persisted
// separately (DataStore) and combined at load time into the effective catalog:
//   effective = (bundled − hiddenPids) + addedClubs
data class CatalogOverride(
    val addedClubs: List<ClubType> = emptyList(),
    val hiddenPids: Set<String> = emptySet()
)
