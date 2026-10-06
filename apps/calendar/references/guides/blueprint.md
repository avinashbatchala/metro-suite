# Calendar — blueprint

**Spec for this app.** Reference screenshots in `images/` are authoritative where they disagree
with this file (update this file when they do). Read this before `images/` or `web-resources.md`.

Target: the mature 2014–2015 **Windows Phone 8.1 Calendar**. Not Windows 10 Mobile.

## Navigation

Top-level **scales** are `CalendarView { Day, Week, Month, Year }`. **Default is Week.** There is
**no permanent view-tab row** and **no pinch-to-switch**.

- **View** is chosen from the app-bar **View** command (a WP8.1 picker listing day/week/month/year).
- **Horizontal swipe** moves to the previous/next day / week / month / year — it never changes scale.
- The **context header** above the grid moves with the pager:
  - Day: `02 APRIL 2026` overline + weekday names (`thursday friday …`)
  - Week: `APRIL 2026` overline + `this week / next week / …`
  - Month: `2026` overline + month names (`april may june …`)
  - Year: `YEAR` overline + years (`2025 2026 2027 …`)

A **selected date** is shared. Selecting a day inside Week/Month expands that day's appointments in
a pane **without leaving the view**; a second tap on the selected day opens the full Day view.

## Agenda (later WP8.1 update)

Agenda is **not** a fifth scale. It is an alternate **presentation** of the selected Day or Week:

- Day/Week ellipsis menu: `show agenda` → a concise list for the selected day/week.
- While agenda is shown the menu becomes `show calendar`.
- Month/Year show no agenda command.

State: `CalendarPresentation { Calendar, Agenda }`; choosing Month/Year returns to Calendar.

## App bar (bottom)

| Icon | Label | Action |
|------|-------|--------|
| Date bubble (day + month) | `today` | Jump to the current date/scale (Day scrolls to now) |
| Add | `new` | New appointment (basic editor) |
| View | `view` | WP8.1 scale selector: day / week / month / year |
| (ellipsis) | — | `show agenda`/`show calendar` (Day/Week) then `settings` |

No global `sync calendars` (subscription refresh lives in Settings).

## Week view

A **continuous 4×2 grid** with hairline separators and no gaps/cards:

```
MON 7   TUE 8   WED 9   THU 10
FRI 11  SAT 12  SUN 13  mini-month
```

Each day tile shows the uppercase weekday, the date, and event titles (calendar colours). Today is
accented. The eighth cell is a full mini-month (today boxed). Tapping a day expands that day's
appointments in a pane below (`TIME · title · duration`), keeping Week visible; tapping the expanded
day again opens Day. Weather (when date-correct) appears on today's tile.

## Month view

A dense, nearly edge-to-edge seven-column grid with horizontal **and** vertical hairline separators.
Weekday labels sit directly above the grid. Each cell: small date number, up to three per-calendar
colour bars below it; outside-month dates are subdued; today is accented. Tapping a day shows the
selected-day pane below (`12:00 PM Event · 1 hour`), keeping Month visible; a second tap opens Day.
There is no invented corner-triangle marker.

## Year view

Twelve mini-months in a 3×4 grid, each with single-letter weekday headers and event bars; today is
boxed. Tapping a month drills into **Month** (never Day), anchored to the first of the month (or
today when the current month is selected).

## Day view

A real time grid: a 24-hour ruler with one **event block per appointment** at its absolute position
and true duration (multi-hour events render **once**, not per hour). Overlapping events share
side-by-side lanes. All-day appointments sit in a compact section above the timeline. Today shows a
current-time marker and auto-scrolls near now.

**Quick Events:** tapping an empty hour opens an inline subject editor at that slot; saving creates a
default-duration appointment in the default writable calendar. The full editor is reachable from the
app-bar Add command.

## Event detail

`title` (calendar colour), `when`, `where`, `calendar`, `status` (when not busy), attendees
(`N people` + list), `notes`, and a read-only note for subscriptions. An editable event shows
**edit** (pencil) and **delete** (trash) in the app bar. Long-pressing an editable event anywhere
opens a context menu with edit/delete; read-only events expose neither.

## Appointment editor

**Basic** screen: subject, location, all-day, starts/ends (date + time open picker surfaces),
`more details`. **More details**: calendar, reminder, repeat, status, notes. Date/time use the
toolkit `MetroDatePicker` / `MetroTimePicker`. Recurring edits apply to the whole series (v1).

## Settings (`… → settings`)

WP8.1 Calendar Settings:

- **device** — every visible device calendar with a square show/hide checkbox and a colour swatch
  (opens a palette). Visibility/colour are stored as **local overrides**, not written to provider
  metadata (safe on GrapheneOS).
- **subscribed calendars** — read-only ICS subscriptions with enable toggles, `subscribe to
  calendar`, and `sync all subscribed calendars`. A subscription with a failed refresh shows a
  concise `couldn't update`; technical detail lives on the subscription detail page.

## Permissions

Requested **by need**, never together on launch: READ_CALENDAR only when the user chooses “allow
device calendar access”; WRITE_CALENDAR only when creating/editing/deleting a provider event. A
subscription-only setup needs no permission. No demo-data onboarding.

## Data

- Reads `CalendarContract.Instances` (title, description, location, organizer, RRULE, availability)
  and `CalendarContract.Attendees`; computes `canEdit`/`canDelete` from the real calendar access
  level — never from source type. Subscriptions are read-only.
- Writes via `WRITE_CALENDAR` (`CalendarWriteRepository`): insert/update/delete `Events`+`Reminders`,
  `RRULE`, availability. A local `Phone` calendar (`ACCOUNT_TYPE_LOCAL`) is bootstrapped when no
  writable calendar exists.
- Loading is **off the main thread**, range-scoped to the visible view (Day/Week/Month/year), with
  loading/error state instead of Toasts. Timezone, locale, and 12/24h refresh on resume.
- Weather is shown only when date-correct (today) and only from the Weather tile contract.
- All-day events are stored in UTC; timed events use the device zone.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `day_dark_blue.png` | Day | Hourly grid |
| `month_dark_blue.jpg` | Month | Month grid |
| `week_dark_blue.jpg` | Week | 4×2 grid |
| `week_expanded_dark_blue.png` | Week | Expanded day |
| `agenda_dark_blue.png` | Agenda | WP8.0 list (restored in 8.1 Update 2) |
| `live_tile_medium_dark_blue.png` / `live_tile_wide_dark_blue.png` | Live tile | Agenda tile |

## Out of scope (v1)

- Lockscreen "next appointment".
- Per-occurrence recurring edits (whole series only).
- Native invite delivery (attendee writes limited to provider-supported calendars).
- Landscape layout.

## MetroSuite extension

Read-only ICS/URL subscriptions are a MetroSuite extension presented inside Calendar Settings; they
are not part of stock WP8.1.
