package it.anak.bruneiazan

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.anak.bruneiazan.alarm.Rescheduler
import it.anak.bruneiazan.alarms.AlarmsScreen
import it.anak.bruneiazan.settings.SettingsScreen
import it.anak.bruneiazan.shared.settings.Settings
import it.anak.bruneiazan.today.TodayScreen
import it.anak.bruneiazan.ui.theme.BruneiAzanTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = (application as BruneiAzanApp).appState
        enableEdgeToEdge()
        setContent {
            val settings by app.settings.collectAsStateWithLifecycle(Settings.DEFAULT)
            BruneiAzanTheme(
                dark = settings.darkMode.isDark(isSystemInDarkTheme()),
                font = settings.font,
            ) {
                AppShell(app, settings)
            }
        }
    }
}

@Composable
fun AppShell(app: AppState, settings: Settings) {
    val context = LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var lastDistrict by remember { mutableStateOf(settings.district) }
    // Reschedule the next alarm whenever settings change (and on first launch).
    LaunchedEffect(settings) {
        if (settings.district != lastDistrict) {
            lastDistrict = settings.district
            // The reschedule below is cache-only, so cache the new district's
            // schedule first — otherwise it would cancel the prayer alarms.
            withContext(Dispatchers.IO) {
                app.repository.day(LocalDate.now(BRUNEI_ZONE), settings.district.apiKey)
            }
        }
        Rescheduler.schedule(context)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { PaperTabBar(selected = tab, onSelect = { tab = it }) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                0 -> TodayScreen(app, settings)
                1 -> AlarmsScreen(app, settings)
                else -> SettingsScreen(app, settings)
            }
        }
    }
}

@Composable
private fun PaperTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Today", "Alarms", "Settings")
    val rule = MaterialTheme.colorScheme.outline
    Column {
        HorizontalDivider(thickness = 1.dp, color = rule)
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val isSelected = i == selected
                val ink = MaterialTheme.colorScheme.onBackground
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) }
                        .drawBehind {
                            if (isSelected) {
                                val y = 0f
                                drawLine(ink, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                            }
                        }
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}
