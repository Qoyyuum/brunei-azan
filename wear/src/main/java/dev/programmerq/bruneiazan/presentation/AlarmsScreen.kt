package dev.programmerq.bruneiazan.presentation

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.programmerq.bruneiazan.shared.settings.AlarmSound
import dev.programmerq.bruneiazan.shared.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PRAYER_ORDER = listOf(
    "imsak" to "Imsak",
    "suboh" to "Suboh",
    "syuruk" to "Syuruk",
    "doha" to "Doha",
    "zohor" to "Zohor",
    "asar" to "Asar",
    "maghrib" to "Maghrib",
    "isyak" to "Isyak",
)

@Composable
fun AlarmsScreen(app: WearAppState, settings: Settings, listState: TransformingLazyColumnState) {
    val context = LocalContext.current
    var testPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var testing by remember { mutableStateOf(false) }
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    fun stopTest() {
        testPlayer?.release()
        testPlayer = null
        vibrator?.cancel()
        testing = false
    }

    DisposableEffect(Unit) {
        onDispose {
            testPlayer?.release()
            testPlayer = null
            vibrator?.cancel()
        }
    }

    fun testAlarm() {
        if (testing) {
            stopTest()
            return
        }
        testPlayer?.release()
        testPlayer = MediaPlayer.create(
            context,
            when (settings.alarmSound) {
                AlarmSound.AZAN -> dev.programmerq.bruneiazan.shared.R.raw.masjid_an_nabawi
                AlarmSound.BEEP -> dev.programmerq.bruneiazan.shared.R.raw.beep
            },
        )?.apply { start() }
        // Vibrate regardless of the vibration setting — the test should
        // feel like a real alarm.
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 400), -1),
        )
        testing = true
        app.scope.launch {
            delay(5_000)
            stopTest()
        }
    }

    TransformingLazyColumn(state = listState, modifier = Modifier.padding(horizontal = 10.dp)) {
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Alarms") }
        }
        PRAYER_ORDER.forEach { (key, label) ->
            item {
                ToggleRow(
                    label = label,
                    on = settings.prayerAlarms[key] == true,
                ) {
                    app.scope.launch { app.settingsStore.setPrayerAlarm(key, it) }
                }
            }
        }
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Custom") }
        }
        settings.customAlarms.forEach { alarm ->
            item {
                ToggleRow(
                    label = "%02d:%02d".format(alarm.hour, alarm.minute) +
                        (alarm.label.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    on = alarm.enabled,
                ) { on ->
                    app.scope.launch {
                        app.settingsStore.upsertCustomAlarm(alarm.copy(enabled = on))
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        app.scope.launch { app.settingsStore.removeCustomAlarm(alarm.id) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text("Delete", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Sound") }
        }
        AlarmSound.entries.forEach { sound ->
            item {
                Button(
                    onClick = { app.scope.launch { app.settingsStore.setAlarmSound(sound) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text("${if (settings.alarmSound == sound) "●" else "○"} ${sound.label}")
                }
            }
        }
        item {
            Button(
                onClick = { testAlarm() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
                Text(if (testing) "Stop test" else "Test alarm (5s)")
            }
        }
        item {
            ToggleRow(
                label = "Vibrate",
                on = settings.vibration,
            ) { app.scope.launch { app.settingsStore.setVibration(it) } }
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Button(
        onClick = { onChange(!on) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(if (on) "on" else "off", style = MaterialTheme.typography.labelSmall)
    }
}
