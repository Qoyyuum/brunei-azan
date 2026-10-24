package dev.programmerq.bruneiazan.shared.schedule

import dev.programmerq.bruneiazan.shared.api.ApiResult
import dev.programmerq.bruneiazan.shared.api.MonthApi
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import java.time.LocalDate
import java.time.YearMonth

/**
 * Cache-first access to prayer schedules. The waktu.co.bn month endpoint may
 * return only part of a month, so lookups are keyed by the month containing
 * the requested date and a missing `dateIso` in the response is a null, not
 * an error.
 */
class PrayerRepository(
    private val cache: MonthCache,
    private val api: MonthApi,
) {

    suspend fun day(date: LocalDate, district: String): PrayerDay? {
        val ym = YearMonth.from(date)
        cache.get(district, ym)?.let { days ->
            days.firstOrNull { it.dateIso == date.toString() }?.let { return it }
        }
        return when (val r = refreshMonth(date, district)) {
            is ApiResult.Ok -> r.value.firstOrNull { it.dateIso == date.toString() }
            is ApiResult.Err -> null
        }
    }

    suspend fun refreshMonth(date: LocalDate, district: String): ApiResult<List<PrayerDay>> {
        val ym = YearMonth.from(date)
        return when (val r = api.fetchMonth(ym.year, ym.monthValue, district)) {
            is ApiResult.Ok -> {
                cache.put(district, ym, r.value)
                r
            }
            is ApiResult.Err -> r
        }
    }
}
