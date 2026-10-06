# Clock — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Target: **Windows Phone 8.1 presentation, Android/GrapheneOS engine.** Flat Metro — no cards, chips,
FAB, bottom sheets, or Material pickers. Not Windows 10 Mobile *Alarms & Clock*.

## Fidelity boundary

```
A — AUTHENTIC WP8.1 (first-party "Alarms")
    alarm list, alarm create/edit, alarm ringing, next-alarm Live Tile
B — PERIOD-CORRECT EXTENSIONS (later/other WP8.x Metro apps)
    world clock, timer, stopwatch, their secondary Live Tiles
C — METROSUITE EXTENSIONS
    multiple concurrent timers, multiple stopwatches, timer/stopwatch peek cycling,
    3-city Widgets World Clock
```

Category A matches WP8.1 as closely as Android permits. B/C use WP8.x Metro grammar, **not** Windows
10 Mobile. World clock / timer / stopwatch were **not** stock WP8.1 and are never described as such.

## App shell

- `MetroPivot` with four titles: `alarms` · `world clock` · `timer` · `stopwatch` (lowercase, large
  Light). One application identity: the large `clock` title (`MetroAppTitle`); no second "metro clock".
- Bottom `MetroAppBar` with a single circular **icon** command per pivot: `+` (`new` for
  alarms/timer/stopwatch, `add` for world clock). Labels appear on bar expansion.
- Subpages use `MetroSubpageHost` (page-pivot enter/exit). **No redundant top Back circles** — the
  system Back / navbar returns.
- `rememberSystem24Hour()` reacts to configuration (12/24h) changes; `rememberMinuteTick()` ticks the
  world clock on minute boundaries.
- Deep links: `metro://clock/{alarms|world|timer|stopwatch}` and `/alarm/<id>`, `/timer/<id>`,
  `/stopwatch/<id>`, `/world/<cityId>`.

## Category A — Alarms

### Alarms list

- Chronological by time of day (repeating and one-time together). Row: large time (`7:00 AM`),
  optional name, localized repeat summary; `MetroToggleSwitch` on the right (no On/Off text).
- Toggle enables/disables and reschedules immediately. Tap row → editor. App-bar `+` → new alarm.
- Empty state `no alarms`. Flat black.

### Alarm editor (authentic field form)

```
ALARMS
new | edit

Time        7:00 AM        → time picker
Repeats     only once      → repeat picker
Sound       Metro Dawn     → sound picker
Name        [ Morning    ]
Snooze time 10 minutes     → snooze picker
```

- **No permanently-visible time wheel** and **no large body Save/Delete buttons**; the
  ApplicationBar owns them: `✓ save`, plus `trash delete` when editing an existing alarm.
- **Time** opens a WP8.1 scrolling time selector (12/24h per system). **Repeats** opens a picker:
  `only once`, `every day`, `weekdays`, `weekends`, and **custom days** (locale-aware full day names;
  bits remain Mon=bit0…Sun=bit6). **Sound** opens a Metro picker (default + system alarm tones) with
  preview; the selection is stored in `AlarmEntity.soundUri`. **Snooze time** offers the WP8.1 Update
  values. **Vibrate** is not a primary field (kept in the DB for compatibility; default/system behavior).
- Save persists to Room, reschedules the next concrete occurrence, and returns to the list without a
  redundant toast. Blank name falls back to `Alarm`.

### Alarm ringing

Full-screen (`setShowWhenLocked` + `setTurnScreenOn`): `ALARM` overline, time (`PageTitle`), name,
the current **snooze interval**, then `snooze` / `dismiss`. Loops the per-alarm sound, else the suite
`MetroSoundRole.ALARM`, else the system alarm default. If full-screen intent is blocked, the
high-priority notification remains actionable. Back never dismisses.

## Category B/C — World clock, Timer, Stopwatch

### World clock

- List of user-chosen cities. Row: name + region (left), local time (large, right) with subtle
  `tomorrow`/`yesterday`. Locale-aware and 12/24h-aware; ticks on minute boundaries.
- **No auto-seeding** of London/New York/Tokyo. Empty state `add a city`.
- **Tap does nothing destructive** (no pin-on-tap). **Long-press** opens a Metro menu:
  `pin to start`, `move up`, `move down`, `remove`.
- Clock add-city flow: `+` → picker → **tap one city adds it and returns**.

### Timer

- List: name + countdown; visual state only (`paused` / `finished`) — no state-machine strings.
- `+` → new-timer page (duration wheels + name); **Start is an ApplicationBar command**; navigation
  is owned by the create action (no Back race). Blank names fall back to `timer`/`timer N`.
- Detail: large countdown; ApplicationBar `play/pause`, `reset`; `…` → `pin to start`, `delete`.
- Finished timers are `FINISHED` and count toward tile attention until opened/acknowledged.

### Stopwatch

- List: name + `H:MM:SS.hh`; `paused` suffix when not running.
- `+` creates a stopwatch at `00:00` **paused** (the user presses Start) with a readable name
  (`stopwatch`, `stopwatch 2`, …). No notification permission is requested for stopwatches.
- Detail: large elapsed time; ApplicationBar `start/pause`, `lap` (disabled when not running),
  `reset`; `…` → `pin to start`, `rename`, `delete`. Flat lap list.

## Live Tiles

- **Primary (`primary`)** peek-cycle priority: finished timers → running timers (soonest completion)
  → running stopwatches → next enabled alarm. **Paused/ready items do not cycle.** Cap: **6 visible
  faces total**; on overflow, **5 real faces + `+N more`**. Counter = finished timers.
- **Secondary:** `timer:<id>`, `stopwatch:<id>`, `world:<cityId>`, `alarm:<id>` carry structured
  `MetroTilePeek.temporal`; the launcher ticks locally (no per-second app broadcasts).
- Authentic (A): next active alarm. Extension (B/C): running timers/stopwatches, finished attention,
  world clock.

## Widgets World Clock (extension)

- 1–3 user-chosen cities; wide tile; shared `MetroWorldClockCatalog`; launcher-local ticking.
- **Selection enforces minimum 1 / maximum 3** — the last city cannot be removed; the Widgets
  config screen uses multi-select checkboxes (distinct from Clock's single-add flow).

## Images

| Image | Page |
|-------|------|
| _(not yet sourced — see known-gaps.md)_ | Alarms list, alarm editor, time picker, repeat picker, sound picker, alarm ringing, world clock, timer, stopwatch, tiles |

## Out of scope

Sleep/focus sessions, Cortana, cloud sync, online city lookup, weather in world clock, Windows 10
Mobile styling.
