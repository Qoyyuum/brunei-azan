package dev.programmerq.bruneiazan.alarm

import android.content.Context
import dev.programmerq.bruneiazan.presentation.WearAppState
import dev.programmerq.bruneiazan.shared.alarms.AlarmPlanner
import dev.programmerq.bruneiazan.shared.alarms.AlarmScheduler
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

object Rescheduler {

    suspend fun schedule(context: Context) {
        val app = WearAppState(context)
        val settings = app.settings.first()
        val today = LocalDate.now(BRUNEI_ZONE)
        val todayDay = app.repository.day(today, settings.district.apiKey)
        val tomorrowDay = app.repository.day(today.plusDays(1), settings.district.apiKey)

        val next = todayDay?.let {
            AlarmPlanner.upcomingAlarms(settings, it, tomorrowDay, ZonedDateTime.now(BRUNEI_ZONE))
        }?.firstOrNull()
            ?: AlarmPlanner.upcomingAlarms(
                settings,
                dev.programmerq.bruneiazan.shared.api.PrayerDay("", today.toString(), "", "", emptyList()),
                tomorrowDay,
                ZonedDateTime.now(BRUNEI_ZONE),
            ).firstOrNull()

        if (next != null) {
            AlarmScheduler.registerNext(context, AlarmReceiver::class.java, next)
        } else {
            AlarmScheduler.cancel(context, AlarmReceiver::class.java)
        }
    }
}
