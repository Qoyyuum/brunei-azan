package dev.programmerq.bruneiazan.shared

import dev.programmerq.bruneiazan.shared.settings.AppFont
import dev.programmerq.bruneiazan.shared.settings.CustomAlarm
import dev.programmerq.bruneiazan.shared.settings.DarkMode
import dev.programmerq.bruneiazan.shared.settings.District
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.shared.settings.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun `default settings round-trip`() {
        assertEquals(Settings.DEFAULT, SettingsCodec.decode(SettingsCodec.encode(Settings.DEFAULT)))
    }

    @Test
    fun `custom alarms and prayer toggles survive round-trip`() {
        val s = Settings.DEFAULT.copy(
            district = District.BELAIT,
            darkMode = DarkMode.DARK,
            font = AppFont.SERIF,
            prayerAlarms = mapOf("suboh" to true, "imsak" to true, "maghrib" to false),
            customAlarms = listOf(
                CustomAlarm("a", 6, 30, "Sahur", true),
                CustomAlarm("b", 22, 0, "", false),
            ),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
    }

    @Test
    fun `unknown keys are ignored`() {
        val json = """{"district":"tutong","darkMode":"DARK","font":"MONO","alarmSound":"BEEP",
            |"vibration":false,"prayerAlarms":{"suboh":true},"customAlarms":[],"future_field":42}"""
            .trimMargin()
        val s = SettingsCodec.decode(json)
        assertEquals(District.TUTONG, s.district)
        assertEquals(mapOf("suboh" to true), s.prayerAlarms)
    }

    @Test
    fun `missing fields fall back to defaults`() {
        val s = SettingsCodec.decode("""{"district":"belait"}""")
        assertEquals(District.BELAIT, s.district)
        assertEquals(Settings.DEFAULT.darkMode, s.darkMode)
        assertEquals(Settings.DEFAULT.prayerAlarms, s.prayerAlarms)
    }

    @Test
    fun `garbage returns defaults`() {
        assertEquals(Settings.DEFAULT, SettingsCodec.decode("not json"))
    }
}
