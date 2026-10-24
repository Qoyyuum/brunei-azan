package dev.programmerq.bruneiazan.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.programmerq.bruneiazan.alarm.Rescheduler
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import dev.programmerq.bruneiazan.shared.settings.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

@Composable
fun PrayerListScreen(app: WearAppState, settings: Settings, listState: TransformingLazyColumnState) {
    var day by remember { mutableStateOf<PrayerDay?>(null) }
    var tomorrow by remember { mutableStateOf<PrayerDay?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(settings.district) {
        app.scope.launch {
            val today = LocalDate.now(BRUNEI_ZONE)
            day = app.repository.day(today, settings.district.apiKey)
            tomorrow = app.repository.day(today.plusDays(1), settings.district.apiKey)
            loading = false
            // Reschedule now that the month cache reflects this district —
            // the settings-change reschedule in WearApp races the first
            // fetch and would otherwise register nothing on first run.
            Rescheduler.schedule(app.appContext)
        }
    }

    val next = day?.let { nextPrayer(it, tomorrow, LocalTime.now(BRUNEI_ZONE)) }

    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.padding(horizontal = 10.dp),
        contentPadding = PaddingValues(bottom = 56.dp),
    ) {
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (loading) "Loading…" else (day?.dateMalay ?: "No data — check connection"),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        if (next != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        next.item.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        next.item.time24,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (next.isTomorrow) {
                        Text("tomorrow", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        day?.schedule?.forEach { item ->
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (next?.item?.key == item.key && !next.isTomorrow) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(item.time24, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}
