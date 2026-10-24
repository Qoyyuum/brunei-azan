package dev.programmerq.bruneiazan.shared

import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.api.ScheduleItem
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextPrayerTest {

    private fun day(dateIso: String, vararg times: Pair<String, String>) = PrayerDay(
        day = "X",
        dateIso = dateIso,
        dateMalay = "x",
        hijri = "x",
        schedule = times.map { (key, t) ->
            ScheduleItem(key, key.replaceFirstChar { it.uppercase() }, t, t, "FARDHU")
        },
    )

    private val today = day(
        "2026-10-24",
        "imsak" to "04:37", "suboh" to "04:47", "syuruk" to "06:05", "doha" to "06:28",
        "zohor" to "12:06", "asar" to "15:24", "maghrib" to "18:04", "isyak" to "19:14",
    )
    private val tomorrow = day(
        "2026-10-25",
        "imsak" to "04:37", "suboh" to "04:47", "syuruk" to "06:05", "doha" to "06:28",
        "zohor" to "12:06", "asar" to "15:24", "maghrib" to "18:04", "isyak" to "19:14",
    )

    @Test
    fun `before imsak returns imsak today`() {
        val next = nextPrayer(today, tomorrow, LocalTime.of(3, 0))!!
        assertEquals("imsak", next.item.key)
        assertFalse(next.isTomorrow)
        assertEquals("2026-10-24", next.dateIso)
    }

    @Test
    fun `midday returns asar today`() {
        val next = nextPrayer(today, tomorrow, LocalTime.of(13, 0))!!
        assertEquals("asar", next.item.key)
        assertFalse(next.isTomorrow)
    }

    @Test
    fun `after isyak rolls to tomorrow imsak`() {
        val next = nextPrayer(today, tomorrow, LocalTime.of(20, 0))!!
        assertEquals("imsak", next.item.key)
        assertTrue(next.isTomorrow)
        assertEquals("2026-10-25", next.dateIso)
    }

    @Test
    fun `after isyak with no tomorrow returns null`() {
        assertNull(nextPrayer(today, null, LocalTime.of(20, 0)))
    }

    @Test
    fun `exactly at a prayer time returns the next one`() {
        val next = nextPrayer(today, tomorrow, LocalTime.of(12, 6))!!
        assertEquals("asar", next.item.key)
    }
}
