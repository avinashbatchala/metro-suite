# Clock — known reference gaps

Fidelity boundary: **A** = authentic WP8.1 *Alarms*; **B** = period-correct WP8.x extensions
(world clock / timer / stopwatch); **C** = MetroSuite extensions. See `guides/blueprint.md`.

| Missing / low-fidelity item | Category | Workaround |
|-----------------------------|----------|------------|
| `images/` is empty (no authentic WP8.1 *Alarms* captures) | A | Blueprint defines the layout from the WP8.1 field-based editor, pickers, and ringing behavior; capture goldens from the running app |
| No first-party WP8.1 world clock / timer / stopwatch app | B | WP8.1 shipped only **Alarms**; these follow WP8.x Metro grammar + period apps (`1:CLOCK`, `TimeMe`, `Interval`, `World'o'Clock`) |
| Per-peek deep links to exact objects | A/C | Secondary tiles have `metro://clock/...` deep links; primary peek-cycle taps currently open the Clock app root (the launcher peek model exposes a package, not a per-peek URI). Wiring a `deepLinkUri` field through `MetroTilePeek` + the launcher is a follow-up. |
| Timer rename | C | Not exposed (no DAO rename yet); stopwatch rename + delete are available |
| Recognition of the WP8.1 Update snooze-option set | A | Editor offers 5/10/15/20/30 minutes (default 10); refine once an authentic Update-2 capture is available |
| Golden screenshots | — | `screenshot_diff` is skipped while `screenshots/golden/` is empty |
