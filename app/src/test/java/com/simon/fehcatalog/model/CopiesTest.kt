package com.simon.fehcatalog.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CopiesTest {

    @Test
    fun `copies are clamped to the valid range`() {
        assertEquals(0, normalizeCopies(-3))
        assertEquals(0, normalizeCopies(0))
        assertEquals(1, normalizeCopies(1))
        assertEquals(12, normalizeCopies(12))
        assertEquals(MAX_COPIES, normalizeCopies(MAX_COPIES + 1))
    }
}
