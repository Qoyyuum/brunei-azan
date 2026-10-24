# Brunei Azan — Phone + Watch Design

Date: 2026-10-22
Status: Approved (in-chat design review)

## Goal

A Wear OS watch app and companion Android phone app for Brunei Darussalam prayer
times, backed by the waktu.co.bn API. Users can view today's prayer schedule,
enable/disable alarms per prayer (plus custom one-off alarms), choose an alarm
sound (call-to-prayer tone or beep) with optional vibration, and use a custom
paper-style watch face showing the time and next prayer. A complication and tile
surface the next prayer on any other watch face.

Visual identity: a "paper" design — black and white only, thin rules, generous
whitespace, user-selectable font (Sans / Serif / Monospace), and a manual
dark/light mode toggle on both devices.

UI language: English labels; prayer names stay in Malay as returned by the API
(Imsak, Suboh, Syuruk, Doha, Zohor, Asar, Maghrib, Isyak).

## Confirmed decisions

- Watch face: custom `WatchFaceService` AND complication + tile.
- Azan audio: synthesized placeholder `.wav` shipped in `:shared/res/raw`,
  replaceable later by dropping a real recording into res/raw.
- Alarms: per-prayer auto-computed toggles for all 8 schedule items, plus
  optional custom one-off alarms, on both devices.
- Watch is standalone: fetches the API itself over its own network. Settings
  additionally sync phone <-> watch over the Wearable Data Layer when paired.
- Shared code lives in a new `:shared` Android library module.
- UI language: English with Malay prayer names.

## API contract (waktu.co.bn)

Base: `https://waktu.co.bn`

`GET /api?action=today&date=today&format=24h&district=<district>` returns:

```json
{
  "status": "success",
  "code": 200,
  "meta": {
    "source": "Pusat Da'wah Islamiah, KEMENTERIAN HAL EHWAL UGAMA",
    "timezone": "Asia/Brunei",
    "district": "Daerah Brunei-Muara",
    "offset_minutes": 0,
    "format": "24h",
    "qibla_bearing": "291.6° WNW",
    "generated_at": "..."
  },
  "data": {
    "day": "Isnin",
    "date": "28/09/2026",
    "date_iso": "2026-09-28",
    "date_malay": "28 September 2026",
    "hijri": "16 Rabiulakhir 1448",
    "timings": { "imsak": "04:41", "suboh": "04:51", "syuruk": "06:08", "doha": "06:31",
                 "zohor": "12:12", "asar": "15:22", "maghrib": "18:14", "isyak": "19:23" },
    "schedule": [
      { "key": "imsak", "name": "Imsak", "time": "04:41", "time_24": "04:41",
        "time_12": "4:41 AM", "type": "TAHAJJUD" }
    ]
  }
}
```

- `action=month&month=today|YYYY-MM` returns the whole month's schedule
  (parsed as a list of the same `data` objects).
- Districts: `brunei-muara` (+0m), `temburong` (+0m), `tutong` (+1m),
  `belait` (+3m). Default `brunei-muara`.
- Schedule `type` values: `TAHAJJUD`, `FARDHU`, `TERBIT MATAHARI`, `SUNAT`.
- Error responses: `{ status: "error", code: 400|404, message }`.

## Architecture

### `:shared` (new Android library, `dev.programmerq.bruneiazan.shared`)

- `api/`
  - `WaktuApi` — OkHttp `GET /api` with `action`, `date`/`month`, `format=24h`,
    `district` params; kotlinx.serialization parsing; returns
    `Result<PrayerDay>` / `Result<List<PrayerDay>>`; maps error bodies to a
    typed `ApiError`.
  - `Models.kt` — `DailyResponse`, `Meta`, `PrayerDay(day, dateIso, dateMalay,
    hijri, timings, schedule)`, `ScheduleItem(key, name, time24, time12, type)`.
- `settings/`
  - `Settings` data class: `district`, `darkMode` (LIGHT | DARK),
    `fontFamily` (SANS | SERIF | MONO), `alarmSound` (AZAN | BEEP),
    `vibration` (bool), `prayerAlarms` (map prayerKey -> enabled),
    `customAlarms` (list of {id, hour, minute, label, enabled}).
  - `SettingsStore` — androidx DataStore-preferences backed; exposes
    `Flow<Settings>` and suspend setters; serializes customAlarms as JSON.
- `schedule/`
  - `MonthCache` — stores fetched month JSON per `YYYY-MM` + district in app
    files dir; `today()` accessor; staleness check.
  - `nextPrayer(now, today, tomorrow?)` — returns next `ScheduleItem` and
    whether it is tomorrow's; after Isyak rolls to tomorrow's first item.
- `alarms/`
  - `AlarmPlanner` — pure function: `PrayerDay` + `Settings` + now → ordered
    list of upcoming `AlarmInstance(prayerKey, label, triggerAtMillis)` for
    enabled prayer alarms and enabled custom alarms.
- `res/raw/azan_placeholder.wav` — generated melodic tone standing in for the
  call to prayer; `res/raw/beep.wav` for the beep option. Generated once by a
  script at implementation time and committed.
- Dependencies added: OkHttp, kotlinx-serialization-json, datastore-preferences,
  kotlinx-coroutines.

### `:mobile` (companion app)

- Three destinations in a single `Scaffold` with bottom nav:
  - **Today** — full 8-row schedule, next prayer highlighted + countdown,
    Malay date + Hijri date, district label.
  - **Alarms** — toggle per prayer item; custom alarms list with add
    (time picker + label) / delete; sound picker (Azan placeholder / Beep) with
    preview; vibration toggle.
  - **Settings** — district picker (4 districts), dark/light toggle,
    font picker, data-layer sync status line.
- `alarm/` — `AlarmScheduler` wraps `AlarmManager.setAlarmClock()`;
  `AlarmReceiver` → posts high-importance notification (sound + vibration per
  settings) and launches full-screen `AlarmActivity` with dismiss action;
  `BootReceiver` + post-fire rescheduling via `AlarmPlanner`.
- `sync/` — `DataLayerService` (WearableListenerService) applying `/settings`
  DataMaps from the watch; settings changes locally are pushed via DataClient.
- Theme: `BruneiAzanTheme` reworked to the paper tokens — white bg / black ink,
  dark variant inverted, font applied from `Settings.fontFamily`.

### `:wear` (watch app)

- `presentation/` — Wear Compose (material3):
  - Prayer list screen (today's schedule, next highlighted).
  - Alarms screen — same toggles/custom-alarm model as phone.
  - Settings screen — district, sound, vibration, font, dark/light.
- `alarm/` — same `AlarmScheduler`/`AlarmReceiver` pattern; `AlarmActivity`
  full-screen with MediaPlayer + Vibrator and a dismiss edge button.
- `watchface/` — `AzanWatchFaceService` (androidx.wear.watchface) with a Canvas
  renderer: large current time, next prayer name + time + countdown, Gregorian
  and Hijri dates. Interactive mode renders paper styling (light or dark per
  settings); ambient mode is always thin white-on-black (burn-in safe). Reads
  `SettingsStore` directly — app settings apply to the face automatically.
- `complication/` — `MainComplicationService` updated to emit SHORT_TEXT
  `Suboh 04:51` (next prayer) from cache.
- `tile/` — `MainTileService` updated to render next prayer + today's list in
  the paper style.
- `sync/` — `WearDataLayerListenerService` applying `/settings` DataMaps and
  triggering alarm reschedule; local changes pushed via DataClient.
- Standalone fetch on app open / boot / post-alarm reschedule; month cache via
  `:shared`.

### Alarm scheduling model (both devices)

`setAlarmClock()` is used everywhere — exact firing, no `SCHEDULE_EXACT_ALARM`
permission, visible in system UI. Only the next upcoming alarm is registered;
when it fires, the receiver plays the alarm, then computes and registers the
following one. Reschedule triggers: after each fire, `BOOT_COMPLETED`, settings
change, month-cache refresh. This keeps a single source of truth in
`AlarmPlanner` instead of maintaining N parallel alarms.

### Sound

`AZAN` plays `res/raw/azan_placeholder.wav` (synthesized melodic sequence —
a stand-in the user replaces later); `BEEP` plays `res/raw/beep.wav`. Vibration
follows the `vibration` setting (pattern: long-pulse repeating while alarm UI
is shown).

### Permissions

- `:shared` manifest: `INTERNET`.
- `:mobile`: `INTERNET`, `VIBRATE`, `POST_NOTIFICATIONS`,
  `RECEIVE_BOOT_COMPLETED`.
- `:wear`: `INTERNET`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (already
  present), `BIND_WATCH_FACE` on the watch-face service, existing
  complication/tile provider permissions.

### Error handling

- API failure → typed `ApiError`; UI shows a "couldn't refresh — showing cached
  schedule" state; alarms continue from cache.
- Empty/absent cache → UI prompts to open the app online once; no alarms
  scheduled.
- Data Layer send failures are silently retried on next change (settings sync
  is best-effort).
- Alarm receiver reschedule is defensive: any exception still leaves the next
  alarm registered from cache.

## Testing

- `:shared` unit tests (JUnit4, run on JVM):
  - JSON parsing of the daily and monthly responses (fixture based on the
    OpenAPI example).
  - `nextPrayer`: before Imsak, between prayers, after Isyak (rolls to
    tomorrow), single-item lists.
  - `AlarmPlanner`: all toggles off → no alarms; mixed toggles; custom alarms
    in the past skipped; ordering.
- Manual/emulator verification:
  - `./gradlew :mobile:assembleDebug :wear:assembleDebug`.
  - Boot `Pixel_9_API_36`, install mobile APK, launch, screenshot the Today
    screen.
  - Boot `Wear_OS_Small_Round`, install wear APK, launch app, verify list;
    confirm the watch face appears in the face picker; verify tile preview.
  - `adb shell dumpsys alarm` to confirm the next `setAlarmClock` is
    registered.

## Out of scope

- Qibla compass (API exposes bearing; not requested).
- Watch Face Format (WFF) XML face — using the interactive androidx
  WatchFaceService instead.
- Play Store pairing/signing, release builds.
- Locale/l10n framework (English strings only).
