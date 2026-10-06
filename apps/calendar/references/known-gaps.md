# Calendar — known reference gaps

| Missing / low-fidelity file | Should show | Workaround |
|-----------------------------|-------------|------------|
| `agenda_dark_blue.png` is WP8.0 capture (240×400) | WP8.1 Update 2 agenda pivot at 768×1280 | Use WP8.0 layout from interact-sw blog + OnMSFT agenda-update description; blueprint § Agenda pivot defines layout |
| No official WP8.1 agenda screenshot at xhdpi | Agenda list with pivot header "agenda" | Blueprint + WP8.0 agenda capture for row layout |
| Week/year views not captured at xhdpi | Week grid with weather, year overview | Documented out of v1 scope; `week_dark_blue.jpg` kept as ref only |

## ICS subscription notes

| Gap | Detail | Mitigation |
|-----|--------|------------|
| No reference for the add-a-calendar / calendars screens | WP8.1 had no public ICS-URL UI to copy | Follow existing Metro patterns (subpage host, `MetroListItem`, bottom app bar) so the flow matches the rest of the suite |
| TZID-aware recurrence uses the device timezone | biweekly does not expose per-event TZID after parse; UTC feeds are expanded in UTC | Most public feeds publish UTC or a fixed zone; document and revisit if exotic TZID feeds are reported |
