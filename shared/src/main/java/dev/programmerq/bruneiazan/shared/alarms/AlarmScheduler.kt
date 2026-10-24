package dev.programmerq.bruneiazan.shared.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Registers the single next alarm via [AlarmManager.setAlarmClock] — exact,
 * doze-safe, and permission-free. Receivers call again after firing.
 */
object AlarmScheduler {

    const val EXTRA_KEY = "dev.programmerq.bruneiazan.extra.ALARM_KEY"
    const val EXTRA_LABEL = "dev.programmerq.bruneiazan.extra.ALARM_LABEL"
    const val EXTRA_CUSTOM = "dev.programmerq.bruneiazan.extra.ALARM_CUSTOM"

    fun registerNext(context: Context, receiverClass: Class<*>, alarm: AlarmInstance) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val receiverPi = receiverIntent(context, receiverClass, alarm)
        // setAlarmClock needs no permission on phones but DOES require an exact
        // alarm permission on Wear — USE_EXACT_ALARM is declared and auto-granted
        // for alarm apps, but fall back rather than crash if it's ever missing.
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(alarm.triggerAtMillis, appOpenIntent(context)),
                receiverPi,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                receiverPi,
            )
        }
    }

    fun cancel(context: Context, receiverClass: Class<*>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(receiverIntent(context, receiverClass, null))
    }

    private fun receiverIntent(context: Context, receiverClass: Class<*>, alarm: AlarmInstance?): PendingIntent {
        val intent = Intent(context, receiverClass).apply {
            if (alarm != null) {
                putExtra(EXTRA_KEY, alarm.key)
                putExtra(EXTRA_LABEL, alarm.label)
                putExtra(EXTRA_CUSTOM, alarm.isCustom)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun appOpenIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val REQUEST_CODE = 4207
}
