package com.simon.fehcatalog.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {

    @Test
    fun `search key ignores accents and case`() {
        assertEquals("otr", "Ótr".searchKey())
        assertEquals("jotun", "Jötun".searchKey())
        assertEquals("abel: the panther", "Abel: The Panther".searchKey())
    }

    @Test
    fun `search key transliterates nordic letters`() {
        assertEquals("thjazi", "Þjazi".searchKey())
        assertEquals("aegir", "Ægir".searchKey())
    }
}
