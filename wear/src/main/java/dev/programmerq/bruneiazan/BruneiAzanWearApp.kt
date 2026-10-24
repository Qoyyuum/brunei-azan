package dev.programmerq.bruneiazan

import android.app.Application
import dev.programmerq.bruneiazan.presentation.WearAppState

/**
 * Process-wide application: owns the single [WearAppState] so settings
 * collectors, sync pushes, and alarm writes share one instance across
 * activities, receivers, the watch face, and services.
 */
class BruneiAzanWearApp : Application() {

    lateinit var appState: WearAppState
        private set

    override fun onCreate() {
        super.onCreate()
        appState = WearAppState(this)
        appState.startSync()
    }
}
