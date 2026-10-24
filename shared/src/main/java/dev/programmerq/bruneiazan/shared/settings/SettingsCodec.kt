package dev.programmerq.bruneiazan.shared.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Single JSON-string codec shared by DataStore persistence and Data Layer sync. */
object SettingsCodec {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class CustomAlarmDto(
        val id: String = "",
        val hour: Int = 0,
        val minute: Int = 0,
        val label: String = "",
        val enabled: Boolean = true,
    )

    @Serializable
    private data class Dto(
        val district: String = Settings.DEFAULT.district.apiKey,
        val darkMode: String = Settings.DEFAULT.darkMode.name,
        val font: String = Settings.DEFAULT.font.name,
        val alarmSound: String = Settings.DEFAULT.alarmSound.name,
        val vibration: Boolean = Settings.DEFAULT.vibration,
        val prayerAlarms: Map<String, Boolean> = Settings.DEFAULT.prayerAlarms,
        val customAlarms: List<CustomAlarmDto> = emptyList(),
    )

    fun encode(s: Settings): String = json.encodeToString(
        Dto.serializer(),
        Dto(
            district = s.district.apiKey,
            darkMode = s.darkMode.name,
            font = s.font.name,
            alarmSound = s.alarmSound.name,
            vibration = s.vibration,
            prayerAlarms = s.prayerAlarms,
            customAlarms = s.customAlarms.map {
                CustomAlarmDto(it.id, it.hour, it.minute, it.label, it.enabled)
            },
        ),
    )

    fun decode(body: String): Settings = runCatching {
        val d = json.decodeFromString(Dto.serializer(), body)
        Settings(
            district = District.entries.firstOrNull { it.apiKey == d.district }
                ?: Settings.DEFAULT.district,
            darkMode = DarkMode.entries.firstOrNull { it.name == d.darkMode }
                ?: Settings.DEFAULT.darkMode,
            font = AppFont.entries.firstOrNull { it.name == d.font }
                ?: Settings.DEFAULT.font,
            alarmSound = AlarmSound.entries.firstOrNull { it.name == d.alarmSound }
                ?: Settings.DEFAULT.alarmSound,
            vibration = d.vibration,
            prayerAlarms = d.prayerAlarms,
            customAlarms = d.customAlarms.map {
                CustomAlarm(it.id, it.hour, it.minute, it.label, it.enabled)
            },
        )
    }.getOrDefault(Settings.DEFAULT)
}
