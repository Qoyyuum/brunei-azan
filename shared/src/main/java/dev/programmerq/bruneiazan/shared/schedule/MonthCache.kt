package dev.programmerq.bruneiazan.shared.schedule

import dev.programmerq.bruneiazan.shared.api.PrayerDay
import java.io.File
import java.time.YearMonth
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Disk cache of parsed prayer days, one file per district+month:
 * `{dir}/{district}-{YYYY-MM}.json`.
 */
class MonthCache(private val dir: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PrayerDay.serializer())

    private fun file(district: String, yearMonth: YearMonth) =
        File(dir, "$district-$yearMonth.json")

    fun get(district: String, yearMonth: YearMonth): List<PrayerDay>? {
        val f = file(district, yearMonth)
        if (!f.isFile) return null
        return runCatching { json.decodeFromString(serializer, f.readText()) }.getOrNull()
    }

    fun put(district: String, yearMonth: YearMonth, days: List<PrayerDay>) {
        dir.mkdirs()
        file(district, yearMonth).writeText(json.encodeToString(serializer, days))
    }

    fun day(district: String, dateIso: String): PrayerDay? {
        val ym = runCatching { YearMonth.from(java.time.LocalDate.parse(dateIso)) }
            .getOrNull() ?: return null
        return get(district, ym)?.firstOrNull { it.dateIso == dateIso }
    }
}
