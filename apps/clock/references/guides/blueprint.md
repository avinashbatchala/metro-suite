# Clock — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1** Clock. Four pivots. Black background, Noto Sans stand-in for Segoe WP,
flat Metro — no cards, chips, FAB, bottom sheets, or Material pickers.

> Historical note: the stock WP8.1 **Alarms** app was much simpler than the later Windows 10 Mobile
> *Alarms & Clock*. Do not copy the Windows 10 Mobile design. Where WP8.1 has no first-party screen
> (world clock, timer, stopwatch, pinning), follow the suite Metro conventions.

## App shell

- `MetroPivot` with four titles: `alarms` · `world clock` · `timer` · `stopwatch` (lowercase, large
  Light). Horizontal swipe moves pivots; tapping a title selects it.
- `MetroAppTitle("clock")` at the top; bottom `MetroAppBar` with a context action per pivot
  (`new` for alarms/timer/stopwatch, `add` for world clock).
- Subpages use `MetroSubpageHost` (page-pivot load/exit). Back returns to the pivot root.
- Deep links: `metro://clock/alarms|world|timer|stopwatch`, plus `/alarm/<id>`, `/timer/<id>`,
  `/stopwatch/<id>`, `/world/<cityId>`.

## Pages

### Page 1 — Alarms pivot

- **Layout:** list of alarms sorted by time. Row: time in large type (e.g. `7:00 AM`) with
  `label · repeat` in grey beneath, and a `MetroToggleSwitch` on the right (no On/Off text). Empty
  state `no alarms` centered.
- **Navigation:** tap row → Alarm edit. App bar `new` → Alarm edit (new).
- **Interactions:** toggle enable/disable schedules/cancels immediately.
- **Background:** Black.

### Page 2 — Alarm edit

- **Layout:** back circle + `alarm` page header; a WP8.1 **wheel time picker** (hour and minute
  columns, three rows visible, centre row is the selection, `AM`/`PM` to the right in 12-hour mode; a
  single 00–23 column in 24-hour mode); `label` text box; **repeat** = seven day checkboxes
  (M T W T F S S) plus `every day` / `weekdays` / `weekends` presets; `vibrate` toggle; `snooze`
  picker; `save` and (when editing) `delete`.
- **Interactions:** save persists to Room and reschedules the next concrete occurrence. Repeat
  weekdays store a bitmask (Mon = bit0).
- **Background:** Black.

### Page 3 — Alarm ringing

- **Layout:** full-screen; `alarm` overline, time in PageTitle, label, and `snooze` / `dismiss`
  buttons. Shown with `setShowWhenLocked` + `setTurnScreenOn`.
- **Interactions:** loops the alarm ringtone and vibrates; snooze re-arms after the alarm's snooze
  interval; dismiss stops and (one-time alarms) disables.
- **Degradation:** if full-screen intent is blocked, the high-priority notification remains actionable.

### Page 4 — World clock pivot

- **Layout:** list of selected cities. Row: city name + region (left), current local time (large,
  right) with a subtle `tomorrow` / `yesterday` when on a different calendar day. Rows tick locally.
- **Navigation:** app bar `add` → City picker. Tap a row → pin that city to Start. `remove` removes it.
- **Background:** Black.

### Page 5 — City picker

- **Layout:** back circle + `choose a city` header, `search cities` box (matches city, country, zone,
  alias), alphabetical list; each row shows name + country/region with a `MetroCheckBox`.
- **Interactions:** tap/checkbox adds or removes; duplicates prevented. No upper limit in Clock
  (the Widgets widget limits to 3).
- **Background:** Black.

### Page 6 — Timer pivot

- **Layout:** list of timers (multiple concurrent). Row: label; `remaining · state` beneath; a
  trailing `start`/`pause`/`resume` action. Remaining counts down locally.
- **Navigation:** app bar `new` → Timer create. Tap row → Timer detail.
- **Background:** Black.

### Page 7 — Timer create

- **Layout:** back circle + `timer` header; hours/minutes/seconds wheel picker; `label` box; `start`
  button (disabled at zero). Uses `MetroDurationPicker`.
- **Background:** Black.

### Page 8 — Timer detail

- **Layout:** label header; remaining time in PageTitle; state beneath; actions `pause`/`resume`/
  `start`, `reset`, `delete`.
- **Background:** Black.

### Page 9 — Stopwatch pivot

- **Layout:** list of stopwatches (multiple concurrent). Row: name; `elapsed · running|paused`
  beneath; trailing `start`/`pause`. Elapsed ticks locally.
- **Navigation:** app bar `new` creates + starts a stopwatch and opens its detail.
- **Background:** Black.

### Page 10 — Stopwatch detail

- **Layout:** name header; `H:MM:SS.hh` elapsed in PageTitle; state; actions `pause`/`start`, `lap`,
  `reset`, `rename`, `delete`; a flat `LAPS` list (lap number, lap split, total).
- **Interactions:** lap records `lapNumber`, `lapDurationMillis`, `totalDurationMillis`.
- **Background:** Black.

### Page 11 — Live Tiles

- **Primary (`primary`):** peek-cycle. Deterministic order — finished timers → running timers
  (soonest completion first) → paused timers → running stopwatches → paused stopwatches → next enabled
  alarm; cap six faces then a `+N more` face. Idle → a single next-alarm face (`next alarm` /
  `7:00 AM` / `weekdays`). Counter badge = finished timers awaiting attention.
- **Secondary:** `timer:<id>`, `stopwatch:<id>`, `world:<cityId>` render a locally-ticking temporal
  peek; `alarm:<id>` shows the time + `label · repeat` and deep-links to the alarm.
- **Taps:** tapping any Clock tile opens the Clock app (the launcher launches it from the foreground);
  `alarm:<id>` opens that alarm's edit page.
- **Contract:** timer/stopwatch/world peeks carry structured `MetroTilePeek.temporal`; the launcher
  ticks locally. Do not broadcast a tile update every second.

## Images

| Image | Page | Notes |
|-------|------|-------|
| _(not yet sourced)_ | all | See `known-gaps.md`; WP8.1 Alarms screenshots pending |
| `alarms_dark_blue.png` | Alarms pivot | expected |
| `alarm_edit_dark_blue.png` | Alarm edit | expected (wheel picker) |
| `worldclock_dark_blue.png` | World clock | expected |
| `timer_dark_blue.png` | Timer | expected |
| `stopwatch_dark_blue.png` | Stopwatch | expected |

## Out of scope (v1)

- Sleep tracking / focus sessions, Cortana, cloud clock sync, online city lookup, weather in World
  Clock, Windows 10 Mobile styling.
