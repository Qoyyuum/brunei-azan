package it.anak.bruneiazan.sync

import android.content.Context
import it.anak.bruneiazan.shared.settings.Settings

/** No-op: the foss build has no Wearable Data Layer, so settings are not synced. */
object SettingsSync {
    const val PATH = "/settings"
    const val FIELD_JSON = "json"
    const val FIELD_TS = "ts"

    fun push(context: Context, settings: Settings) {}
}
