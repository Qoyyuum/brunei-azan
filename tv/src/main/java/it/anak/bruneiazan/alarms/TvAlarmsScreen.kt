package it.anak.bruneiazan.alarms

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.anak.bruneiazan.TvAppState
import it.anak.bruneiazan.components.FocusableRow
import it.anak.bruneiazan.components.SectionHeader
import it.anak.bruneiazan.components.TvRadioRow
import it.anak.bruneiazan.components.TvSwitchRow
import it.anak.bruneiazan.shared.settings.AlarmSound
import it.anak.bruneiazan.shared.settings.CustomAlarm
import it.anak.bruneiazan.shared.settings.Settings
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
fun TvAlarmsScreen(app: TvAppState, settings: Settings) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<MediaPlayer?>(null) }
    var testing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    fun stopTest() {
        preview?.release()
        preview = null
        vibrator?.cancel()
        testing = false
    }

    DisposableEffect(Unit) {
        onDispose {
            preview?.release()
            preview = null
            vibrator?.cancel()
        }
    }

    fun playPreview(resId: Int) {
        stopTest()
        preview = MediaPlayer.create(context, resId)?.apply { start() }
    }

    fun testAlarm() {
        if (testing) {
            stopTest()
            return
        }
        preview?.release()
        preview = MediaPlayer.create(
            context,
            when (settings.alarmSound) {
                AlarmSound.AZAN -> it.anak.bruneiazan.shared.R.raw.masjid_an_nabawi
                AlarmSound.BEEP -> it.anak.bruneiazan.shared.R.raw.beep
            },
        )?.apply { start() }
        // Vibrate regardless of the vibration setting — the test should
        // feel like a real alarm. No-ops on TVs without a vibrator.
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 400), -1),
        )
        testing = true
        app.scope.launch {
            delay(5_000)
            stopTest()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
    ) {
        item { SectionHeader("Prayer alarms") }
        items(PRAYER_ORDER) { (key, label) ->
            TvSwitchRow(
                label = label,
                checked = settings.prayerAlarms[key] == true,
                onChecked = { on ->
                    app.scope.launch { app.settingsStore.setPrayerAlarm(key, on) }
                },
            )
        }

        item { SectionHeader("Custom alarms") }
        items(settings.customAlarms, key = { it.id }) { alarm ->
            Column {
                TvSwitchRow(
                    label = "%02d:%02d".format(alarm.hour, alarm.minute),
                    subtitle = alarm.label.ifBlank { null },
                    checked = alarm.enabled,
                    onChecked = { on ->
                        app.scope.launch {
                            app.settingsStore.upsertCustomAlarm(alarm.copy(enabled = on))
                        }
                    },
                )
                FocusableRow(onClick = {
                    app.scope.launch { app.settingsStore.removeCustomAlarm(alarm.id) }
                }) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            FocusableRow(onClick = { showAddDialog = true }) {
                Text("+ Add alarm", style = MaterialTheme.typography.bodyLarge)
            }
        }

        item { SectionHeader("Sound") }
        items(AlarmSound.entries.toList()) { sound ->
            TvRadioRow(
                label = sound.label,
                selected = settings.alarmSound == sound,
                onClick = { app.scope.launch { app.settingsStore.setAlarmSound(sound) } },
                trailing = {
                    androidx.tv.material3.OutlinedButton(onClick = {
                        playPreview(
                            when (sound) {
                                AlarmSound.AZAN -> it.anak.bruneiazan.shared.R.raw.masjid_an_nabawi
                                AlarmSound.BEEP -> it.anak.bruneiazan.shared.R.raw.beep
                            },
                        )
                    }) {
                        Text(
                            "Play",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
        item {
            FocusableRow(onClick = { testAlarm() }) {
                Text(
                    if (testing) "Stop test" else "Test alarm (5s)",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item { SectionHeader("Vibration") }
        item {
            TvSwitchRow(
                label = "Vibrate on alarm",
                checked = settings.vibration,
                onChecked = { on -> app.scope.launch { app.settingsStore.setVibration(on) } },
            )
        }
    }

    if (showAddDialog) {
        AddCustomAlarmDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { alarm ->
                app.scope.launch { app.settingsStore.upsertCustomAlarm(alarm) }
                showAddDialog = false
            },
        )
    }
}
