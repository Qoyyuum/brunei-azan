package it.anak.bruneiazan.alarm

import android.content.Context
import it.anak.bruneiazan.BruneiAzanTvApp
import it.anak.bruneiazan.shared.alarms.AlarmPlanner
import it.anak.bruneiazan.shared.alarms.AlarmScheduler
import it.anak.bruneiazan.shared.api.PrayerDay
import it.anak.bruneiazan.shared.schedule.MonthCache
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

/**
 * Computes the single next alarm from settings + the *cached* schedule and
 * registers it. Cache-only by design: receivers have a ~10s window and must
 * never block on the network; the month cache is refreshed by UI/app-open
 * paths instead.
 */
object Rescheduler {

    suspend fun schedule(context: Context) = withContext(Dispatchers.IO) {
        val app = (context.applicationContext as BruneiAzanTvApp).appState
        val settings = app.settings.first()
        val cache = MonthCache(File(context.filesDir, "months"))
        val district = settings.district.apiKey
        val today = LocalDate.now(BRUNEI_ZONE)
        val tomorrow = today.plusDays(1)

        val todayDay = cached(cache, district, today)
        val tomorrowDay = cached(cache, district, tomorrow)

        val next = todayDay?.let {
            AlarmPlanner.upcomingAlarms(settings, it, tomorrowDay, ZonedDateTime.now(BRUNEI_ZONE))
        }?.firstOrNull()
            // Custom alarms still fire when the schedule isn't cached.
            ?: AlarmPlanner.upcomingAlarms(
                settings,
                PrayerDay("", today.toString(), "", "", emptyList()),
                tomorrowDay,
                ZonedDateTime.now(BRUNEI_ZONE),
            ).firstOrNull()

        if (next != null) {
            AlarmScheduler.registerNext(context, AlarmReceiver::class.java, next)
        } else {
            AlarmScheduler.cancel(context, AlarmReceiver::class.java)
        }
    }

    private fun cached(cache: MonthCache, district: String, date: LocalDate): PrayerDay? =
        cache.get(district, YearMonth.from(date))?.firstOrNull { it.dateIso == date.toString() }
}
