# Calendar — known reference gaps

| Missing / low-fidelity file | Should show | Workaround |
|-----------------------------|-------------|------------|
| `agenda_dark_blue.png` is WP8.0 capture (240×400) | WP8.1 Update 2 agenda pivot at 768×1280 | Use WP8.0 layout from interact-sw blog + OnMSFT agenda-update description; blueprint § Agenda pivot defines layout |
| No official WP8.1 agenda screenshot at xhdpi | Agenda list with pivot header "agenda" | Blueprint + WP8.0 agenda capture for row layout |
| No close-up of the week mini-month tile / year cell at xhdpi | Week 4×2 grid + year 3×4 mini-months | Implemented from `week_dark_blue.jpg` / year capture + WP8.1 conventions; revisit if better captures arrive |

## Implemented (previously "out of v1 scope")

- **Week view** — 4×2 grid (7 day tiles + mini-month tile), `this week next week` pivot, weather.
- **Year view** — 3×4 mini-months with event bars, year pivot, today boxed.
- **Writes** — `WRITE_CALENDAR`, local "Phone" calendar bootstrap, create/edit/delete, per-calendar
  show/hide + colour. Recurring edits apply to the whole series (v1).

## ICS subscription notes

| Gap | Detail | Mitigation |
|-----|--------|------------|
| No reference for the add-a-calendar / calendars screens | WP8.1 had no public ICS-URL UI to copy | Follow existing Metro patterns (subpage host, `MetroListItem`, bottom app bar) so the flow matches the rest of the suite |
| TZID-aware recurrence uses the device timezone | biweekly does not expose per-event TZID after parse; UTC feeds are expanded in UTC | Most public feeds publish UTC or a fixed zone; document and revisit if exotic TZID feeds are reported |
