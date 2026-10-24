package dev.programmerq.bruneiazan.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
import dev.programmerq.bruneiazan.presentation.theme.BruneiAzanTheme
import dev.programmerq.bruneiazan.shared.settings.DarkMode
import dev.programmerq.bruneiazan.shared.settings.Settings

private val SCREEN_LABELS = listOf("Alarms", "Settings", "Times")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = WearAppState(this)
        setContent {
            val settings by app.settings.collectAsState(Settings.DEFAULT)
            BruneiAzanTheme(
                dark = settings.darkMode == DarkMode.DARK,
                font = settings.font,
            ) {
                WearApp(app, settings)
            }
        }
    }
}

@Composable
fun WearApp(app: WearAppState, settings: Settings) {
    var screen by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberTransformingLazyColumnState()
    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
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
                0 -> PrayerListScreen(app, settings, listState)
                1 -> AlarmsScreen(app, settings, listState)
                else -> SettingsScreen(app, settings, listState)
            }
        }
    }
}
