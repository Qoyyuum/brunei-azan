package it.anak.bruneiazan

import android.content.Context
import it.anak.bruneiazan.shared.api.WaktuApi
import it.anak.bruneiazan.shared.schedule.MonthCache
import it.anak.bruneiazan.shared.schedule.PrayerRepository
import it.anak.bruneiazan.shared.settings.Settings
import it.anak.bruneiazan.shared.settings.SettingsStore
import it.anak.bruneiazan.sync.SettingsSync
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Shared app state: settings store + prayer repository, plus a scope for UI-triggered writes. */
class AppState(context: Context) {
    private val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val repository = PrayerRepository(
        MonthCache(File(appContext.filesDir, "months")),
        WaktuApi,
    )
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: Flow<Settings> = settingsStore.settings

    private var syncJob: Job? = null

    @OptIn(FlowPreview::class)
    fun startSync() {
        if (syncJob?.isActive == true) return
        // Push settings to the watch whenever they change (debounced).
        // distinctUntilChanged stops identical-value writes (e.g. a sync echo)
        // from pushing straight back to the peer and looping forever.
        syncJob = scope.launch {
            settings
                .distinctUntilChanged()
                .debounce(500)
                .collect { SettingsSync.push(appContext, it) }
        }
    }
}
