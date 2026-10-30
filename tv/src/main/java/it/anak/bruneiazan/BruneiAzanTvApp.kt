package it.anak.bruneiazan

import android.app.Application

/**
 * Process-wide application: owns the single [TvAppState] so settings
 * collectors and alarm writes share one instance across activities,
 * receivers, and services.
 */
class BruneiAzanTvApp : Application() {

    lateinit var appState: TvAppState
        private set

    override fun onCreate() {
        super.onCreate()
        appState = TvAppState(this)
    }
}
