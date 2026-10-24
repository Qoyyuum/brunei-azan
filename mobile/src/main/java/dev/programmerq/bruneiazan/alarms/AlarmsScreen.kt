package dev.programmerq.bruneiazan.alarms

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.programmerq.bruneiazan.AppState
import dev.programmerq.bruneiazan.components.RadioRow
import dev.programmerq.bruneiazan.components.SectionHeader
import dev.programmerq.bruneiazan.components.SwitchRow
import dev.programmerq.bruneiazan.shared.settings.AlarmSound
import dev.programmerq.bruneiazan.shared.settings.CustomAlarm
import dev.programmerq.bruneiazan.shared.settings.Settings
import java.util.UUID
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
fun AlarmsScreen(app: AppState, settings: Settings) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        SectionHeader("Prayer alarms")
        PRAYER_ORDER.forEach { (key, label) ->
            SwitchRow(
                label = label,
                checked = settings.prayerAlarms[key] == true,
                onChecked = { on ->
                    app.scope.launch { app.settingsStore.setPrayerAlarm(key, on) }
                },
            )
        }

        SectionHeader("Custom alarms")
        settings.customAlarms.forEach { alarm ->
            SwitchRow(
                label = "%02d:%02d".format(alarm.hour, alarm.minute),
                subtitle = alarm.label.ifBlank { null },
                checked = alarm.enabled,
                onChecked = { on ->
                    app.scope.launch {
                        app.settingsStore.upsertCustomAlarm(alarm.copy(enabled = on))
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                TextButton(onClick = {
                    app.scope.launch { app.settingsStore.removeCustomAlarm(alarm.id) }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = { showAddDialog = true }) {
            Text("+ Add alarm", color = MaterialTheme.colorScheme.onBackground)
        }

        SectionHeader("Sound")
        AlarmSound.entries.forEach { sound ->
            RadioRow(
                label = sound.label,
                selected = settings.alarmSound == sound,
                onClick = { app.scope.launch { app.settingsStore.setAlarmSound(sound) } },
                trailing = {
                    TextButton(onClick = {
                        playPreview(
                            when (sound) {
                                AlarmSound.AZAN -> dev.programmerq.bruneiazan.shared.R.raw.masjid_an_nabawi
                                AlarmSound.BEEP -> dev.programmerq.bruneiazan.shared.R.raw.beep
                            },
                        )
                    }) {
                        Text("Play", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        }
        TextButton(onClick = { testAlarm() }, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                if (testing) "Stop test" else "Test alarm (5s)",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            "Swap the azan recording by replacing res/raw/masjid_an_nabawi.mp3.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        SectionHeader("Vibration")
        SwitchRow(
            label = "Vibrate on alarm",
            checked = settings.vibration,
            onChecked = { on -> app.scope.launch { app.settingsStore.setVibration(on) } },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomAlarmDialog(onDismiss: () -> Unit, onAdd: (CustomAlarm) -> Unit) {
    var label by remember { mutableStateOf("") }
    val timeState = rememberTimePickerState(is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Add alarm", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(
                    CustomAlarm(
                        id = UUID.randomUUID().toString(),
                        hour = timeState.hour,
                        minute = timeState.minute,
                        label = label.trim(),
                        enabled = true,
                    ),
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
