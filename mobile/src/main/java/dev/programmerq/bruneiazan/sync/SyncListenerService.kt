package dev.programmerq.bruneiazan.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dev.programmerq.bruneiazan.alarm.Rescheduler
import dev.programmerq.bruneiazan.shared.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Applies settings pushed from the paired watch and reschedules alarms. */
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
                // Skip the echo of our own push.
                if (map.getLong(SettingsSync.FIELD_TS) <= SettingsSync.lastLocalPushTs.get()) {
                    continue
                }
                scope.launch {
                    SettingsStore(this@SyncListenerService).apply(json)
                    Rescheduler.schedule(this@SyncListenerService)
                }
            }
        }
    }
}
