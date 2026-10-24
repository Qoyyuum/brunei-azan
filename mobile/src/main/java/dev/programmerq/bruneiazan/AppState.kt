package dev.programmerq.bruneiazan

import android.content.Context
import dev.programmerq.bruneiazan.shared.api.WaktuApi
import dev.programmerq.bruneiazan.shared.schedule.MonthCache
import dev.programmerq.bruneiazan.shared.schedule.PrayerRepository
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.shared.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

/** Shared app state: settings store + prayer repository, plus a scope for UI-triggered writes. */
class AppState(context: Context) {
    val settingsStore = SettingsStore(context.applicationContext)
    val repository = PrayerRepository(
        MonthCache(File(context.applicationContext.filesDir, "months")),
        WaktuApi,
    )
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: Flow<Settings> = settingsStore.settings
}
