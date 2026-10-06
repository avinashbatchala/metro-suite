# Clock — known reference gaps

| Missing / low-fidelity item | Should show | Workaround |
|-----------------------------|-------------|------------|
| No first-party WP8.1 world clock / timer / stopwatch app | Those pivots | WP8.1 shipped only **Alarms**; world clock/timer/stopwatch follow repo Metro conventions + period third-party apps |
| WP8.1 Alarms screenshots not yet in `images/` | Alarms list, edit (wheel picker), ringing | Blueprint §Pages 1–3 define layout; period captures at windowscentral.com |
| Primary Clock tile peek-cycle reference | Next-alarm face + multi-item cycle | Blueprint §Page 11 + `ClockPeekLogic` define ordering; launcher `PEEK_CYCLE` behavior |
| Golden screenshots | Emulator captures for diff | Not captured yet; `screenshot_diff` is skipped when `screenshots/golden/` is empty |
