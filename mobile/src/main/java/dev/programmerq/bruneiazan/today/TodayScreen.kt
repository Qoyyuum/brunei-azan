package dev.programmerq.bruneiazan.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.programmerq.bruneiazan.AppState
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.api.ScheduleItem
import dev.programmerq.bruneiazan.shared.schedule.NextPrayer
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.ui.theme.BruneiAzanTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

@Composable
fun TodayScreen(app: AppState, settings: Settings) {
    var day by remember { mutableStateOf<PrayerDay?>(null) }
    var tomorrow by remember { mutableStateOf<PrayerDay?>(null) }
    var loading by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(LocalTime.now(BRUNEI_ZONE)) }

    fun load() {
        app.scope.launch {
            loading = true
            val today = LocalDate.now(BRUNEI_ZONE)
            day = app.repository.day(today, settings.district.apiKey)
            tomorrow = app.repository.day(today.plusDays(1), settings.district.apiKey)
            loading = false
        }
    }

    LaunchedEffect(settings.district) { load() }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now(BRUNEI_ZONE)
            delay(30_000)
        }
    }

    TodayContent(
        day = day,
        tomorrow = tomorrow,
        loading = loading,
        now = now,
        settings = settings,
        onRefresh = { load() },
    )
}

@Composable
fun TodayContent(
    day: PrayerDay?,
    tomorrow: PrayerDay?,
    loading: Boolean,
    now: LocalTime,
    settings: Settings,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (day == null) {
            Text(
                text = if (loading) "Loading…" else "Couldn't load prayer times.\nCheck your connection and refresh.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 48.dp),
            )
            TextButton(onClick = onRefresh, modifier = Modifier.padding(top = 8.dp)) {
                Text("Refresh", color = MaterialTheme.colorScheme.onBackground)
            }
            return@Column
        }

        Text(day.dateMalay, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${day.day} · ${day.hijri} · ${settings.district.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        val next = nextPrayer(day, tomorrow, now)
        if (next != null) {
            NextPrayerBanner(next, now)
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        day.schedule.forEach { item ->
            PrayerRow(item, isNext = next != null && !next.isTomorrow && next.item.key == item.key)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onRefresh) {
                Text("Refresh", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun NextPrayerBanner(next: NextPrayer, now: LocalTime) {
    val target = LocalTime.parse(next.item.time24)
    val countdown = countdownText(now, target, next.isTomorrow)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            next.item.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            next.item.time24,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = if (next.isTomorrow) "tomorrow · $countdown" else countdown,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun countdownText(now: LocalTime, target: LocalTime, isTomorrow: Boolean): String {
    var d = Duration.between(now, target)
    if (isTomorrow) d = d.plusHours(24)
    if (d.isNegative) d = d.plusHours(24)
    val h = d.toHours()
    val m = d.toMinutesPart()
    return if (h > 0) "in ${h}h ${m}m" else "in ${m}m"
}

@Composable
private fun PrayerRow(item: ScheduleItem, isNext: Boolean) {
    val rule = MaterialTheme.colorScheme.outline
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                item.time24,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = rule)
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayPreview() {
    val day = PrayerDay(
        day = "Sabtu",
        dateIso = "2026-10-24",
        dateMalay = "24 Oktober 2026",
        hijri = "13 Jamadilawal 1448",
        schedule = listOf(
            ScheduleItem("imsak", "Imsak", "04:37", "4:37 AM", "TAHAJJUD"),
            ScheduleItem("suboh", "Suboh", "04:47", "4:47 AM", "FARDHU"),
            ScheduleItem("syuruk", "Syuruk", "06:05", "6:05 AM", "TERBIT MATAHARI"),
            ScheduleItem("doha", "Doha", "06:28", "6:28 AM", "SUNAT"),
            ScheduleItem("zohor", "Zohor", "12:06", "12:06 PM", "FARDHU"),
            ScheduleItem("asar", "Asar", "15:24", "3:24 PM", "FARDHU"),
            ScheduleItem("maghrib", "Maghrib", "18:04", "6:04 PM", "FARDHU"),
            ScheduleItem("isyak", "Isyak", "19:14", "7:14 PM", "FARDHU"),
        ),
    )
    BruneiAzanTheme {
        TodayContent(day, day, false, LocalTime.of(11, 0), Settings.DEFAULT) {}
    }
}
