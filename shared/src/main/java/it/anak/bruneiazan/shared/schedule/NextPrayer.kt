package it.anak.bruneiazan.shared.schedule

import it.anak.bruneiazan.shared.api.PrayerDay
import it.anak.bruneiazan.shared.api.ScheduleItem
import java.time.LocalTime

data class NextPrayer(
    val item: ScheduleItem,
    val dateIso: String,
    val isTomorrow: Boolean,
)

fun nextPrayer(today: PrayerDay, tomorrow: PrayerDay?, now: LocalTime): NextPrayer? {
    today.schedule.firstOrNull { item ->
        runCatching { LocalTime.parse(item.time24) > now }.getOrDefault(false)
    }?.let { return NextPrayer(it, today.dateIso, isTomorrow = false) }
    return tomorrow?.schedule?.firstOrNull()?.let {
        NextPrayer(it, tomorrow.dateIso, isTomorrow = true)
    }
}
