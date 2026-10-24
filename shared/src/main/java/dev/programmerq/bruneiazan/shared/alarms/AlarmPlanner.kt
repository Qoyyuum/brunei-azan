package dev.programmerq.bruneiazan.shared.alarms

import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.settings.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class AlarmInstance(
    val key: String,
    val label: String,
    val triggerAtMillis: Long,
    val isCustom: Boolean,
)

object AlarmPlanner {

    fun upcomingAlarms(
        settings: Settings,
        today: PrayerDay,
        tomorrow: PrayerDay?,
        now: ZonedDateTime,
    ): List<AlarmInstance> {
        val nowMillis = now.toInstant().toEpochMilli()
        val out = mutableListOf<AlarmInstance>()

        fun addDay(day: PrayerDay) {
            val date = runCatching { LocalDate.parse(day.dateIso) }.getOrNull() ?: return
            for (item in day.schedule) {
                if (settings.prayerAlarms[item.key] != true) continue
                val time = runCatching { LocalTime.parse(item.time24) }.getOrNull() ?: continue
                val millis = date.atTime(time).atZone(now.zone).toInstant().toEpochMilli()
                if (millis > nowMillis) {
                    out += AlarmInstance(item.key, item.name, millis, isCustom = false)
                }
            }
        }
        addDay(today)
        tomorrow?.let(::addDay)

        val todayDate = now.toLocalDate()
        for (custom in settings.customAlarms) {
            if (!custom.enabled) continue
            val time = runCatching { LocalTime.of(custom.hour, custom.minute) }.getOrNull() ?: continue
            var millis = todayDate.atTime(time).atZone(now.zone).toInstant().toEpochMilli()
            if (millis <= nowMillis) {
                millis = todayDate.plusDays(1).atTime(time).atZone(now.zone).toInstant().toEpochMilli()
            }
            out += AlarmInstance(
                key = custom.id,
                label = custom.label.ifBlank { "Alarm" },
                triggerAtMillis = millis,
                isCustom = true,
            )
        }

        // Stable sort keeps prayer items ahead of identical-time custom alarms;
        // distinctBy then collapses the duplicate toward the prayer.
        return out.sortedBy { it.triggerAtMillis }.distinctBy { it.triggerAtMillis }
    }
}
