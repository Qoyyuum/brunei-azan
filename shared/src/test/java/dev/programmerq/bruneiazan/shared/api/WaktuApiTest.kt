package dev.programmerq.bruneiazan.shared.api

import org.junit.Assert.assertEquals
import org.junit.Test

class WaktuApiTest {

    @Test
    fun `monthParam uses MM-slash-YYYY format`() {
        assertEquals("10/2026", monthParam(2026, 10))
        assertEquals("01/2026", monthParam(2026, 1))
    }
}
