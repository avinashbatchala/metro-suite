# Agent instructions — Calendar (`com.metro.calendar`)

**Tier 2** | Day / Week / Month / Year scales (default **Week**). Reference: `references/images/`.

Scale is a `CalendarView { Day, Week, Month, Year }`; agenda is a separate
`CalendarPresentation { Calendar, Agenda }` toggled from the ellipsis (Day/Week only). **No permanent
view tabs and no pinch** — the app bar **View** command selects the scale, and horizontal swipe moves
the period only. Context headers (overline + pivot) move with the pager. Selecting a day in
Week/Month expands a pane in place; a second tap opens Day. Year→Month drills into Month. The Day
view is a real time grid (one block per event, durations/lanes) with Quick Events from empty slots.

Events come from the on-device provider plus read-only ICS subscriptions
(`com.metro.calendar.data.subscription`), merged in `CalendarRepository`. `canEdit`/`canDelete` are
derived from the real calendar access level — never from the source type. Subscriptions are
read-only. Loading runs off the main thread, range-scoped to the visible view, with loading/error
state (no Android Toasts).

Writes use `WRITE_CALENDAR` (`CalendarWriteRepository`), bootstrapping a local `Phone` calendar when
none exists. Request READ and WRITE **by need**, never together on launch. No demo-data onboarding.
Calendar Settings (ellipsis → settings) holds device show/hide + colour (local overrides) and
subscribed calendars; there is no global "sync calendars".

Verify: `../../scripts/verify-app.sh calendar`
