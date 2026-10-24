package dev.programmerq.bruneiazan.shared

import dev.programmerq.bruneiazan.shared.alarms.AlarmPlanner
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.api.ScheduleItem
import dev.programmerq.bruneiazan.shared.settings.CustomAlarm
import dev.programmerq.bruneiazan.shared.settings.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlannerTest {

    private val zone = ZoneId.of("Asia/Brunei")

    private fun day(dateIso: String) = PrayerDay(
        day = "X", dateIso = dateIso, dateMalay = "x", hijri = "x",
        schedule = listOf(
            "imsak" to "04:37", "suboh" to "04:47", "syuruk" to "06:05", "doha" to "06:28",
            "zohor" to "12:06", "asar" to "15:24", "maghrib" to "18:04", "isyak" to "19:14",
        ).map { (key, t) ->
            ScheduleItem(key, key.replaceFirstChar { it.uppercase() }, t, t, "FARDHU")
        },
    )

    private val today = day("2026-10-24")
    private val tomorrow = day("2026-10-25")

    private fun at(h: Int, m: Int = 0) =
        LocalDateTime.of(LocalDate.of(2026, 10, 24), LocalTime.of(h, m)).atZone(zone)

    private fun millis(dateIso: String, t: String) =
        LocalDateTime.of(LocalDate.parse(dateIso), LocalTime.parse(t)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `all toggles off yields no alarms`() {
        val settings = Settings.DEFAULT.copy(prayerAlarms = emptyMap())
        assertTrue(AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(13)).isEmpty())
    }

    @Test
    fun `defaults at 13h give asar then maghrib isyak then tomorrow`() {
        val alarms = AlarmPlanner.upcomingAlarms(Settings.DEFAULT, today, tomorrow, at(13))
        assertEquals("asar", alarms[0].key)
        assertEquals(millis("2026-10-24", "15:24"), alarms[0].triggerAtMillis)
        assertEquals(listOf("asar", "maghrib", "isyak", "suboh"), alarms.map { it.key }.take(4))
        assertEquals(millis("2026-10-25", "04:47"), alarms[3].triggerAtMillis)
    }

    @Test
    fun `before dawn includes all of today then tomorrow`() {
        val alarms = AlarmPlanner.upcomingAlarms(Settings.DEFAULT, today, tomorrow, at(3))
        assertEquals("suboh", alarms[0].key)
        assertEquals(millis("2026-10-24", "04:47"), alarms[0].triggerAtMillis)
    }

    @Test
    fun `disabled prayer is skipped`() {
        val settings = Settings.DEFAULT.copy(prayerAlarms = mapOf("suboh" to false, "maghrib" to true))
        val alarms = AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(3))
        assertTrue(alarms.none { it.key == "suboh" })
        assertEquals("maghrib", alarms[0].key)
    }

    @Test
    fun `past custom alarm fires tomorrow`() {
        val settings = Settings.DEFAULT.copy(
            prayerAlarms = emptyMap(),
            customAlarms = listOf(CustomAlarm("c1", 9, 0, "Wake", true)),
        )
        val alarms = AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(13))
        assertEquals(1, alarms.size)
        assertTrue(alarms[0].isCustom)
        assertEquals("Wake", alarms[0].label)
        val expected = LocalDateTime.of(LocalDate.of(2026, 10, 25), LocalTime.of(9, 0))
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, alarms[0].triggerAtMillis)
    }

    @Test
    fun `future custom alarm fires today`() {
        val settings = Settings.DEFAULT.copy(
            prayerAlarms = emptyMap(),
            customAlarms = listOf(CustomAlarm("c1", 16, 30, "Tea", true)),
        )
        val alarms = AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(13))
        assertEquals(millis("2026-10-24", "16:30"), alarms[0].triggerAtMillis)
    }

    @Test
    fun `disabled custom alarm is absent`() {
        val settings = Settings.DEFAULT.copy(
            prayerAlarms = emptyMap(),
            customAlarms = listOf(CustomAlarm("c1", 16, 30, "Tea", false)),
        )
        assertTrue(AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(13)).isEmpty())
    }

    @Test
    fun `custom alarm duplicating a prayer time collapses to the prayer`() {
        val settings = Settings.DEFAULT.copy(
            customAlarms = listOf(CustomAlarm("c1", 15, 24, "Dupe", true)),
        )
        val alarms = AlarmPlanner.upcomingAlarms(settings, today, tomorrow, at(13))
        assertEquals(1, alarms.count { it.triggerAtMillis == millis("2026-10-24", "15:24") })
        assertEquals("asar", alarms.first { it.triggerAtMillis == millis("2026-10-24", "15:24") }.key)
    }
}
