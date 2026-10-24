package dev.programmerq.bruneiazan.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import dev.programmerq.bruneiazan.BruneiAzanWearApp
import dev.programmerq.bruneiazan.alarm.Rescheduler
import dev.programmerq.bruneiazan.presentation.theme.BruneiAzanTheme
import dev.programmerq.bruneiazan.shared.settings.Settings

private val SCREEN_LABELS = listOf("Alarms", "Settings", "Times")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as BruneiAzanWearApp).appState
        setContent {
            val settings by app.settings.collectAsState(Settings.DEFAULT)
            BruneiAzanTheme(
                dark = settings.darkMode.isDark(isSystemInDarkTheme()),
                font = settings.font,
            ) {
                WearApp(app, settings)
            }
        }
    }
}

@Composable
fun WearApp(app: WearAppState, settings: Settings) {
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    // Reschedule the next alarm whenever settings change (and on first launch).
    LaunchedEffect(settings) {
        Rescheduler.schedule(app.appContext)
    }
    var screen by rememberSaveable { mutableIntStateOf(0) }
    // One list state per screen so scroll position doesn't bleed across tabs.
    val listStates = listOf(
        rememberTransformingLazyColumnState(),
        rememberTransformingLazyColumnState(),
        rememberTransformingLazyColumnState(),
    )
    AppScaffold {
        ScreenScaffold(
            scrollState = listStates[screen],
            edgeButton = {
                EdgeButton(
                    onClick = { screen = (screen + 1) % 3 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(SCREEN_LABELS[screen])
                }
            },
        ) {
            when (screen) {
                0 -> PrayerListScreen(app, settings, listStates[0])
                1 -> AlarmsScreen(app, settings, listStates[1])
                else -> SettingsScreen(app, settings, listStates[2])
            }
        }
    }
}
