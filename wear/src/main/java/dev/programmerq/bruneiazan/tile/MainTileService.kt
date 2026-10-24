package dev.programmerq.bruneiazan.tile

import android.content.Context
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography.BODY_SMALL
import androidx.wear.protolayout.material3.Typography.DISPLAY_SMALL
import androidx.wear.protolayout.material3.Typography.LABEL_MEDIUM
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.schedule.MonthCache
import dev.programmerq.bruneiazan.shared.schedule.NextPrayer
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val RESOURCES_VERSION = "0"
private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

class MainTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(tile(requestParams, this))

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(resources(requestParams))
}

private fun resources(requestParams: ResourcesRequest): Resources =
    Resources.Builder().setVersion(RESOURCES_VERSION).build()

private fun cachedDays(context: Context, district: String): Pair<PrayerDay?, PrayerDay?> {
    val cache = MonthCache(File(context.filesDir, "months"))
    val today = LocalDate.now(BRUNEI_ZONE)
    val days = cache.get(district, YearMonth.from(today))
    val todayDay = days?.firstOrNull { it.dateIso == today.toString() }
    val tomorrowDay = cache
        .get(district, YearMonth.from(today.plusDays(1)))
        ?.firstOrNull { it.dateIso == today.plusDays(1).toString() }
    return todayDay to tomorrowDay
}

private fun tile(requestParams: RequestBuilders.TileRequest, context: Context): TileBuilders.Tile {
    val district = runBlocking {
        dev.programmerq.bruneiazan.shared.settings.SettingsStore(context).settings.first().district.apiKey
    }
    val (today, tomorrow) = cachedDays(context, district)
    val now = LocalTime.now(BRUNEI_ZONE)
    val next = today?.let { nextPrayer(it, tomorrow, now) }

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setFreshnessIntervalMillis(30 * 60_000)
        .setTileTimeline(
            TimelineBuilders.Timeline.fromLayoutElement(
                materialScope(context, requestParams.deviceConfiguration) {
                    primaryLayout(
                        mainSlot = {
                            val col = LayoutElementBuilders.Column.Builder()
                            if (next != null) {
                                col.addContent(
                                    text(
                                        next.item.name.uppercase().layoutString,
                                        typography = LABEL_MEDIUM,
                                    ),
                                )
                                col.addContent(
                                    text(
                                        next.item.time24.layoutString,
                                        typography = DISPLAY_SMALL,
                                    ),
                                )
                                upcoming(today, tomorrow, now).take(4).forEach { item ->
                                    col.addContent(
                                        text(
                                            "${item.name}  ${item.time24}".layoutString,
                                            typography = BODY_SMALL,
                                        ),
                                    )
                                }
                            } else {
                                col.addContent(
                                    text(
                                        "No prayer data — open the app".layoutString,
                                        typography = BODY_SMALL,
                                    ),
                                )
                            }
                            col.build()
                        },
                    )
                },
            ),
        )
        .build()
}

private fun upcoming(today: PrayerDay, tomorrow: PrayerDay?, now: LocalTime) =
    today.schedule.filter { runCatching { LocalTime.parse(it.time24) > now }.getOrDefault(false) } +
        (tomorrow?.schedule ?: emptyList())

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}
