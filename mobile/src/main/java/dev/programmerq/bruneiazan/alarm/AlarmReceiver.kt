package dev.programmerq.bruneiazan.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import dev.programmerq.bruneiazan.BruneiAzanApp
import dev.programmerq.bruneiazan.R
import dev.programmerq.bruneiazan.shared.alarms.AlarmScheduler
import dev.programmerq.bruneiazan.shared.settings.AlarmSound
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "Prayer time"
        val key = intent.getStringExtra(AlarmScheduler.EXTRA_KEY)
        val isCustom = intent.getBooleanExtra(AlarmScheduler.EXTRA_CUSTOM, false)
        val triggerAt = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_MILLIS, -1L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = (context.applicationContext as BruneiAzanApp).appState
                val settings = app.settings.first()
                postAlarmNotification(context, label, settings.alarmSound, settings.vibration)
            } finally {
                runCatching { disableFiredCustoms(context, key, isCustom, triggerAt) }
                runCatching { Rescheduler.schedule(context) }
                pending.finish()
            }
        }
    }

    /**
     * One-off customs never re-arm. Disable the one that fired plus every
     * enabled custom whose instant collides with it — the planner registers a
     * single PendingIntent, so colliding customs would otherwise stay enabled
     * and fire a day late.
     */
    private suspend fun disableFiredCustoms(
        context: Context,
        key: String?,
        isCustom: Boolean,
        triggerAt: Long,
    ) {
        val app = (context.applicationContext as BruneiAzanApp).appState
        val today = LocalDate.now(BRUNEI_ZONE)
        app.settingsStore.update { st ->
            st.copy(
                customAlarms = st.customAlarms.map { c ->
                    val todayMillis = today
                        .atTime(LocalTime.of(c.hour, c.minute))
                        .atZone(BRUNEI_ZONE)
                        .toInstant()
                        .toEpochMilli()
                    if (c.enabled && ((isCustom && c.id == key) || todayMillis == triggerAt)) {
                        c.copy(enabled = false)
                    } else {
                        c
                    }
                },
            )
        }
    }

    private fun postAlarmNotification(
        context: Context,
        label: String,
        sound: AlarmSound,
        vibrate: Boolean,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm, context, sound, vibrate)

        val fullScreen = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_LABEL, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText("Prayer time")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(
        nm: NotificationManager,
        context: Context,
        sound: AlarmSound,
        vibrate: Boolean,
    ) {
        val soundUri = soundUri(context, sound)
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null &&
            (existing.sound != soundUri || existing.shouldVibrate() != vibrate)
        ) {
            nm.deleteNotificationChannel(CHANNEL_ID)
        }
        val channel = NotificationChannel(CHANNEL_ID, "Prayer alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Azan and prayer-time alarms"
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build(),
            )
            enableVibration(vibrate)
            if (vibrate) vibrationPattern = longArrayOf(0, 500, 400, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun soundUri(context: Context, sound: AlarmSound): Uri {
        val res = when (sound) {
            AlarmSound.AZAN -> dev.programmerq.bruneiazan.shared.R.raw.masjid_an_nabawi
            AlarmSound.BEEP -> dev.programmerq.bruneiazan.shared.R.raw.beep
        }
        return Uri.parse("android.resource://${context.packageName}/$res")
    }

    companion object {
        const val CHANNEL_ID = "azan_alarms"
        const val NOTIFICATION_ID = 4207
    }
}
