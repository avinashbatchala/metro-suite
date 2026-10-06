# Agent instructions — Clock (`com.metro.clock`)

**Tier 2** | Alarms, world clock, timer, stopwatch pivots. Reference: `references/images/`.

Four pivot items max. World clock uses list with city names. Alarm uses `MetroToggleSwitch`.

Data: Room (`data/`), state transitions + timestamps only — never ticking values. Alarms use
`AlarmManager.setAlarmClock` (`USE_EXACT_ALARM`); recompute next occurrence on change/boot/time change.
Timers/stopwatches are multi-instance and derive time from `SystemClock.elapsedRealtime()`.

Tiles: authority `com.metro.clock.tiles`; ids `primary` / `alarm:<id>` / `timer:<id>` /
`stopwatch:<id>` / `world:<cityId>`. Timer/stopwatch/world peeks carry `MetroTilePeek.temporal` so the
launcher ticks locally — **never broadcast a tile update per second**. Peeks carry
`packageName = com.metro.clock` so tapping a tile opens the app (launcher-driven); `alarm:<id>` tile
deep-links to the alarm. Primary peek ordering lives in `tiles/ClockPeekLogic` (pure, tested). Shared
city catalog: `MetroWorldClockCatalog` (SDK).

Verify: `../../scripts/verify-app.sh clock`
