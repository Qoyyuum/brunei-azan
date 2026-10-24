package dev.programmerq.bruneiazan.presentation

import android.content.Context
import dev.programmerq.bruneiazan.shared.api.WaktuApi
import dev.programmerq.bruneiazan.shared.schedule.MonthCache
import dev.programmerq.bruneiazan.shared.schedule.PrayerRepository
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.shared.settings.SettingsStore
import dev.programmerq.bruneiazan.sync.SettingsSync
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Watch-side app state: settings store + prayer repository + UI coroutine scope. */
class WearAppState(context: Context) {
    val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val repository = PrayerRepository(
        MonthCache(File(appContext.filesDir, "months")),
        WaktuApi,
    )
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: Flow<Settings> = settingsStore.settings

    @OptIn(FlowPreview::class)
    fun startSync() {
        // Push settings to the phone whenever they change (debounced).
        scope.launch {
            settings.debounce(500).collect { SettingsSync.push(appContext, it) }
        }
    }
}
