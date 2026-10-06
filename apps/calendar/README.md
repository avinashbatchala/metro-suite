# Calendar

**Package:** `com.metro.calendar`  
**Tier:** 2

## Status

Implemented — agenda / day / month pivot views with local calendar provider data, read-only ICS/URL subscriptions, demo fallback, and live tile provider.

## App role

This app recreates the WP8.1 **Calendar** experience with agenda, day, and month views organized via pivot navigation and backed by local calendar-provider data in v1.

The emphasis is information clarity, fast pivot switching, and Metro layout discipline rather than dense Android calendar chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Local calendar-provider access strategy approved

## Screen inventory

### 1. Agenda pivot

- Chronological agenda list
- Expected reference: `references/images/agenda_dark_blue.png`

### 2. Day pivot

- Focused day schedule
- Expected reference: `references/images/day_dark_blue.png`

### 3. Month pivot

- Month overview surface
- Expected reference: `references/images/month_dark_blue.png`

## Subscribed calendars (read-only ICS)

The app can subscribe to public **iCalendar (`.ics`) URLs** in addition to the on-device calendar
provider. This is deliberately **read-only**: no CalDAV, no OAuth, no write-back, and no
`WRITE_CALENDAR` permission.

- **URLs:** HTTPS only; `webcal://` is normalized to `https://`. Subscription URLs are treated like
  secrets — they are stored in app-private storage and only ever displayed masked
  (`SubscriptionUrl.mask`), never logged or put on a tile.
- **Fetching:** conditional requests (`ETag` / `Last-Modified`), 10 MB cap, `BEGIN:VCALENDAR`
  validation. A failed refresh **keeps the last-good cache** rather than clearing events.
- **Parsing:** RFC 5545 via biweekly with recurrence expansion (`RRULE` / `RDATE` / `EXDATE`),
  `RECURRENCE-ID` overrides, and `STATUS:CANCELLED` filtering. UTC feeds expand in UTC; floating /
  TZID / date-only events expand in the device timezone.
- **Background refresh:** WorkManager periodic job (~6 h, network-constrained); manual **sync now**
  performs a real fetch.
- **Merge:** subscription events carry `sourceType = SUBSCRIPTION` and a deterministic id derived
  from `(subscriptionId, UID, occurrence start)`, then merge with provider events into the single
  `CalendarEvent` stream that feeds agenda/day/month and the live tile.

Manage subscriptions from the app bar (**calendars** → **add calendar** / subscription detail).
The device calendar permission is optional: a subscription-only setup works without granting it.

## System functions and contracts

- Use local calendar provider data plus read-only ICS subscriptions
- Provide a Today action in the app bar
- Normalize event models so agenda/day/month render from the same source of truth
- Recurrence and timezone handling: expanded with biweekly (`RRULE`/`RDATE`/`EXDATE`/`RECURRENCE-ID`); UTC feeds in UTC, others in the device timezone

## UI and interaction guardrails

- Pivot is the top-level navigation pattern here
- Keep headers and typography consistent with WP8.1 hierarchy
- Avoid dense Material calendars, chips, or floating create actions
- Use app bar actions for Today and any add/edit flow

## Data and state model

- `CalendarEvent`, `DayBucket`, `MonthGridCell`
- Track selected date, current pivot, timezone context, and provider sync/load state

## Primary implementation order

1. Build provider repository and event normalization
2. Implement selected-date state and Today action
3. Implement agenda view
4. Implement day view
5. Implement month view
6. Add event detail/create flows if in scope

## Test-critical user flows

1. Load local calendar events
2. Switch among agenda/day/month pivots
3. Jump back to Today
4. Preserve selected date when navigating in and out of detail screens

## Reference and golden expectations

- `references/images/agenda_dark_blue.png`
- `references/images/day_dark_blue.png`
- `references/images/month_dark_blue.png`

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
