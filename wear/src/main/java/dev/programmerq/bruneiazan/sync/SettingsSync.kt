package dev.programmerq.bruneiazan.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.shared.settings.SettingsCodec
import java.util.concurrent.atomic.AtomicLong

object SettingsSync {
    const val PATH = "/settings"
    const val FIELD_JSON = "json"
    const val FIELD_TS = "ts"

    /** Timestamp of our own last push, used to skip echo events on this node. */
    val lastLocalPushTs = AtomicLong(0)

    /** Best-effort push of the full settings blob to the paired phone. */
    fun push(context: Context, settings: Settings) {
        val ts = System.currentTimeMillis()
        lastLocalPushTs.set(ts)
        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putString(FIELD_JSON, SettingsCodec.encode(settings))
            dataMap.putLong(FIELD_TS, ts)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }
}
