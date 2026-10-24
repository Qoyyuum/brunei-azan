package dev.programmerq.bruneiazan.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.programmerq.bruneiazan.presentation.WearAppState
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

/** SHORT_TEXT complication showing the next prayer, e.g. "Suboh 04:47". */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return complication("Suboh 04:47", "Suboh")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = WearAppState(this)
        val settings = app.settings.first()
        val today = LocalDate.now(BRUNEI_ZONE)
        val todayDay = app.repository.day(today, settings.district.apiKey) ?: return null
        val tomorrowDay = app.repository.day(today.plusDays(1), settings.district.apiKey)
        val next = nextPrayer(todayDay, tomorrowDay, LocalTime.now(BRUNEI_ZONE)) ?: return null
        return complication("${next.item.name} ${next.item.time24}", "Next prayer: ${next.item.name}")
    }

    private fun complication(text: String, description: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(description).build(),
        ).build()
}
