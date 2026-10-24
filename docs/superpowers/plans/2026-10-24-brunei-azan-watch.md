# Brunei Azan — Phone + Watch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Brunei Azan companion phone app and Wear OS watch app — prayer times from waktu.co.bn, per-prayer and custom alarms with azan/beep sounds and vibration, a custom paper-style watch face plus tile and complication, dark/light paper theming with font choice, and settings sync over the Wearable Data Layer.

**Architecture:** New `:shared` Android library owns the API client, models, month disk cache, DataStore settings, and pure alarm-planning logic. `:mobile` and `:wear` are thin Compose shells that reuse it; each device fetches the API itself and schedules its own `setAlarmClock` alarms; settings sync via Wearable Data Layer.

**Tech Stack:** Kotlin (AGP built-in), Jetpack Compose + Wear Compose material3, OkHttp + kotlinx.serialization, DataStore preferences, AlarmManager, androidx.wear.watchface, Wearable Data Layer, JUnit4.

**Spec:** `docs/superpowers/specs/2026-10-22-brunei-azan-watch-design.md`

## Global Constraints

- API base `https://waktu.co.bn/api`; always send `format=24h` and a `district` param (`brunei-muara`, `tutong`, `belait`, `temburong`; default `brunei-muara`).
- **API quirk (verified live):** `action=month` requires `month=MM` or `MM/YYYY` (e.g. `10/2026`). Documented `YYYY-MM` returns 404. Month `data` is a JSON array of daily objects and may only cover part of the month — always fetch the month containing the date needed; never assume full coverage.
- Alarm mechanism is `AlarmManager.setAlarmClock()` everywhere — no exact-alarm permission, exact firing, doze-safe.
- minSdk 35, targetSdk 37, Java 11 — `java.time` available natively. All times interpreted in `Asia/Brunei` (device-local equivalent: the API returns wall-clock times; use `ZoneId.of("Asia/Brunei")`).
- Paper aesthetic: black/white only, thin rules, no color accents; fonts limited to `Sans | Serif | Monospace` built-in families; ambient watch-face rendering is always white-on-black.
- New deps go in `gradle/libs.versions.toml`; versions must be existing published releases (pin okhttp `4.12.0`, kotlinx-serialization-json `1.8.1`, datastore-preferences `1.1.7`, kotlinx-coroutines `1.10.2`, androidx.wear.watchface `1.2.1`).
- Package root `dev.programmerq.bruneiazan`; shared module namespace `dev.programmerq.bruneiazan.shared`.
- Default enabled prayer alarms: `suboh`, `zohor`, `asar`, `maghrib`, `isyak`. All 8 schedule keys are toggleable. Custom alarms are one-off (non-repeating); an enabled custom alarm with a past time today fires tomorrow.

## Review Focus

1. **Month-boundary alarms**: tomorrow can fall in the next calendar month — `PrayerRepository.day(date)` must fetch the month containing `date`, never assume today's cache covers tomorrow. Test in Task 5.
2. **After-Isyak rollover**: `nextPrayer` after the last schedule item must return tomorrow's first item flagged as tomorrow — tested in Task 4.
3. **Cold start with no cache + no network**: UI must render an empty/error state instead of crashing; alarms simply aren't scheduled. Manual check in Task 18.
4. **Notification permission denied (phone)**: the alarm notification silently no-ops; app must still not crash and should prompt for permission at onboarding. Manual check Task 18.
5. **Settings DataMap round-trip**: any settings change on one device must apply on the other and trigger alarm reschedule; serialization via one JSON field (not nested DataMaps) keeps parity guaranteed by parser tests in Task 6.

---

### Task 1: `:shared` module skeleton + version-catalog deps

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `shared/build.gradle.kts`
- Create: `shared/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `:shared` library module, namespace `dev.programmerq.bruneiazan.shared`, that `:mobile` and `:wear` can depend on; catalog entries `okhttp`, `kotlinx-serialization-json`, `datastore-preferences`, `kotlinx-coroutines-android`, `kotlinx-coroutines-core`, `androidx-lifecycle-runtime-compose`, `androidx-wear-watchface`; plugins `android-library`, `kotlin-serialization`.

- [ ] **Step 1: Extend `gradle/libs.versions.toml`**

Add versions `okhttp = "4.12.0"`, `kotlinxSerialization = "1.8.1"`, `datastorePreferences = "1.1.7"`, `kotlinxCoroutines = "1.10.2"`, `lifecycleRuntimeCompose = "2.6.1"`, `wearWatchface = "1.2.1"`. Add libraries: `okhttp`, `kotlinx-serialization-json`, `androidx-datastore-preferences`, `kotlinx-coroutines-core`, `kotlinx-coroutines-android`, `androidx-lifecycle-runtime-compose`, `androidx-wear-watchface` (group `androidx.wear.watchface`, name `watchface`). Add plugins: `android-library = { id = "com.android.library", version.ref = "agp" }`, `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`.

- [ ] **Step 2: Create the module**

`settings.gradle.kts`: add `include(":shared")`. Write `shared/build.gradle.kts` mirroring `mobile/build.gradle.kts` but with `alias(libs.plugins.android.library)` + `alias(libs.plugins.kotlin.serialization)` (no `kotlin.compose`, no `defaultConfig.applicationId`), namespace `dev.programmerq.bruneiazan.shared`, minSdk 35. Dependencies: okhttp, kotlinx-serialization-json, datastore-preferences, coroutines core+android, junit (test). Manifest: `<uses-permission android:name="android.permission.INTERNET"/>` only.

- [ ] **Step 3: Verify build**

Run: `./gradlew :shared:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

`git add -A && git commit -m "chore: add shared library module"` — note: repo is not yet git-initialized; `git init` first per spec workflow, single initial commit of existing template files, then this commit.

### Task 2: Models + JSON parser (TDD)

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/api/Models.kt`
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/api/PrayerJson.kt`
- Test: `shared/src/test/java/dev/programmerq/bruneiazan/shared/api/PrayerJsonTest.kt`

**Interfaces:**
- Produces:
  - `data class ScheduleItem(key: String, name: String, time24: String, time12: String, type: String)`
  - `data class PrayerDay(day: String, dateIso: String, dateMalay: String, hijri: String, schedule: List<ScheduleItem>)` (timings map not needed — schedule carries everything)
  - `data class Meta(district: String, offsetMinutes: Int, qiblaBearing: String)` — district/qibla surfaced in UI
  - `sealed interface ApiResult<out T> { data class Ok<T>(value: T); data class Err(code: Int, message: String) }`
  - `object PrayerJson { fun parseDay(body: String): ApiResult<Pair<Meta, PrayerDay>>; fun parseMonth(body: String): ApiResult<List<PrayerDay>> }` — parses `data` as object (day) or array (month); `status:"error"` → `Err`.

- [ ] **Step 1: Write failing tests**

Fixture: embed the daily JSON from the spec verbatim (trim `meta` to needed fields) and a 2-day month array fixture; assert `dateIso=="2026-09-28"`, `schedule.size==8`, `schedule[0]` = `("imsak","Imsak","04:41","4:41 AM","TAHAJJUD")`, `meta.district=="Daerah Brunei-Muara"`; error body `{"status":"error","code":404,"message":"..."}` → `Err(404, …)`; month returns `List<PrayerDay>` with `size==2`.

- [ ] **Step 2: Run tests, verify failure** — `./gradlew :shared:testDebugUnitTest --tests "*PrayerJsonTest*"` fails (classes missing).

- [ ] **Step 3: Implement `Models.kt` + `PrayerJson.kt`**

`@Serializable` models matching the wire names (`@SerialName("time_24")` etc.). `PrayerJson` uses a lenient `Json { ignoreUnknownKeys = true }`; branch on whether `data` is `JsonObject` vs `JsonArray` via `parseToJsonElement` first so one parser serves both endpoints.

- [ ] **Step 4: Run tests, verify pass.**

- [ ] **Step 5: Commit** — `feat: shared prayer-times models and parser`

### Task 3: `WaktuApi` network client

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/api/WaktuApi.kt`
- Test: `shared/src/test/java/dev/programmerq/bruneiazan/shared/api/WaktuApiTest.kt`

**Interfaces:**
- Consumes: `PrayerJson`, `ApiResult` (Task 2).
- Produces:
  - `object WaktuApi { suspend fun fetchDay(dateIso: String?, district: String): ApiResult<PrayerDay>; suspend fun fetchMonth(year: Int, month: Int, district: String): ApiResult<List<PrayerDay>> }` — `dateIso=null` sends `date=today`; month sends `MM/YYYY` (verified quirk).
  - `internal fun monthParam(year: Int, month: Int): String` → `"MM/YYYY"`.

- [ ] **Step 1: Write failing test** — assert `monthParam(2026,10)=="10/2026"` and `monthParam(2026,1)=="01/2026"` (URL-encoding is OkHttp's job via `HttpUrl.Builder`).

- [ ] **Step 2: Verify failure.**

- [ ] **Step 3: Implement** — single shared `OkHttpClient` (10s timeouts); `HttpUrl.get("https://waktu.co.bn/api").newBuilder()` with query params; `suspend` via `withContext(Dispatchers.IO)`; non-2xx or `status:"error"` body → `Err`; `fetchDay` delegates to `PrayerJson.parseDay` → `PrayerDay` (drop Meta except district surfaced separately — keep `Meta` out of `fetchDay` return; callers don't need it).

- [ ] **Step 4: Verify pass.**

- [ ] **Step 5: Commit** — `feat: waktu.co.bn api client`

### Task 4: `nextPrayer` + `AlarmPlanner` (TDD)

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/schedule/NextPrayer.kt`
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/alarms/AlarmPlanner.kt`
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/settings/Settings.kt`
- Test: `shared/src/test/java/dev/programmerq/bruneiazan/shared/NextPrayerTest.kt`, `…/AlarmPlannerTest.kt`

**Interfaces:**
- Produces:
  - `data class NextPrayer(item: ScheduleItem, dateIso: String, isTomorrow: Boolean)`
  - `fun nextPrayer(today: PrayerDay, tomorrow: PrayerDay?, now: LocalTime): NextPrayer?` — first item with `time24 > now`, else tomorrow's first item (null if `tomorrow==null`).
  - `enum class District(val apiKey: String, val label: String)` — 4 entries. `enum class DarkMode { LIGHT, DARK }`, `enum class AppFont { SANS, SERIF, MONO }`, `enum class AlarmSound { AZAN, BEEP }`.
  - `data class CustomAlarm(id: String, hour: Int, minute: Int, label: String, enabled: Boolean)`
  - `data class Settings(district: District, darkMode: DarkMode, font: AppFont, alarmSound: AlarmSound, vibration: Boolean, prayerAlarms: Map<String,Boolean>, customAlarms: List<CustomAlarm>)` + `companion object { val DEFAULT }` — defaults: brunei-muara, LIGHT, SANS, AZAN, vibration=true, 5 fardhu enabled.
  - `data class AlarmInstance(key: String, label: String, triggerAtMillis: Long, isCustom: Boolean)`
  - `object AlarmPlanner { fun upcomingAlarms(settings: Settings, today: PrayerDay, tomorrow: PrayerDay?, now: ZonedDateTime): List<AlarmInstance> }` — enabled prayer items + enabled custom alarms, trigger at `LocalTime` on today/tomorrow in `now.zone`, only future instants, sorted; take `first()` = next registration.

- [ ] **Step 1: Write failing tests**

`nextPrayer`: now=03:00 → imsak/today; now=13:00 → asar; now=20:00 (after isyak) → tomorrow imsak `isTomorrow=true`; tomorrow null → null. `AlarmPlanner`: all toggles off → empty; defaults at 13:00 → asar first then maghrib/isyak then tomorrow imsak? — no: tomorrow's items only included for prayers whose time is in the future *of that day*; assert ordering across day boundary; custom alarm 09:00 enabled at 13:00 → appears with tomorrow's date; disabled custom → absent; duplicate times collapse (keep prayer item precedence).

- [ ] **Step 2: Verify failure.**

- [ ] **Step 3: Implement** — pure Kotlin + `java.time`; parse `time24` via `LocalTime.parse`. Tomorrow instances get `date=tomorrow.dateIso` when included; a custom alarm time already past → schedule on tomorrow's date using `now.toLocalDate().plusDays(1)` (custom alarms don't need tomorrow's PrayerDay — they exist regardless of API data).

- [ ] **Step 4: Verify pass.**

- [ ] **Step 5: Commit** — `feat: next-prayer and alarm planning logic`

### Task 5: `MonthCache` + `PrayerRepository` (TDD)

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/schedule/MonthCache.kt`
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/schedule/PrayerRepository.kt`
- Test: `shared/src/test/java/dev/programmerq/bruneiazan/shared/MonthCacheTest.kt`

**Interfaces:**
- Consumes: `WaktuApi`, `PrayerJson`, `PrayerDay`, `ApiResult`.
- Produces:
  - `class MonthCache(dir: File)` — `fun get(district: String, yearMonth: YearMonth): List<PrayerDay>?` reads `dir/{district}-{YYYY-MM}.json` → `PrayerJson.parseMonth`-equivalent list (cache stores the already-parsed list re-serialized as JSON of `PrayerDay`); `fun put(district, yearMonth, days)`; `fun day(district, dateIso): PrayerDay?` convenience.
  - `class PrayerRepository(cache: MonthCache, api: WaktuApi) { suspend fun day(date: LocalDate, district: String): PrayerDay?; suspend fun refreshMonth(date: LocalDate, district: String): ApiResult<*> }` — cache hit → return; miss → `fetchMonth(date.year, date.monthValue)` → `put` → lookup `dateIso` in result (result may not contain every day — return null with no exception).

- [ ] **Step 1: Write failing tests** — temp-dir cache: `put` then `get`/`day` round-trips a `PrayerDay`; missing file → null; month-boundary `day(LocalDate of next month)` consults the correct file name; repository with a fake `api` (make `WaktuApi` an `interface`/constructor param — declare `fun interface DayFetcher { suspend fun fetchMonth(y,m,d): ApiResult<List<PrayerDay>> }` so tests inject fakes without network).

- [ ] **Step 2: Verify failure.**

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Verify pass.**

- [ ] **Step 5: Commit** — `feat: month cache and prayer repository`

### Task 6: `SettingsStore` + settings JSON codec

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/settings/SettingsCodec.kt`
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/settings/SettingsStore.kt`
- Test: `shared/src/test/java/dev/programmerq/bruneiazan/shared/SettingsCodecTest.kt`

**Interfaces:**
- Consumes: `Settings`, `District`, `DarkMode`, `AppFont`, `AlarmSound`, `CustomAlarm` (Task 4).
- Produces:
  - `object SettingsCodec { fun encode(s: Settings): String; fun decode(json: String): Settings }` — one JSON string; used by DataStore AND the Data Layer (single source → round-trip parity).
  - `class SettingsStore(context: Context)` — `preferencesDataStore(name="azan_settings")`; `val settings: Flow<Settings>`; `suspend fun update(transform: (Settings) -> Settings)`; `suspend fun apply(json: String)` (from watch/phone sync — writes decoded settings); convenience `suspend fun setDistrict(d)` etc. as thin `update` wrappers.

- [ ] **Step 1: Write failing tests** — `decode(encode(DEFAULT)) == DEFAULT`; customAlarms + prayerAlarms maps survive round-trip; unknown keys ignored; missing fields → defaults.

- [ ] **Step 2: Verify failure.**

- [ ] **Step 3: Implement** — `@Serializable SettingsDto` mirroring `Settings` (enums by name); DataStore stores a single string preference `settings_json`.

- [ ] **Step 4: Verify pass.**

- [ ] **Step 5: Commit** — `feat: settings store and codec`

### Task 7: `AlarmScheduler` helper + sound resources

**Files:**
- Create: `shared/src/main/java/dev/programmerq/bruneiazan/shared/alarms/AlarmScheduler.kt`
- Create: `shared/src/main/res/raw/azan_placeholder.wav`, `shared/src/main/res/raw/beep.wav` (generated)

**Interfaces:**
- Produces:
  - `object AlarmScheduler { fun registerNext(context: Context, receiverClass: Class<*>, alarm: AlarmInstance); fun cancel(context: Context, receiverClass: Class<*>) }` — wraps `AlarmManager.setAlarmClock(AlarmClockInfo(triggerAt, appOpenPI), receiverPI)`; `receiverPI` is a `getBroadcast` with explicit component + extras `key`,`label`,`isCustom`; app-open PI launches the package's launcher activity (`packageManager.getLaunchIntentForPackage`).
  - `const val EXTRA_KEY/EXTRA_LABEL/EXTRA_CUSTOM` names.

- [ ] **Step 1: Implement `AlarmScheduler`** (no unit test — thin AlarmManager wrapper; verified on emulator Task 18 via `dumpsys alarm`).

- [ ] **Step 2: Generate WAVs** — small Python script (run once, not committed): 16-bit PCM mono 22050Hz; `azan_placeholder.wav` ≈ 12s melodic sine sequence (e.g., notes A4-C5-E5-D5-C5-A4 phrasing with gentle vibrato + fade envelopes); `beep.wav` ≈ 3×200ms 880Hz beeps. Verify both decode (file size, `python -c "import wave;…"`).

- [ ] **Step 3: Verify build** — `./gradlew :shared:assembleDebug` still green.

- [ ] **Step 4: Commit** — `feat: alarm scheduler and placeholder sounds`

### Task 8: Mobile — paper theme + app shell

**Files:**
- Modify: `mobile/src/main/java/dev/programmerq/bruneiazan/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- Modify: `mobile/build.gradle.kts` (deps: `project(":shared")`, datastore implicit, `androidx-lifecycle-runtime-compose`)
- Modify: `mobile/src/main/java/dev/programmerq/bruneiazan/MainActivity.kt`
- Create: `mobile/src/main/java/dev/programmerq/bruneiazan/AppState.kt`

**Interfaces:**
- Consumes: `SettingsStore`, `PrayerRepository`, `Settings` (Tasks 5–6).
- Produces:
  - `BruneiAzanTheme(dark: Boolean, font: AppFont, content)` — paper colors: light = `#FAFAF7` bg / `#111111` ink, dark = `#0D0D0D`/`#EDEDE8`; `FontFamily` mapped SANS→Default SERIF→Serif MONO→Monospace; all dividers `1.dp` hairline.
  - `class AppState(context)` — holds `SettingsStore`, `PrayerRepository(MonthCache(File(context.filesDir,"months")), WaktuApi)`, exposes `settings: Flow<Settings>`; constructed in `MainActivity` and passed to screens. Bottom-nav scaffold with three tabs: `Today`, `Alarms`, `Settings` (text-only tabs, paper style).

- [ ] **Step 1: Update theme files + MainActivity skeleton** — replace Material3 dynamic-color theme with fixed paper palette; `MainActivity` creates `AppState`, collects `settings`, applies `BruneiAzanTheme(dark = s.darkMode==DARK, font = s.font)`, shows scaffold w/ empty tab content stubs.

- [ ] **Step 2: Verify build** — `./gradlew :mobile:assembleDebug`.

- [ ] **Step 3: Commit** — `feat: paper theme and mobile app shell`

### Task 9: Mobile — Today screen

**Files:**
- Create: `mobile/src/main/java/dev/programmerq/bruneiazan/today/TodayScreen.kt`
- Modify: `MainActivity.kt` (wire tab)

**Interfaces:**
- Consumes: `AppState`, `nextPrayer`, `PrayerDay`, `Settings`.
- Produces: `@Composable fun TodayScreen(app: AppState, settings: Settings)` — loads `repository.day(today)` + `repository.day(tomorrow)` on district/date change (`LaunchedEffect`), state: loading/error(empty cache)/loaded; UI: Malay date + Hijri, district label, next-prayer banner ("Zohor 12:06 — in 1h 25m" via `nextPrayer`), 8-row list (name left, `time_24` right, hairline dividers, next row emphasized w/ slightly heavier weight — no color), pull-to-refresh optional (skip — simple refresh button).

- [ ] **Step 1: Implement screen + wire tab.**

- [ ] **Step 2: Verify** — build + manual check deferred to Task 18; `@Preview` with fake `PrayerDay` renders.

- [ ] **Step 3: Commit** — `feat: today prayer-times screen`

### Task 10: Mobile — Alarms + Settings screens

**Files:**
- Create: `mobile/src/main/java/dev/programmerq/bruneiazan/alarms/AlarmsScreen.kt`
- Create: `mobile/src/main/java/dev/programmerq/bruneiazan/settings/SettingsScreen.kt`
- Modify: `MainActivity.kt`

**Interfaces:**
- Consumes: `AppState`, `Settings`, `AlarmSound`, `District`, `AppFont`, `DarkMode`, `CustomAlarm`.
- Produces:
  - `AlarmsScreen` — one toggle row per schedule key (fixed order imsak…isyak); "Custom alarms" section: `+ Add` → `TimePickerDialog` + label field; each row toggle + delete; sound section: two radio rows (Azan / Beep) with a small "preview" play (`MediaPlayer` on `R.raw.azan_placeholder`/`beep`); vibration switch.
  - `SettingsScreen` — district radio list (4), Dark/Light radio, font radio (Aa previews in each family), note "Changes sync to your watch". Writes via `app.settingsStore.update { … }`.

- [ ] **Step 1: Implement both screens.**

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: alarms and settings screens`

### Task 11: Mobile — alarm pipeline

**Files:**
- Modify: `mobile/src/main/AndroidManifest.xml`
- Create: `mobile/…/alarm/AlarmReceiver.kt`, `alarm/AlarmActivity.kt`, `alarm/BootReceiver.kt`, `alarm/Rescheduler.kt`

**Interfaces:**
- Consumes: `AlarmScheduler.registerNext/cancel`, `AlarmPlanner.upcomingAlarms`, repository+settings.
- Produces:
  - `object Rescheduler { suspend fun schedule(context) }` — loads settings + today/tomorrow days → `upcomingAlarms` → `registerNext` or `cancel`. Called: app open, alarm-fire, boot, settings change, sync receive.
  - `AlarmReceiver` — posts notification on channel `azan_alarms` (IMPORTANCE_HIGH, vibration per settings, sound = `android.resource://…/raw/{azan_placeholder|beep}`) with full-screen intent → `AlarmActivity`; then `goAsync` → `Rescheduler.schedule`.
  - `AlarmActivity` — shows label + "Dismiss", plays sound via `MediaPlayer` looping until dismissed (also covered by notification sound once), vibrates while shown; `setShowWhenLocked/setTurnScreenOn`.
  - `BootReceiver` (RECEIVE_BOOT_COMPLETED, enabled) → `Rescheduler`.
  - Manifest: `POST_NOTIFICATIONS`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`; MainActivity requests `POST_NOTIFICATIONS` on first launch.

- [ ] **Step 1: Implement all pieces + manifest.**

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: mobile alarm pipeline`

### Task 12: Mobile — Data Layer sync

**Files:**
- Create: `mobile/…/sync/SettingsSync.kt`, `sync/SyncListenerService.kt`
- Modify: `AndroidManifest.xml`, `AppState.kt` (push on settings change)

**Interfaces:**
- Consumes: `SettingsCodec`.
- Produces:
  - `object SettingsSync { suspend fun push(context, settings: Settings) }` — `DataClient.putDataItem` path `/settings`, item field `json` = `SettingsCodec.encode` + `ts` = epoch.
  - `SyncListenerService : WearableListenerService` — `DATA_CHANGED` filter `wear://*/settings` → `apply` via `SettingsStore.apply(json)` → `Rescheduler.schedule`.
  - `AppState` collects `settings` flow → debounce 500ms → `SettingsSync.push` (skip echoes via `ts` guard).

- [ ] **Step 1: Implement.**

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: settings sync to watch`

### Task 13: Wear — app screens

**Files:**
- Modify: `wear/build.gradle.kts` (+ `project(":shared")`, `androidx-wear-watchface`, datastore implicit)
- Rewrite: `wear/src/main/java/dev/programmerq/bruneiazan/presentation/MainActivity.kt`
- Create: `wear/…/presentation/PrayerListScreen.kt`, `AlarmsScreen.kt`, `SettingsScreen.kt`, `WearAppState.kt`
- Modify: `wear/…/presentation/theme/Theme.kt` (paper tokens, font param)

**Interfaces:**
- Produces: `WearAppState` mirroring `AppState` (SettingsStore + repository); three screens via simple state-based navigation (no nav lib — a `var screen by remember`); `PrayerListScreen` = TransformingLazyColumn of today's schedule + next highlighted; `AlarmsScreen`/`SettingsScreen` = wear-material3 `SwitchButton`/radio rows with same settings model.

- [ ] **Step 1: Implement theme + state + three screens.**

- [ ] **Step 2: Verify build** — `./gradlew :wear:assembleDebug`.

- [ ] **Step 3: Commit** — `feat: wear app screens`

### Task 14: Wear — alarm pipeline + Data Layer listener

**Files:**
- Modify: `wear/src/main/AndroidManifest.xml`
- Create: `wear/…/alarm/AlarmReceiver.kt`, `alarm/AlarmActivity.kt`, `alarm/BootReceiver.kt`, `alarm/Rescheduler.kt`, `sync/SyncListenerService.kt`, `sync/SettingsSync.kt` (mirrors of Task 11–12 wear-side)

**Interfaces:**
- Same contracts as Task 11/12 but wear-side: `AlarmActivity` full-screen dismiss edge-button, MediaPlayer + `Vibrator`; notification channel `azan_alarms` with FSI; `SyncListenerService` applies `/settings` → `SettingsStore.apply` → `Rescheduler.schedule`; local settings changes also `SettingsSync.push`.

- [ ] **Step 1: Implement + manifest entries** (`VIBRATE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, listener service intent-filter).

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: wear alarm pipeline and sync`

### Task 15: Wear — `AzanWatchFaceService`

**Files:**
- Create: `wear/…/watchface/AzanWatchFaceService.kt`, `watchface/PaperRenderer.kt`, `res/xml/watch_face.xml`, `res/drawable/watchface_preview.png` (generated: simple paper-style time+next-prayer render)
- Modify: `wear/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `SettingsStore`, `PrayerRepository`/`MonthCache`, `nextPrayer`.
- Produces:
  - `AzanWatchFaceService : WatchFaceService()` — `createWatchFace` returns `WatchFace(WatchFaceStyle, renderer)`; `UserStyleSchema()` empty; `WatchFaceType.DIGITAL`.
  - `PaperRenderer : Renderer.CanvasRenderer2` — interactive: bg per `darkMode`, large time `HH:mm`, next prayer `name + time` + countdown line, `date_malay` + `hijri` small; ambient: black bg, thin white strokes, time + next prayer only. Reads a `AtomicReference<Settings>` + `AtomicReference<NextPrayer?>` kept fresh by a service coroutine collecting `settings` flow and re-evaluating `nextPrayer` each minute boundary.
  - Manifest: service `BIND_WATCH_FACE` permission + wallpaper `watch_face.xml` meta-data + `com.google.android.wearable.watchface.preview(_circular)` → `watchface_preview.png` + `<meta-data android:name="com.google.android.wearable.watchface.wearableConfigurationService">` omitted (no config activity).

- [ ] **Step 1: Implement service + renderer + manifest + preview PNG** (generate via Python PIL or reuse a styled bitmap).

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: paper watch face`

### Task 16: Wear — complication + tile

**Files:**
- Rewrite: `wear/…/complication/MainComplicationService.kt`, `wear/…/tile/MainTileService.kt`

**Interfaces:**
- Consumes: `MonthCache`, `nextPrayer`.
- Produces:
  - Complication `onComplicationRequest` → `ShortTextComplicationData` text `"Suboh 04:47"`, contentDescription = next prayer name; update period 900s; also refreshed via `ComplicationDataSourceUpdateRequester` after alarm fires / settings sync.
  - Tile: `timeline` single entry — protolayout-material3 column: next prayer prominent + up to 4 upcoming rows, black-on-white text style; `freshnessIntervalMillis = 30min`.

- [ ] **Step 1: Implement both.**

- [ ] **Step 2: Verify build.**

- [ ] **Step 3: Commit** — `feat: complication and tile`

### Task 17: Full-suite verification + fixes

- [ ] **Step 1:** `./gradlew :shared:testDebugUnitTest :mobile:assembleDebug :wear:assembleDebug` — all green.
- [ ] **Step 2:** Commit.

### Task 18: Emulator end-to-end test

- [ ] **Step 1: Boot emulators** — `emulator -avd Pixel_9_API_36` and `emulator -avd Wear_OS_Small_Round` (background); `adb wait-for-device` both.
- [ ] **Step 2: Install + launch** — `adb -s <serial> install -r mobile/build/outputs/apk/debug/mobile-debug.apk` (+ wear apk on the wear serial); `am start` both `MainActivity`s.
- [ ] **Step 3: Verify screens** — `adb exec-out screencap -p > shot.png` each device; Today screen shows real times (emulator has network).
- [ ] **Step 4: Verify alarms** — enable a prayer alarm in UI (or via default settings already on), `adb shell dumpsys alarm | grep -A2 bruneiazan` shows the `setAlarmClock` entry; optional: set a custom alarm 2 min out, capture it firing (notification + AlarmActivity).
- [ ] **Step 5: Verify watch face** — on wear emulator `adb shell` dumpsys or picker screenshot confirms `AzanWatchFaceService` appears in the watch-face picker.
- [ ] **Step 6: Report results** — screenshots + findings to user; any failures triaged and fixed before claiming done.

---

## Self-review notes (already applied)

- Month API param `MM/YYYY` and partial-month coverage are pinned in Global Constraints + Task 3/5 tests — spec doc's `YYYY-MM` assumption corrected by live probing.
- Notification-permission denial and empty-cache cold start are manual-verification items in Task 18 (can't be unit-tested).
- `SettingsCodec` JSON-string approach guarantees phone↔watch parity through Task 6 tests.
