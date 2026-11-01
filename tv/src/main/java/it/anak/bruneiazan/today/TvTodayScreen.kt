package it.anak.bruneiazan.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.anak.bruneiazan.TvAppState
import it.anak.bruneiazan.alarm.Rescheduler
import it.anak.bruneiazan.components.Divider
import it.anak.bruneiazan.components.FocusableRow
import it.anak.bruneiazan.shared.api.PrayerDay
import it.anak.bruneiazan.shared.api.ScheduleItem
import it.anak.bruneiazan.shared.schedule.NextPrayer
import it.anak.bruneiazan.shared.schedule.nextPrayer
import it.anak.bruneiazan.shared.settings.Settings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

@Composable
fun TvTodayScreen(app: TvAppState, settings: Settings) {
    val context = LocalContext.current
    var day by remember { mutableStateOf<PrayerDay?>(null) }
    var tomorrow by remember { mutableStateOf<PrayerDay?>(null) }
    var loading by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(LocalTime.now(BRUNEI_ZONE)) }

    fun load() {
        app.scope.launch {
            loading = true
            val today = LocalDate.now(BRUNEI_ZONE)
            app.repository.refreshMonth(today, settings.district.apiKey)
            day = app.repository.day(today, settings.district.apiKey)
            tomorrow = app.repository.day(today.plusDays(1), settings.district.apiKey)
            loading = false
            // Reschedule now that the month cache reflects this district —
            // the settings-change reschedule in AppShell races the first
            // fetch and would otherwise register nothing on first run.
            Rescheduler.schedule(context)
        }
    }

    LaunchedEffect(settings.district) { load() }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now(BRUNEI_ZONE)
            delay(30_000)
        }
    }

    TvTodayContent(
        day = day,
        tomorrow = tomorrow,
        loading = loading,
        now = now,
        onRefresh = { load() },
    )
}

@Composable
fun TvTodayContent(
    day: PrayerDay?,
    tomorrow: PrayerDay?,
    loading: Boolean,
    now: LocalTime,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
    ) {
        if (day == null) {
            item {
                Text(
                    text = if (loading) "Loading…" else "Couldn't load prayer times.\nCheck your connection and refresh.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
            item {
                FocusableRow(onClick = onRefresh) {
                    Text("Refresh", style = MaterialTheme.typography.bodyLarge)
                }
            }
            return@LazyColumn
        }

        item {
            Text(day.dateMalay, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${day.day} · ${day.hijri}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val next = nextPrayer(day, tomorrow, now)
        if (next != null) {
            item { NextPrayerBanner(next, now) }
        }

        item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }

        items(day.schedule) { item ->
            PrayerRow(item, isNext = next != null && !next.isTomorrow && next.item.key == item.key)
        }

        item {
            FocusableRow(onClick = onRefresh) {
                Text("Refresh", style = MaterialTheme.typography.bodyLarge)
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
        Divider()
    }
}
