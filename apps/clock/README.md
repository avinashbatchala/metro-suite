# Clock

**Package:** `com.metro.clock`  
**Tier:** 2

## Status

Implemented — buildable Android app with four WP8.1 pivots: **alarms**, **world clock**, **timer**,
**stopwatch**. Alarms use exact `AlarmManager.setAlarmClock` scheduling; timers/stopwatches are
multi-instance and derive their time from monotonic timestamps. Live Tiles export structured temporal
state so the launcher ticks locally (no per-second broadcasts).

## App role

Recreates the WP8.1 **Clock** experience. Utility-focused, typography-led, flat Metro — no Material
pickers/chips/cards. The stock WP8.1 Alarms app was simpler than the later Windows 10 Mobile app;
this app follows WP8.1 layout with the repo's four-pivot product spec.

## Architecture

```
apps/clock/app/src/main/java/com/metro/clock/
├── MainActivity.kt              # launcher + metro://clock/* deep links
├── ClockRestoreReceiver.kt      # BOOT_COMPLETED / TIME_SET / TIMEZONE_CHANGED / package update
├── data/                        # Room: AlarmEntity, TimerEntity, StopwatchEntity, lap, world city
├── alarms/                      # AlarmSchedule (pure), AlarmScheduler, AlarmReceiver,
│                                #   AlarmNotifications, AlarmRingingActivity, AlarmRepository
├── timers/                      # TimerLogic (pure), TimerScheduler, TimerReceiver,
│                                #   TimerNotifications, TimerRepository
├── stopwatch/                   # StopwatchLogic (pure), StopwatchRepository
├── worldclock/                  # WorldClockRepository (uses shared MetroWorldClockCatalog)
├── tiles/                       # ClockTileProvider, ClockTileDataSource, ClockPeekLogic (pure),
│                                #   ClockTileActions, ClockTileActionReceiver, ClockTileRefresh
└── ui/                          # ClockState, ClockShell, screens, Metro wheel time/duration picker
```

- **Persistence:** Room (`metro_clock.db`). State transitions + timestamps only — never ticking values.
  Timers store `duration`, `remainingWhenPaused`, `targetElapsedRealtime`, `targetEpochMillis`, `state`.
  Stopwatches store `accumulatedElapsedMillis`, `runningSinceElapsedRealtime`, `runningSinceEpochMillis`.
- **Timers:** multiple concurrent; each schedules its own completion alarm
  (`setExactAndAllowWhileIdle`, `ELAPSED_REALTIME_WAKEUP`, stable PendingIntent per row id). Remaining
  is computed from `targetElapsedRealtime - SystemClock.elapsedRealtime()`; `targetEpochMillis` is the
  reboot recovery aid.
- **Stopwatches:** multiple concurrent; laps persisted (`lapNumber`, `lapDurationMillis`,
  `totalDurationMillis`). Elapsed = `accumulated + (now - runningSince)`.
- **World clock:** `ZoneId` from the shared `MetroWorldClockCatalog`; DST handled by the platform.

## Permissions & scheduling

- `USE_EXACT_ALARM` (auto-granted for genuine alarm-clock apps on API 33+), `SCHEDULE_EXACT_ALARM`
  (maxSdk 32), `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`,
  `WAKE_LOCK`. No Google Play Services / Firebase.
- Alarms: `AlarmManager.setAlarmClock()` (exempt from Doze, shown in the status bar), recomputed to the
  **next concrete occurrence** (never a platform repeating alarm) on every change, boot, time/timezone
  change, and package update.
- Ringing: high-priority alarm channel (no channel sound — the ringing activity loops the alarm tone via
  `Ringtone` + vibration) with a full-screen intent; dismiss + snooze. `WRITE`-nothing; no permanent
  foreground service.
- `POST_NOTIFICATIONS` is requested when the user first creates an alarm/timer/stopwatch.

## Reboot semantics

- Alarms: every enabled alarm is rescheduled to its next occurrence; disabled ones cancelled.
- Timers: running timers are resolved against `targetEpochMillis`; expired ones become `FINISHED` and
  notify, future ones are rescheduled.
- Stopwatches: a running stopwatch keeps running and absorbs the wall-clock gap since
  `runningSinceEpochMillis` (documented choice).

## Live Tiles & deep links

- Authority `com.metro.clock.tiles`. Tile ids: `primary`, `alarm:<id>`, `timer:<id>`,
  `stopwatch:<id>`, `world:<cityId>`.
- The **primary** tile cycles deterministic peeks (see `ClockPeekLogic`): finished timers → running
  timers (soonest first) → paused timers → running stopwatches → paused stopwatches → next enabled
  alarm; capped at 6 with a `+N more` face. Idle → next-alarm face.
- Timer/stopwatch/world peeks carry `MetroTilePeek.temporal` (structured state); the launcher ticks them
  locally. World Clock tile payloads are **not** refreshed every minute.
- `MetroTileUpdates` is called only on meaningful transitions (enable/edit/create/start/pause/resume/
  finish/reset/delete/rename, city change, boot recovery).
- Tapping a Clock tile opens the Clock app (the launcher does this from the foreground via the
  peek's `packageName`); the `alarm:<id>` tile deep-links to that alarm's edit page.
- Deep links: `metro://clock/alarms`, `/alarm/<id>`, `/timers`, `/timer/<id>`, `/stopwatch/`,
  `/stopwatch/<id>`, `/world`.
- Pinning: World Clock city rows pin `world:<cityId>` via `MetroIntents.requestPinTile`.

## Sounds

- **Alarm:** a ringing alarm plays its per-alarm `soundUri` if set, else the user's actual system
  alarm default (`RingtoneManager.getActualDefaultRingtoneUri(TYPE_ALARM)`) — set from Settings →
  ringtones + sounds (Metro `ALARM`) or Android. Clock never plays call audio.
- **Timer:** the timer-completion notification channel adopts the Metro `TIMER` sound on **first
  creation** via `MetroNotificationChannels.applyInitialSound` (existing channels are user-owned).
  If the pack is not installed, the channel keeps its default. Timer uses alarm audio attributes.
- Per-alarm sound selection UI is out of scope for v1 (`AlarmEntity.soundUri` is already nullable so
  it can be added later without duplicating audio).

## Tests

- `AlarmScheduleTest` — one-time/daily/weekday/weekend/specific, disabled, DST spring & autumn,
  next-earliest, repeat summaries.
- `TimerLogicTest` — running/paused/ready/finished, expiry, zero clamp, display rounding.
- `StopwatchLogicTest` — running/paused, large durations, precise formatting.
- `ClockPeekLogicTest` — deterministic ordering, soonest-completion sort, overflow, active detection.
- SDK tests (`metro-system-sdk`): `MetroTileTemporalRenderTest`, `MetroWorldClockCatalogTest`,
  temporal/`world_clock` codec round-trips, 12/24h `MetroClockFace`.

## Commands

```bash
cd apps/clock
./gradlew :app:assembleDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh clock
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Background alarm/timer reliability | OEM/GrapheneOS background restrictions | Use `setAlarmClock` + `USE_EXACT_ALARM`; recompute on boot/time change; document device quirks |
| One global stopwatch in some OEM apps | — | Multiple independent stopwatches per product spec |
| Windows 10 Mobile Alarms & Clock richness | Wrong visual target | Implement the four-pivot feature set in WP8.1 Metro language |

## Known limitations

- Full-screen alarm UI depends on `USE_FULL_SCREEN_INTENT`; when unavailable it degrades to the
  strongest alarm notification.
- Goldens not captured (device captures live in this session's notes); screenshot-diff is skipped.
