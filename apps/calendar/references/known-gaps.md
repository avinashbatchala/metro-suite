# Calendar — known reference gaps

| Missing / low-fidelity file | Should show | Workaround |
|-----------------------------|-------------|------------|
| `agenda_dark_blue.png` is WP8.0 capture (240×400) | WP8.1 Update 2 agenda presentation at xhdpi | Use WP8.0 layout + OnMSFT agenda-update description; agenda is a Day/Week presentation here |
| No close-up of the year view / mini-month tile at xhdpi | Year 3×4 mini-months + week mini-month | Implemented from the 2014 week capture + WP8.1 conventions; revisit if better captures arrive |
| No authenticated capture of the Calendar **Settings** screen | device/subscribed groups, square checks, colour selectors | Implemented from the 2015 Microsoft Devices Blog description of Calendar settings |

## Implemented (was out of scope)

- **Week** (default) — continuous 4×2 grid + mini-month tile + expanded selected-day pane.
- **Year** — 3×4 mini-months; tapping a month drills into Month.
- **Day** — real time grid (one block per event, durations/lanes), Quick Events, now marker.
- **Writes** — `WRITE_CALENDAR`, local "Phone" calendar bootstrap, create/edit/delete, availability,
  reminders. Recurring edits apply to the whole series (v1).
- **Settings** — device calendar show/hide + colour (local overrides) and subscribed calendars.

## Interaction tests

Logic (timeline, ranges, availability, week/month/year grids, DST, all-day, recurrence) is unit
tested. Compose interaction-flow tests (launch→Week, View switching, swipe, panes, Quick Event,
long-press) are documented in the blueprint/README flow list and verified manually on device;
automated Compose UI tests are a follow-up.

## ICS subscription notes

| Gap | Detail | Mitigation |
|-----|--------|------------|
| No reference for the subscribe-to-calendar screen | WP8.1 had no public ICS-URL UI | MetroSuite extension following existing Metro patterns |
| TZID-aware recurrence uses the device timezone | biweekly does not expose per-event TZID after parse | Most public feeds publish UTC or a fixed zone; document and revisit if exotic TZID feeds are reported |
