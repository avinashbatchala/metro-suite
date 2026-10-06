# Clock — web resources

Curated links for how this app should look and behave on Windows Phone 8.1.

Add one `##` section per screen or feature. Keep URLs stable; use archive.org mirrors when originals disappear.

## General

| Resource | URL | Notes |
|----------|-----|-------|
| scope.md — this app | [`scope.md`](../../../scope.md) | Repo source of truth |
| Metro UX language (repo) | [`toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](../../../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) | Flat Metro rules, banned Material patterns |
| Microsoft design language (type ramp, tiles) | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh700394(v=win.10) | Historical Windows Phone design guidance |
| Windows Phone 8.1 features / Alarms | https://en.wikipedia.org/wiki/Windows_Phone_8.1 | Feature context; the stock Alarms app was minimal |

## Alarms

| Resource | URL | Notes |
|----------|-----|-------|
| WP8.1 Alarms app screenshots / discussion | https://www.windowscentral.com/how-use-alarms-windows-phone-8-1 | Period-correct layout: list + toggle, wheel time picker, repeat presets |
| WP8.1 Alarms tile (next alarm) | https://www.windowscentral.com/how-customize-start-screen-windows-phone-8-1 | Live Tile exposing the next active alarm |

## World clock / timer / stopwatch

WP8.1 did not ship first-party world-clock / timer / stopwatch apps; these pivots follow the repo's
Metro conventions and period third-party apps.

| Resource | URL | Notes |
|----------|-----|-------|
| Period Metro timer/stopwatch references (TimeMe, 1:CLOCK) | _(community captures)_ | Big thin digits, flat controls, list of instances |
| Windows 8.1 Alarms behavior (secondary) | https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-8.1-and-8/ | Behavior reference only — not visual |

## Android platform behavior

| Resource | URL | Notes |
|----------|-----|-------|
| Exact alarms (`setAlarmClock`, `USE_EXACT_ALARM`, Doze) | https://developer.android.com/develop/background-work/services/alarms/schedule | Scheduling strategy |
| Full-screen intents | https://developer.android.com/develop/ui/views/notifications/time-sensitive | Alarm ringing when locked |
| Notification channels & POST_NOTIFICATIONS | https://developer.android.com/develop/ui/views/notifications/channels | Alarm + timer channels |
