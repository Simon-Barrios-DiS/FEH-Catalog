package com.simon.fehcatalog.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MergesTest {

    @Test
    fun `merges are clamped to the valid range`() {
        assertEquals(0, normalizeMerges(-4))
        assertEquals(0, normalizeMerges(0))
        assertEquals(7, normalizeMerges(7))
        assertEquals(10, normalizeMerges(10))
        assertEquals(10, normalizeMerges(42))
    }
}
