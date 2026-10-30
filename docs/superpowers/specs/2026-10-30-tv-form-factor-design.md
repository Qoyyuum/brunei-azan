# Android TV form factor — design

Date: 2026-10-30
Status: approved (in-chat, approach A selected)

## Intent

Add a full-parity Android TV ("TvOS") version of Brunei Azan as a new `:tv`
application module, alongside `:mobile`, `:wear`, and `:watchface`. Everything
the phone app does — today's schedule, per-prayer azan alarms, custom alarms,
sound/vibration settings, district picker, theme/font — adapted for a 10-foot,
D-pad-driven UI.

## Approach

New `:tv` application module built on Compose for TV
(`androidx.tv:tv-foundation`, `androidx.tv:tv-material`). Rejected: a
leanback-enabled mobile APK (no D-pad UX) and a shared adaptive `:ui` module
(Material3 isn't TV-idiomatic; too large a refactor for the payoff).

## Module & build

- `namespace` / `applicationId` = `it.anak.bruneiazan` (same app record
  convention as `:wear`).
- `versionCode = 20001` (mobile `1`, wear `10002`); `versionName = "1.0"`.
- minSdk 35, targetSdk 37, compileSdk 37, Java 11, compose build feature.
- `include(":tv")` in `settings.gradle.kts`; `tv/.gitignore` with `/build`
  and `/release` per module convention.
- Version catalog gains `androidx-tv-foundation` and `androidx-tv-material`
  (latest stable). Other deps mirror `:mobile` minus `play-services-wearable`
  (no Wearable Data Layer on Android TV).

## App structure

```
tv/src/main/java/it/anak/bruneiazan/
  BruneiAzanTvApp.kt    Application; owns TvAppState. No startSync().
  TvAppState.kt         SettingsStore + PrayerRepository + scope.
  MainActivity.kt       POST_NOTIFICATIONS request; Rescheduler on settings
                        change; hosts TvAppShell.
  TvAppShell.kt         Top TabRow (Today / Alarms / Settings).
  today/TvTodayScreen.kt    Malay date, Hijri, district, next-prayer
                            countdown banner, full schedule list.
  alarms/TvAlarmsScreen.kt  Prayer-alarm switches, custom alarms (add via
                            D-pad time-wheel dialog, delete), sound radios
                            with Play preview, 5s test, vibration switch.
  settings/TvSettingsScreen.kt District, dark mode, font radios; credits;
                            "settings stay on this device" note.
  alarm/AlarmReceiver.kt    Notification + full-screen intent -> AlarmActivity.
  alarm/BootReceiver.kt     Reschedule on BOOT_COMPLETED.
  alarm/Rescheduler.kt      Cache-only next-alarm registration (port of
                            mobile's; TvApp references).
  alarm/AlarmActivity.kt    Full-screen alarm UI, looping MediaPlayer
                            (shared azan/beep raw res), focused Dismiss.
  ui/theme/               Paper palette + typography on
                          androidx.tv.material3.MaterialTheme.
```

## UI decisions

- Top `TabRow` (tv-material3) for 3 destinations — simpler than a drawer.
- Rows reimplemented as focusable TV rows with visible focus state
  (border/scale); `TvLazyColumn` for lists.
- Custom-alarm dialog uses hour/minute wheel columns driven by D-pad
  (Material3 `TimePicker` isn't remote-friendly); optional label keeps IME.
- Alarm path identical to mobile: notification channel + FSI launches
  AlarmActivity; azan/beep loop through TV speakers; vibration setting kept
  (no-op without hardware).
- Paper theme (light/dark ink-on-paper palettes) ported to tv-material3;
  `DarkMode.SYSTEM` follows the TV.

## Manifest

- `<uses-feature android:name="android.software.leanback" android:required="true"/>`
- `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`
- `MainActivity`: MAIN + LAUNCHER + `LEANBACK_LAUNCHER` categories.
- `android:banner` on `<application>` — new 320x180 banner drawable.
- Permissions: POST_NOTIFICATIONS, VIBRATE, USE_EXACT_ALARM,
  RECEIVE_BOOT_COMPLETED, USE_FULL_SCREEN_INTENT (same as mobile).
- Same receivers/activity registrations; no SyncListenerService.

## Data & behavior

- All logic stays in `:shared`: WaktuApi, PrayerRepository + MonthCache,
  SettingsStore (DataStore), AlarmPlanner/AlarmScheduler (TV passes its own
  AlarmReceiver class).
- Standalone: TV keeps its own DataStore; no phone<->TV sync (API absent
  on Android TV).

## Testing & verification

- Unit test for the D-pad time-wheel state (only new pure logic).
- `./gradlew test` across modules + `:tv:assembleDebug` must pass.
- README: module table, versionCode offsets, build command.

## Out of scope

- Phone<->TV settings sync (no API), TV home-screen channel, screensaver/
  ambient mode (possible follow-ups).
