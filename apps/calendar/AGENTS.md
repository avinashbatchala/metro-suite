# Agent instructions — Calendar (`com.metro.calendar`)

**Tier 2** | Day / week / month / year views (+ agenda). Reference: `references/images/`.

Views are a state machine in `CalendarView { Day, Week, Month, Year, Agenda }`, each with its own
context pivot (weekday names / `this week next week` / month names / years) rendered by
`MetroPivotTitleWindow` (toolkit). Switch views with **pinch-to-zoom** or the app-bar switch icon
(`cycleView`); the app-bar **list** icon opens agenda. Region-first weekday / localized names come
from `WeekFields.of(locale)`; 12/24h and locale are device-driven (`CalendarLogic`).

Events come from the on-device provider plus read-only ICS/URL subscriptions
(`com.metro.calendar.data.subscription`), merged in `CalendarRepository` into one normalized
`CalendarEvent` stream. Subscriptions: HTTPS only, stored app-privately, never logged; fetched with
ETag/Last-Modified and a last-good cache; recurrence expanded with biweekly. Manage via app-bar
**calendars** subpages (`MetroSubpageHost`).

Writes use `READ_CALENDAR` **and** `WRITE_CALENDAR` (`CalendarWriteRepository`). If no writable
calendar exists (e.g. GrapheneOS with no accounts) the repo bootstraps a local `Phone` calendar
(`ACCOUNT_TYPE_LOCAL`). Create/edit/delete appointments via the `EventEdit` subpage; per-calendar
show/hide + colour live in the `Calendars` screen. v1 edits/deletes apply to the whole recurring
series.

Day/week headers read the Weather app's tile peek (`CalendarWeatherReader`, best-effort).

Verify: `../../scripts/verify-app.sh calendar`
