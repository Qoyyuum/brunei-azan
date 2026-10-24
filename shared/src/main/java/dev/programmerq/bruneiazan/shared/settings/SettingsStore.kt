package dev.programmerq.bruneiazan.shared.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "azan_settings")

class SettingsStore(private val context: Context) {

    private val key = stringPreferencesKey("settings_json")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        prefs[key]?.let { SettingsCodec.decode(it) } ?: Settings.DEFAULT
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[key]?.let { SettingsCodec.decode(it) } ?: Settings.DEFAULT
            prefs[key] = SettingsCodec.encode(transform(current))
        }
    }

    /**
     * Applies a settings JSON received over the Data Layer. Skips corrupt
     * payloads and no-op writes — both would otherwise emit downstream and
     * echo the value straight back to the peer that sent it.
     */
    suspend fun apply(json: String) {
        val decoded = SettingsCodec.decodeOrNull(json) ?: return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[key]?.let { SettingsCodec.decode(it) } ?: Settings.DEFAULT
            if (decoded != current) {
                prefs[key] = SettingsCodec.encode(decoded)
            }
        }
    }

    suspend fun setDistrict(d: District) = update { it.copy(district = d) }
    suspend fun setDarkMode(m: DarkMode) = update { it.copy(darkMode = m) }
    suspend fun setFont(f: AppFont) = update { it.copy(font = f) }
    suspend fun setAlarmSound(s: AlarmSound) = update { it.copy(alarmSound = s) }
    suspend fun setVibration(v: Boolean) = update { it.copy(vibration = v) }
    suspend fun setPrayerAlarm(key: String, enabled: Boolean) =
        update { it.copy(prayerAlarms = it.prayerAlarms + (key to enabled)) }

    suspend fun upsertCustomAlarm(alarm: CustomAlarm) = update { s ->
        s.copy(customAlarms = s.customAlarms.filterNot { it.id == alarm.id } + alarm)
    }

    suspend fun removeCustomAlarm(id: String) = update { s ->
        s.copy(customAlarms = s.customAlarms.filterNot { it.id == id })
    }
}
