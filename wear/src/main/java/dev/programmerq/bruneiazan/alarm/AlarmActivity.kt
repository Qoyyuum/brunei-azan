package dev.programmerq.bruneiazan.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.programmerq.bruneiazan.BruneiAzanWearApp
import dev.programmerq.bruneiazan.presentation.theme.BruneiAzanTheme
import dev.programmerq.bruneiazan.shared.alarms.AlarmScheduler
import dev.programmerq.bruneiazan.shared.settings.AlarmSound
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

class AlarmActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "Prayer time"
        val app = (application as BruneiAzanWearApp).appState
        // The alarm UI is now up — remove the notification so its channel
        // sound doesn't play over the looping MediaPlayer.
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(AlarmReceiver.NOTIFICATION_ID)

        lifecycleScope.launch {
            val settings = app.settings.first()
            startAlarm(settings.alarmSound, settings.vibration)
            setContent {
                BruneiAzanTheme(
                    dark = settings.darkMode.isDark(isSystemInDarkTheme()),
                    font = settings.font,
                ) {
                    AlarmContent(label, onDismiss = { finish() })
                }
            }
        }
    }

    private fun startAlarm(sound: AlarmSound, vibrate: Boolean) {
        val res = when (sound) {
            AlarmSound.AZAN -> dev.programmerq.bruneiazan.shared.R.raw.masjid_an_nabawi
            AlarmSound.BEEP -> dev.programmerq.bruneiazan.shared.R.raw.beep
        }
        player = MediaPlayer.create(this, res)?.apply {
            isLooping = true
            start()
        }
        if (vibrate) {
            vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        }
    }

    private fun stopAlarm() {
        player?.stop()
        player?.release()
        player = null
        vibrator?.cancel()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(AlarmReceiver.NOTIFICATION_ID)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
private fun AlarmContent(label: String, onDismiss: () -> Unit) {
    AppScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                LocalTime.now(BRUNEI_ZONE).format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.displayMedium,
            )
            androidx.wear.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Dismiss")
            }
        }
    }
}
