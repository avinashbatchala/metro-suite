# Calendar — blueprint

**Spec for this app.** Reference screenshots in `images/` are authoritative where they disagree
with this file (update this file when they do). Read this before `images/` or `web-resources.md`.

## Navigation

Top-level **views** are `CalendarView { Day, Week, Month, Year, Agenda }`:

- **day** — full 24-hour schedule for the selected date.
- **week** — 4×2 grid of tiles (seven days + a mini-month tile).
- **month** — month grid with per-calendar colour bars.
- **year** — twelve mini-months in a 3×4 grid.
- **agenda** — chronological list of upcoming events (reached via the app-bar **list** icon).

Each view shows a **context pivot** of its unit, rendered by `MetroPivotTitleWindow`:

| View | Pivot titles |
|------|--------------|
| day | weekday names (`tuesday wednesday …`) |
| week | `this week`, `next week`, `last week`, `week of <Mon d>` |
| month | month names (`october november …`) |
| year | years (`2026 2027 …`) |
| agenda | none |

**View switching:** a compact tab row `day · week · month · year` sits directly under the status
bar (the app title is omitted; the tabs are the header). Tapping a tab switches with an animated
cross-fade + scale. Two-finger pinch is an accelerator: it previews the target tab, then commits on
release. Pivot flick/tap moves between units within a view. **Agenda is a separate destination**,
reached via the app-bar **list** icon — it is not a zoom level and is excluded from pinch.

Region drives the first day of week (`WeekFields.of(locale)` — Sunday in the US, Monday in the UK).
12/24h and localized month/day names follow the device.

## Header (top)

```
day   week   month   year        <- view tabs (active = accent; preview = accent @ 55%)
<context pivot>                  <- weekday names / this week next week / month names / years
<content>
```

## App bar (bottom)

Round icon buttons (no text buttons, no labels at rest):

| Icon | Label | Action |
|------|-------|--------|
| Date bubble (day + month) | `today` | Jump to today; highlight/scroll to now |
| Add | `new` | Open the new-appointment editor |
| List | `agenda` | Switch to the agenda view (icon shows the **selected** filled state in agenda) |
| (ellipsis) | — | Expand; menu: `calendars`, `sync calendars` |

## Pages

### Agenda

Black background. Selected-date line above the list, then a vertically scrolling list grouped by
date. Date group header: `SUNDAY, 06 OCTOBER 2026` (`SectionHeader`, secondary). Event row: time
column (`All day` / `HH:mm`), accent title, duration subtitle, thin accent bar on the right.

### Day

Date overline (`TUESDAY, 06 OCTOBER 2026`), all-day rows (with weather temp + condition when the
Weather app is installed), then a **full 24-hour** grid. Left-aligned hour labels, thin dividers;
events inline in their hour, accent-coloured. Today scrolls to and highlights the current hour.

### Week

Four columns × two rows of tiles: seven day tiles (`MON 7` + event titles) and a **mini-month tile**
in the bottom-right. Today is accent-bordered; today's tile shows the weather temp. Weekend tiles are
tinted. Tapping a day drills into the day view; tapping an event opens its detail.

### Month

Region-first weekday header, six-week grid with thin dividers. Date number top-left; up to three
**per-calendar colour bars under the date number**; selected day shows an accent corner notch; today
is emphasised. Tapping a day drills into the day view.

### Year

Twelve mini-months in a 3×4 grid, each with single-letter weekday headers, event bars, and today
boxed. Tapping a mini-month drills into it.

## Event detail

Subpage with a back affordance: title (accent), `when`, `where`, `calendar`. For writable (device)
events the app bar shows **edit** (save glyph) and **delete** (trash). Subscriptions are read-only.

## Appointment editor (`new appointment` / `edit appointment`)

Fields: subject, location, all-day toggle, starts (date + time wheels), ends, calendar list picker,
reminder list picker (none / at start / 5 / 10 / 30 min / 1 h / 1 day before), repeat (never / daily
/ weekly / monthly / yearly), notes. Date/time use the toolkit `MetroDatePicker` / `MetroTimePicker`.
App bar: save (check); delete (when editing). Recurring edits apply to the whole series (v1).

## Calendars screen (`… → calendars`)

Device calendars (writable) with a visibility toggle and a per-calendar colour picker (accent
palette). Subscribed ICS calendars with enable toggles + drill-in detail. App-bar `add calendar`.

## Data & writes

- Reads `CalendarContract` via `READ_CALENDAR`; subscriptions are read-only ICS (HTTPS only).
- Writes via `WRITE_CALENDAR` (`CalendarWriteRepository`): insert/update/delete `Events` +
  `Reminders`, `RRULE` for recurrence. If no writable calendar exists, bootstrap a local `Phone`
  calendar (`ACCOUNT_TYPE_LOCAL`) — needed on GrapheneOS with no accounts.
- All-day events are stored in UTC; timed events use the device zone.
- Weather (day/week) is a best-effort read of the Weather app's tile peek.

## Live tile

WP8.1 Calendar agenda tile. Accent background from the system accent. Structured agenda payload
(`MetroTileData.agenda`) for 2×2 / 4×2; lines `[title, location?, time]`; date badge (short weekday
+ day-of-month) bottom-right; footer `Calendar`.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `agenda_dark_blue.png` | Agenda | WP8.0 agenda list (restored in WP8.1 Update 2) |
| `day_dark_blue.png` | Day | Hourly grid |
| `month_dark_blue.jpg` | Month | Month grid |
| `week_dark_blue.jpg` | Week | 4×2 grid + mini-month |
| `week_expanded_dark_blue.png` | Week | Expanded day |
| `live_tile_medium_dark_blue.png` | Live tile (2×2) | Agenda tile |
| `live_tile_wide_dark_blue.png` | Live tile (4×2) | Agenda tile |
| `hero_dark_blue.jpg` | — | Marketing hero |

## Out of scope (v1)

- Lockscreen "next appointment".
- Recurring-instance (this-occurrence-only) edits — whole series only.
- Account-specific sync (Exchange/Google quirks).
- Landscape layout.
