package com.whutshisname.cgolfapp

import com.whutshisname.cgolfapp.model.displayNameFromPid
import com.whutshisname.cgolfapp.model.parseCallawayUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogUrlParserTest {

    @Test
    fun parsesCgidAndPid() {
        val parsed = parseCallawayUrl(
            "https://www.callawaygolfpreowned.com/hybrids/hybrids-2026-apex-ti-super.html"
        )!!
        assertEquals("hybrids", parsed.cgid)
        assertEquals("hybrids-2026-apex-ti-super", parsed.pid)
        assertEquals("Apex Ti Super Hybrid", parsed.displayValue)
    }

    @Test
    fun displayNameDropsCategoryAndYear() {
        assertEquals(
            "Elyte Triple Diamond Driver",
            displayNameFromPid("drivers-2025-elyte-triple-diamond", "drivers")
        )
    }

    @Test
    fun displayNameUppercasesKnownInitialisms() {
        assertEquals(
            "Opus SP Chrome Wedges",
            displayNameFromPid("wedges-2025-opus-sp-chrome", "wedges")
        )
    }

    @Test
    fun returnsNullForUnparseableUrl() {
        assertNull(parseCallawayUrl(""))
        assertNull(parseCallawayUrl("https://www.callawaygolfpreowned.com/"))
    }
}
