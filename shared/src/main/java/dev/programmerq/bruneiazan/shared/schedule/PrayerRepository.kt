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
        val days = cache.get(district, ym)
        days?.firstOrNull { it.dateIso == date.toString() }?.let { return it }
        // A recently fetched month that doesn't contain the date means the API
        // doesn't publish that day — trust the coverage instead of refetching
        // on every call. Stale files are refetched in case coverage grew.
        val age = cache.fileAgeMillis(district, ym)
        if (days != null && age != null && age < FRESH_MILLIS) return null
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

    private companion object {
        const val FRESH_MILLIS = 24 * 60 * 60 * 1000L
    }
}
