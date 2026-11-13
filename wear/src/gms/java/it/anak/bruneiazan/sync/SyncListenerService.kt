package it.anak.bruneiazan.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import android.content.ComponentName
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import it.anak.bruneiazan.BruneiAzanWearApp
import it.anak.bruneiazan.alarm.Rescheduler
import it.anak.bruneiazan.complication.MainComplicationService
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

/** Applies settings pushed from the paired phone and reschedules alarms. */
class SyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(events: DataEventBuffer) {
        events.use { buffer ->
            for (event in buffer) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val item = event.dataItem
                if (item.uri.path != SettingsSync.PATH) continue
                val map = DataMapItem.fromDataItem(item).dataMap
                val json = map.getString(SettingsSync.FIELD_JSON) ?: continue
                if (map.getLong(SettingsSync.FIELD_TS) <= SettingsSync.lastLocalPushTs.get()) {
                    continue
                }
                scope.launch {
                    val app = (applicationContext as BruneiAzanWearApp).appState
                    app.settingsStore.apply(json)
                    // The reschedule below is cache-only, so make sure the
                    // pushed district's schedule is cached first.
                    val settings = app.settings.first()
                    app.repository.day(LocalDate.now(BRUNEI_ZONE), settings.district.apiKey)
                    Rescheduler.schedule(this@SyncListenerService)
                    ComplicationDataSourceUpdateRequester
                        .create(
                            this@SyncListenerService,
                            ComponentName(
                                this@SyncListenerService,
                                MainComplicationService::class.java,
                            ),
                        )
                        .requestUpdateAll()
                }
            }
        }
    }
}
