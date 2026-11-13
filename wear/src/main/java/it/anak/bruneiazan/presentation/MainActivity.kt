package it.anak.bruneiazan.presentation

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import it.anak.bruneiazan.BruneiAzanWearApp
import it.anak.bruneiazan.alarm.Rescheduler
import it.anak.bruneiazan.presentation.theme.BruneiAzanTheme
import it.anak.bruneiazan.shared.settings.Settings
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

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
        Rescheduler.schedule(app.appContext)
    }
    // Pages: Settings | Times | Alarms. Times lands first; swipe right for
    // Settings, swipe left for Alarms. One list state per screen so scroll
    // position doesn't bleed across pages.
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val listStates = listOf(
        rememberTransformingLazyColumnState(),
        rememberTransformingLazyColumnState(),
        rememberTransformingLazyColumnState(),
    )
    AppScaffold {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(state = pagerState) { page ->
                ScreenScaffold(scrollState = listStates[page]) {
                    when (page) {
                        0 -> SettingsScreen(app, settings, listStates[0])
                        1 -> PrayerListScreen(app, settings, listStates[1])
                        else -> AlarmsScreen(app, settings, listStates[2])
                    }
                }
            }
        }
    }
}
