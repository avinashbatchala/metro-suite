# Settings

**Package:** `com.metro.settings`  
**Tier:** 2

## Status

Implemented — Settings root with `system` | `applications` pivot (all launchable user + system apps; in-Settings app detail with toggles/open/uninstall), start+theme (accent colour + font + icon pack + Start background choose photo + show more columns), accent picker (20 WP8 colours), ease of access (10-step text size), brightness, storage sense, **ringtones + sounds** (Metro sound pack install + semantic sound pickers + system defaults), connected apps (gallery / music live-tile package lists + conversation apps), navigation bar / status bar / notifications / volume / lock screen (launch shell setup apps), keyboard (launches `com.metro.keyboard`), and about (WP8.1 more info device details; Software = metro-os alpha-3). Hosts `content://com.metro.system` preferences provider and the read-only `content://com.metro.settings.sounds` sound provider.

## App role

This app recreates the WP8.1 **Settings** experience and is the authoritative owner of system preference writes for metro-os. It controls accent color, font scale, and surfaces a small set of system pages (brightness, storage, about).

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Shared preference contract in `metro-system-sdk` understood before UI work

## Screen inventory

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

| Screen | Status |
|--------|--------|
| Settings root (`system` \| `applications` pivot) | Done |
| applications list | Done (all launchable user + system apps) |
| application detail | Done (info, background/notifications toggles, open, uninstall) |
| start+theme | Done (accent colour + font + icon pack + Start background + show more columns) |
| Accent colour picker | Done (20 official) |
| ease of access (text size) | Done |
| brightness | Done (`WRITE_SETTINGS` when granted) |
| storage sense | Done — usage bar + open files → `com.metro.files` |
| navigation bar | Done (launches `com.metro.navbar` setup) |
| status bar | Done (launches `com.metro.statusbar` setup) |
| notifications | Done (launches `com.metro.notifications` setup) |
| volume | Done (launches `com.metro.volume` setup) |
| lock screen | Done (launches `com.metro.lockscreen` setup) |
| keyboard | Done (launches `com.metro.keyboard` settings) |
| connected apps | Done (gallery / music live tiles + conversation apps) |
| gallery apps / music apps | Done (apps corner list + multi-select picker) |
| ringtones + sounds | Done (install Metro sound pack; ringtone / messages / mail / calendar / reminders / system / alarm / timer; previews; system defaults) |
| sound picker / about Metro sounds | Done |
| about / more info | Done (device information; Software = metro-os alpha-3) |

## Ringtones + sounds

Settings is the canonical owner and installer of the Metro sound pack (`assets/metro_sounds/`, OGG
only; WAV masters are not shipped). There is **no separate Sounds app**.

- **Install:** user taps `install Metro sounds`; the pack is copied into Android **MediaStore** under
  `Ringtones/Metro/`, `Notifications/Metro/`, `Alarms/Metro/` using scoped APIs
  (`RELATIVE_PATH` + `IS_PENDING`). No `READ/WRITE_EXTERNAL_STORAGE` or
  `MANAGE_EXTERNAL_STORAGE` is requested. Installation is **idempotent** — a stored URI is reused,
  else an existing row is matched by `DISPLAY_NAME` (+ `RELATIVE_PATH`), else a new row is inserted.
  A registry (`MetroSoundStore`, prefs `metro_sounds`) maps `soundId → content:// URI` and tracks the
  pack version.
- **Semantic roles** live in `metro-system-sdk` (`MetroSoundRole`, `MetroSoundCategory`,
  `MetroSoundDescriptor`, `MetroSoundContract`). Apps resolve a role; they never see Settings asset
  paths. Settings persists the role → sound-id selection separately from the URI so a URI can be
  repaired/reinstalled.
- **Preview:** tapping a sound selects it and previews it via a single-instance `Ringtone` (no
  service); leaving the page stops playback.
- **System defaults:** `ringtone` → `TYPE_RINGTONE`, `system` (notifications) → `TYPE_NOTIFICATION`,
  `default alarm` → `TYPE_ALARM` via `RingtoneManager.setActualDefaultRingtoneUri`. This requires
  Android's **Modify System Settings** capability (`Settings.System.canWrite`), requested only when
  the user performs such a change. Silent/DND/volume/Bluetooth policy is left to Android.
- **Notification channels:** Android channels are user-owned after creation. Settings never
  deletes/recreates a channel to change its sound; the semantic `messages`/`mail`/`calendar`/
  `reminders` selections are suite defaults that apps adopt only when first creating their channel
  (`MetroNotificationChannels.applyInitialSound`), and channel settings deep links are available
  (`MetroNotificationChannels.openChannelSettings`).
- **Contract provider:** `content://com.metro.settings.sounds` (`role/<ROLE>`, `pack`) is read-only
  and exposes the resolved sound id/title/category/URI plus pack version.
- **Reversibility:** each picker offers `system default`; the prior default URI for a system role is
  backed up before the first Metro change. Missing/broken URIs fail safely (no crash), with reinstall
  available.
- **Attribution:** `about Metro sounds` states the set is original for MetroSuite and not official
  Microsoft/Nokia/Windows Phone audio.
- **Offline:** the pack is bundled; no network access.

## System functions and contracts

### Preference ownership

- This app owns writes to `MetroPreferences` and exports `MetroSystemPreferencesProvider` (`com.metro.system`)
- Other apps read via ContentResolver + observe `THEME_CHANGED`

### Broadcast contract

- Broadcast `THEME_CHANGED` on every relevant preference change (`theme_mode`, `accent_color`, `font_scale`)

### Official setting keys

- `theme_mode`
- `accent_color` (official palette hex)
- `font_scale` (10 discrete steps, 0.625–1.6, default 1.0)
- `connected_gallery_apps` (comma-separated packages; null → suite defaults)
- `connected_music_apps` (comma-separated packages; null → suite defaults)

## Commands

```bash
cd apps/settings

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh settings
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| True OS-level ownership of all system visuals | App-layer suite on Android | Settings owns metro-os shared prefs + broadcasts; Android system chrome outside suite remains out of scope |
| Full system settings list | Large OEM surface | v1 implements personalization + brightness, storage, shell setup launches, keyboard, about + applications pivot |
| Applications hubs (IE, photos+camera, …) live inside Settings | Suite apps lack per-app hubs | Shared application detail page for every launchable package |
| OS notification / background kill for other packages | Privileged AppOps | Toggles store metro-os `metro_app_policy` prefs; suite enforcement when agents exist |
| Uninstall confirmation | Package installer is a system activity | `MetroMessageDialog` then `ACTION_DELETE` (same as launcher); not Android Settings |
| start+theme Background ListPicker | Deferred | Theme stays dark (`theme_mode` default); UI matches Accent colour combo only |
| Icon packs on Start | WP8.1 had no third-party icon packs | Settings → start+theme Icon pack ListPicker drills into `MetroIconPackPickerScreen`; launcher applies via `MetroIconPacks` / `appfilter.xml` |
| Icon packs on Start | WP8.1 had no third-party icon packs | Settings → start+theme Icon pack ListPicker drills into `MetroIconPackPickerScreen`; launcher applies via `MetroIconPacks` / `appfilter.xml` |
| Brightness write | Needs `WRITE_SETTINGS` app-op | Write `Settings.System` directly; grant via `adb shell appops set com.metro.settings WRITE_SETTINGS allow` |
| Never open Android Settings from Metro Settings | Permission grant UIs are system activities | No in-app “open settings” buttons; grant permissions out-of-band (adb / privileged install) |
| Settings → keyboard inside system Settings hub | Keyboard is a separate suite APK | Root `keyboard` row launches `com.metro.keyboard` (not Android Settings) |
| Settings → navigation bar / status bar / notifications / volume / lock screen | Overlay + accessibility grants are per shell package | Root rows launch `com.metro.navbar` / `com.metro.statusbar` / `com.metro.notifications` / `com.metro.volume` / `com.metro.lockscreen` setup (not Android Settings) |
| About IMEI / MAC / SIM ID | Need telephony / Wi-Fi MAC permissions | Omitted; more info shows Build.* and storage fields without privileged identifiers |
| ease of access Text size has 7 steps (0.85–1.6) | Modern panels are far denser than a 4.5" WVGA Lumia, so 0.85 is still large | Slider keeps the 7 WP8.1 steps and prepends 0.625 / 0.7 / 0.775 at the same 0.075 spacing (10 total, default 1.0 unchanged) |

## Agent postmortem

_None._
