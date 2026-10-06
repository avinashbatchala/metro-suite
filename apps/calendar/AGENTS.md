# Agent instructions — Calendar (`com.metro.calendar`)

**Tier 2** | Agenda / day / month pivots. Reference: `references/images/`.

Use pivot for view switching (agenda, day, month). `MetroAppBar` for today navigation. Events come from the on-device provider plus read-only ICS/URL subscriptions (`com.metro.calendar.data.subscription`), merged in `CalendarRepository` into one normalized `CalendarEvent` stream.

Subscriptions: HTTPS only, stored app-privately, never logged; fetched with ETag/Last-Modified and a last-good cache; recurrence expanded with biweekly. Manage via app-bar **calendars** subpages (`MetroSubpageHost`). Reads the device calendar only when `READ_CALENDAR` is granted — never `WRITE_CALENDAR`.

Verify: `../../scripts/verify-app.sh calendar`
