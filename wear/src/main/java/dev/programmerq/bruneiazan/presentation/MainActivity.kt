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
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import dev.programmerq.bruneiazan.BruneiAzanWearApp
import dev.programmerq.bruneiazan.alarm.Rescheduler
import dev.programmerq.bruneiazan.presentation.theme.BruneiAzanTheme
import dev.programmerq.bruneiazan.shared.settings.Settings

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
