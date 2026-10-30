package it.anak.bruneiazan

import it.anak.bruneiazan.alarms.TimeWheel
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeWheelTest {

    @Test
    fun `wrap keeps values inside the range`() {
        assertEquals(0, TimeWheel.wrap(0, 24))
        assertEquals(23, TimeWheel.wrap(23, 24))
        assertEquals(59, TimeWheel.wrap(59, 60))
    }

    @Test
    fun `wrap rolls past the top back to zero`() {
        assertEquals(0, TimeWheel.wrap(24, 24))
        assertEquals(0, TimeWheel.wrap(60, 60))
        assertEquals(1, TimeWheel.wrap(25, 24))
    }

    @Test
    fun `wrap rolls below zero to the top`() {
        assertEquals(23, TimeWheel.wrap(-1, 24))
        assertEquals(59, TimeWheel.wrap(-1, 60))
        assertEquals(22, TimeWheel.wrap(-2, 24))
    }

    @Test
    fun `wrap handles large offsets both ways`() {
        assertEquals(2, TimeWheel.wrap(50, 24))
        assertEquals(21, TimeWheel.wrap(-51, 24))
    }
}
