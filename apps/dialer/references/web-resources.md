# Phone — web resources

Curated references. **Blueprint wins on conflict.** Use mature WP8.1 / Update 1 / Update 2 sources;
do not substitute Windows 10 Mobile captures or behaviour.

## Official & Microsoft

| Topic | URL | Notes |
|-------|-----|-------|
| Dialer deep dive | https://blogs.windows.com/devices/2014/02/18/deep-dive-dialer/ | History search, keypad, block number |
| WP 8.1 announcement | https://blogs.windows.com/windowsphone/2014/04/02/windows-phone-8-1-availability/ | Phone as standalone updatable app |

## Android Telecom (implementation authority)

| Topic | URL | Notes |
|-------|-----|-------|
| Telecom overview | https://developer.android.com/develop/connectivity/telecom | `placeCall`, `InCallService`, roles |
| Build a default dialer | https://developer.android.com/develop/connectivity/telecom/dialer-app | `ROLE_DIALER`, `InCallService`, callbacks |
| `InCallService` | https://developer.android.com/reference/android/telecom/InCallService | Call + audio-endpoint callbacks |
| `TelecomManager.placeCall` | https://developer.android.com/reference/android/telecom/TelecomManager#placeCall(android.net.Uri,%20android.os.Bundle) | Real SIM/carrier routing |
| `CallEndpoint` (API 34+) | https://developer.android.com/reference/android/telecom/CallEndpoint | Modern audio endpoints |

## Community reviews & changelogs (WP8.1 / Update 1–2)

| Topic | URL | Notes |
|-------|-----|-------|
| Phone app 8.1 changes | https://www.windowsphonearea.com/video-changes-improvements-phone-app/ | Call/save in grid, grouped history, speed dial |
| Speed dial & call history | https://www.windowscentral.com/windows-phone-81-adds-new-features-call-history | Feature overview |
| GDR1 select calls | https://www.windowscentral.com/how-use-windows-phone-81-update-1 | Multi-select delete |
| Lumia 920 8.1 walkthrough | https://diptimayapatra.wordpress.com/2014/04/14/windows-phone-8-1-update-for-nokia-lumia-920/ | Source of several reference screenshots |
| Call history details | https://forums.windowscentral.com/threads/wp8-1-tips-tricks.275061/ | Tap-and-hold for duration |
| Dual SIM dialer | https://www.wpxbox.com/dual-sim-features-windows-phone-8-1/ | SIM 1/2 switcher reference |

## Implementation hints

| Topic | URL | Notes |
|-------|-----|-------|
| T9 autocomplete pattern | https://stackoverflow.com/questions/34585590/auto-complete-list-for-contact-for-wp8 | Dial-pad suggestion UI reference image |
