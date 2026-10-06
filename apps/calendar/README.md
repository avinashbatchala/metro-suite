# Calendar

**Package:** `com.metro.calendar`  
**Tier:** 2

## Status

Implemented — WP8.1 **Day/Week/Month/Year** scales (default **Week**) with the View app-bar selector,
Week/Month selected-day panes, a real Day timeline with Quick Events, appointment create/edit/delete,
Calendar Settings, and a next-appointment live tile. Backed by the on-device calendar provider plus
read-only ICS/URL subscriptions (MetroSuite extension).

## App role

This app recreates the mature 2014–2015 **Windows Phone 8.1 Calendar**: Week as the primary view, a
context header that moves with horizontal paging, day/month selection panes that preserve context,
and a fast appointment flow. Not Windows 10 Mobile Outlook Calendar.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Local calendar-provider access strategy approved

## Screen inventory

### Week (default)

- Continuous 4×2 grid: seven day tiles + a mini-month tile
- Selected-day pane expands in place (`references/images/week_dark_blue.jpg`, `week_expanded_dark_blue.png`)

### Day

- 24-hour time grid with one block per appointment (durations/lanes), all-day section, now marker
- Quick Events from empty slots (`references/images/day_dark_blue.png`)

### Month

- Dense edge-to-edge grid with per-calendar colour bars + selected-day pane (`references/images/month_dark_blue.jpg`)

### Year

- Twelve mini-months (3×4); tapping a month enters Month

### Agenda (later update)

- Alternate Day/Week presentation reached from the ellipsis (`references/images/agenda_dark_blue.png`)

## Subscribed calendars (read-only ICS — MetroSuite extension)

The app can subscribe to public **iCalendar (`.ics`) URLs**. This is deliberately **read-only**: no
CalDAV, no OAuth, no write-back.

- **URLs:** HTTPS only; `webcal://` is normalized to `https://`. URLs are stored app-privately and
  only ever displayed masked (`SubscriptionUrl.mask`), never logged or put on a tile.
- **Fetching:** conditional requests (`ETag` / `Last-Modified`), 10 MB cap, `BEGIN:VCALENDAR`
  validation; a failed refresh keeps the last-good cache.
- **Parsing:** RFC 5545 via biweekly with recurrence expansion (`RRULE`/`RDATE`/`EXDATE`),
  `RECURRENCE-ID` overrides, `STATUS:CANCELLED` filtering.
- **Background refresh:** WorkManager periodic job (~6 h); manual **sync all** lives in Calendar
  Settings.

Manage them in **Settings → subscribed calendars**. The device calendar permission is optional: a
subscription-only setup works without granting it.

## System functions and contracts

- View selector for Day/Week/Month/Year; Today, New, and agenda/settings commands in the app bar
- Writes to the device provider (create/edit/delete, reminders, recurrence, availability)
- Normalizes provider + subscription events into one `CalendarEvent` stream
- Recurrence/timezone: biweekly expansion; UTC feeds in UTC, others in the device timezone

## UI and interaction guardrails

- One way to change scale: the View command. Horizontal swipe changes period only. No pinch, no tabs
- Week = continuous 4×2 grid; Month = dense hairline grid; selection expands in place
- No Material cards/chips/FAB/bottom-nav/tab-row, no Android Toast/snackbar

## Data and state model

- `CalendarEvent` (+ `calendarId`, `canEdit`/`canDelete`, description, organizer, availability, recurring)
- `CalendarView {Day,Week,Month,Year}` + `CalendarPresentation {Calendar,Agenda}`
- Loading/error state; timezone/locale/12-24h refreshed on resume

## Primary implementation order

1. Provider + subscription repository and event normalization
2. View/presentation state machine (default Week) + View selector
3. Week grid + selected-day pane
4. Month grid + selected-day pane
5. Day timeline + Quick Events
6. Year view + drill-down to Month
7. Appointment editor (basic + more details)
8. Calendar Settings

## Test-critical user flows

1. Launch → Week
2. View: Week → Day → Month → Year
3. Swipe next day/week/month/year
4. Year → tap month → Month (not Day)
5. Month → tap date → pane in Month; Week → tap day → pane in Week
6. Day → tap empty slot → Quick Event
7. Long-press editable appointment → edit/delete; read-only → no actions
8. Agenda: Day/Week → show agenda → show calendar
9. Today jumps the selected period in every view

## Reference and golden expectations

- `references/images/day_dark_blue.png`
- `references/images/month_dark_blue.jpg`
- `references/images/week_dark_blue.jpg`, `week_expanded_dark_blue.png`
- `references/images/agenda_dark_blue.png`

## Commands

```bash
cd apps/calendar

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh calendar
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Exact WP calendar account integrations | Android provider/account combinations differ by device | Build the Metro surfaces over local provider data first and document unsupported account-specific quirks |

## Sounds

No notification channel exists yet. When calendar reminders post notifications, the reminder channel
should adopt the suite default on **first creation**:

```kotlin
MetroNotificationChannels.applyInitialSound(context, manager, channel, MetroSoundRole.CALENDAR)
```

Existing channels are user-owned — do not recreate them to change the sound. Metro Settings →
ringtones + sounds exposes `calendar` / `reminders` and can deep-link to the channel. Do not bundle
sound files here.

## Agent postmortem

_None._
