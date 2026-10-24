package dev.programmerq.bruneiazan.watchface

import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import dev.programmerq.bruneiazan.presentation.WearAppState
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.shared.settings.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val BRUNEI_ZONE: ZoneId = ZoneId.of("Asia/Brunei")

/** Snapshot of everything the renderer needs, kept off the render thread. */
class FaceState(
    val settings: AtomicReference<Settings> = AtomicReference(Settings.DEFAULT),
    val today: AtomicReference<PrayerDay?> = AtomicReference(null),
    val tomorrow: AtomicReference<PrayerDay?> = AtomicReference(null),
)

class AzanWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val faceState = FaceState()

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): WatchFace {
        val app = WearAppState(this)

        // Keep settings + schedule snapshots fresh for the renderer.
        serviceScope.launch {
            SettingsStore(this@AzanWatchFaceService).settings.collect {
                faceState.settings.set(it)
                refreshDays(app)
            }
        }
        serviceScope.launch {
            while (true) {
                refreshDays(app)
                delay(10 * 60_000)
            }
        }

        return WatchFace(
            WatchFaceType.DIGITAL,
            PaperRenderer(
                surfaceHolder = surfaceHolder,
                currentUserStyleRepository = currentUserStyleRepository,
                watchState = watchState,
                faceState = faceState,
            ),
        )
    }

    private suspend fun refreshDays(app: WearAppState) {
        val district = faceState.settings.get().district.apiKey
        val today = LocalDate.now(BRUNEI_ZONE)
        faceState.today.set(app.repository.day(today, district))
        faceState.tomorrow.set(app.repository.day(today.plusDays(1), district))
    }

    override fun onDestroy() {
        serviceScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        super.onDestroy()
    }
}
