package it.anak.bruneiazan

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.anak.bruneiazan.alarm.Rescheduler
import it.anak.bruneiazan.alarms.TvAlarmsScreen
import it.anak.bruneiazan.settings.TvSettingsScreen
import it.anak.bruneiazan.shared.settings.Settings
import it.anak.bruneiazan.today.TvTodayScreen
import it.anak.bruneiazan.ui.theme.BruneiAzanTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as BruneiAzanTvApp).appState
        setContent {
            val settings by app.settings.collectAsStateWithLifecycle(Settings.DEFAULT)
            BruneiAzanTvTheme(
                dark = settings.darkMode.isDark(isSystemInDarkTheme()),
                font = settings.font,
            ) {
                AppShell(app, settings)
            }
        }
    }
}

@Composable
fun AppShell(app: TvAppState, settings: Settings) {
    val context = LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Reschedule the next alarm whenever settings change (and on first launch).
    LaunchedEffect(settings) {
        Rescheduler.schedule(context)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        PaperTabBar(selected = tab, onSelect = { tab = it })
        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                0 -> TvTodayScreen(app, settings)
                1 -> TvAlarmsScreen(app, settings)
                else -> TvSettingsScreen(app, settings)
            }
        }
    }
}

/**
 * Top tab bar — same three destinations as the phone's bottom bar, rebuilt
 * for D-pad: each tab is a focus stop, focus draws an ink box, selection
 * draws the paper underline.
 */
@Composable
private fun PaperTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Today", "Alarms", "Settings")
    val ink = MaterialTheme.colorScheme.onBackground
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            labels.forEachIndexed { i, label ->
                var focused by remember { mutableStateOf(false) }
                val isSelected = i == selected
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected || focused) {
                        ink
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = if (focused) ink else Color.Transparent,
                        )
                        .clickable { onSelect(i) }
                        .drawBehind {
                            if (isSelected) {
                                val y = size.height - 2.dp.toPx()
                                drawLine(
                                    ink,
                                    Offset(0f, y),
                                    Offset(size.width, y),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        it.anak.bruneiazan.components.Divider()
    }
}
