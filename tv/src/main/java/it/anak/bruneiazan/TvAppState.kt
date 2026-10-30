package it.anak.bruneiazan

import android.content.Context
import it.anak.bruneiazan.shared.api.WaktuApi
import it.anak.bruneiazan.shared.schedule.MonthCache
import it.anak.bruneiazan.shared.schedule.PrayerRepository
import it.anak.bruneiazan.shared.settings.Settings
import it.anak.bruneiazan.shared.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

/**
 * Shared app state: settings store + prayer repository, plus a scope for
 * UI-triggered writes. No wearable sync — the Data Layer does not exist on
 * Android TV, so settings stay on this device.
 */
class TvAppState(context: Context) {
    private val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val repository = PrayerRepository(
        MonthCache(File(appContext.filesDir, "months")),
        WaktuApi,
    )
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: Flow<Settings> = settingsStore.settings
}
