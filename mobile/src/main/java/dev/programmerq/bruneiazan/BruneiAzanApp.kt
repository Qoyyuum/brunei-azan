package dev.programmerq.bruneiazan

import android.app.Application

/**
 * Process-wide application: owns the single [AppState] so settings collectors,
 * sync pushes, and alarm writes share one instance across activities,
 * receivers, and services.
 */
class BruneiAzanApp : Application() {

    lateinit var appState: AppState
        private set

    override fun onCreate() {
        super.onCreate()
        appState = AppState(this)
        appState.startSync()
    }
}
