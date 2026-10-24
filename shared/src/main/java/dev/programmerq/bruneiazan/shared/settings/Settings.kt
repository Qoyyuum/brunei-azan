package dev.programmerq.bruneiazan.shared.settings

enum class District(val apiKey: String, val label: String) {
    BRUNEI_MUARA("brunei-muara", "Brunei-Muara"),
    TUTONG("tutong", "Tutong"),
    BELAIT("belait", "Belait"),
    TEMBURONG("temburong", "Temburong"),
}

enum class DarkMode { LIGHT, DARK }

enum class AppFont(val label: String) { SANS("Sans"), SERIF("Serif"), MONO("Monospace") }

enum class AlarmSound(val label: String) { AZAN("Azan"), BEEP("Beep") }

data class CustomAlarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
)

data class Settings(
    val district: District,
    val darkMode: DarkMode,
    val font: AppFont,
    val alarmSound: AlarmSound,
    val vibration: Boolean,
    val prayerAlarms: Map<String, Boolean>,
    val customAlarms: List<CustomAlarm>,
) {
    companion object {
        val DEFAULT = Settings(
            district = District.BRUNEI_MUARA,
            darkMode = DarkMode.LIGHT,
            font = AppFont.SANS,
            alarmSound = AlarmSound.AZAN,
            vibration = true,
            prayerAlarms = mapOf(
                "suboh" to true,
                "zohor" to true,
                "asar" to true,
                "maghrib" to true,
                "isyak" to true,
            ),
            customAlarms = emptyList(),
        )
    }
}
