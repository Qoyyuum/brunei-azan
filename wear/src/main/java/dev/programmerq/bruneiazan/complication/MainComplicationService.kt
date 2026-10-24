package dev.programmerq.bruneiazan.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.programmerq.bruneiazan.BruneiAzanWearApp
import dev.programmerq.bruneiazan.shared.schedule.MonthCache
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

/** SHORT_TEXT complication showing the next prayer, e.g. "Suboh 04:47". */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return complication("04:47", "Suboh")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = (applicationContext as BruneiAzanWearApp).appState
        val settings = app.settings.first()
        // Cache-only like the tile — a complication request must never block
        // on the network.
        val cache = MonthCache(File(filesDir, "months"))
        val district = settings.district.apiKey
        val today = LocalDate.now(BRUNEI_ZONE)
        val tomorrow = today.plusDays(1)
        val todayDay = cache.get(district, YearMonth.from(today))
            ?.firstOrNull { it.dateIso == today.toString() } ?: return null
        val tomorrowDay = cache.get(district, YearMonth.from(tomorrow))
            ?.firstOrNull { it.dateIso == tomorrow.toString() }
        val next = nextPrayer(todayDay, tomorrowDay, LocalTime.now(BRUNEI_ZONE)) ?: return null
        return complication(next.item.time24, next.item.name)
    }

    /**
     * SHORT_TEXT with the prayer name in the title slot and the time as the
     * main text — two short fields the watch face can lay out instead of one
     * long string that gets truncated ("IMSAK 0...").
     */
    private fun complication(time: String, name: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(time).build(),
            contentDescription = PlainComplicationText
                .Builder("Next prayer: $name at $time")
                .build(),
        )
            .setTitle(PlainComplicationText.Builder(name).build())
            .build()
}
