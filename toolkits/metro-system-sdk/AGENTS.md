# Agent instructions — metro-system-sdk

**Phase 1 — build after `metro-ui-android`.**

## Purpose

Cross-app system contracts: shared preferences, intents, broadcasts, content provider URIs.

## Package

`com.metro.system`

## Required APIs

### MetroPreferences

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `theme_mode` | `enum { dark, light }` | `dark` | System theme |
| `accent_color` | `String` (hex) | `#1BA1E2` | Accent from palette |
| `font_scale` | `Float` | `1.0` | Font scaling |
| `font_family` | `String` (`metro_noto` / `source_sans_3` / `alegreya_sans`) | `metro_noto` | Suite chrome typeface |
| `nav_bar_color` | `String?` | null | Nav bar override |
| `show_more_columns` | `Boolean` | `false` | Start 6-col grid (3 medium tiles across) when true; 4-col when false |
| `start_background_enabled` | `Boolean` | `false` | Start background photo active; image at `/start_background` |
| `icon_pack_package` | `String?` | null | Active Android icon-pack package for Start / app-list glyphs; null = system icons ([MetroIconPacks]) |
| `connected_gallery_apps` | `String` (CSV packages) | suite defaults when unset | Photos-style live-tile packages |
| `connected_music_apps` | `String` (CSV packages) | suite defaults when unset | Xbox Music now-playing live-tile packages |
| `connected_conversation_apps` | `String` (CSV packages) | suite defaults when unset | Packages Conversations may show from the shade |

Storage: Settings-hosted ContentProvider (`content://com.metro.system`) backed by
`SharedPreferences` file `metro_system`. Client apps always attempt ContentResolver first,
then fall back to a mirrored local cache (never gate reads on `resolveContentProvider` —
Android 11+ package visibility makes that unreliable). SDK manifest declares `<queries>` for
the provider + `com.metro.settings` so visibility merges into every dependent app.

Theme keys (`theme_mode`, `accent_color`, `font_scale`, `font_family`) use `SharedPreferences.Editor.commit`
for the local mirror and Settings host writes so cold starts survive process death. Clients
should call `pullThemeFromProvider()` (or use `MetroSystemTheme`, which retries on cold open)
before painting accent-dependent UI when Settings may still be waking.

### MetroIntents

| Constant | Action | Extras |
|----------|--------|--------|
| `LAUNCH_APP` | `com.metro.action.LAUNCH_APP` | `package` |
| `SEARCH` | `com.metro.action.SEARCH` | `query` |
| `SHARE` | `com.metro.action.SHARE` | `uri`, `mime` |
| `PIN_TILE` | `com.metro.action.PIN_TILE` | `package`, `tile_id`, optional `tile_size` (`1x1` / `2x2` / `4x2`) |
| `TILE_TAP` | `com.metro.action.TILE_TAP` | `package`, `tile_id` — or app-private action via `MetroTileData.tapAction` |
| `ADD_SPEED_DIAL` | `com.metro.action.ADD_SPEED_DIAL` | `display_name`, `phone_number` |

### Custom Start widget faces (`MetroTileWidgetFace`)

Apps that pin interactive catalog widgets export face state on `MetroTileData.widgetFace` (+ optional `tapAction`). The **launcher renders** the face (clock / battery / glyph) and calls `MetroIntents.dispatchTileTap` instead of launching the app. Same rule as agenda / photo grids: no in-process UI from the source app on Start.

| `kind` | Payload | Tap |
|--------|---------|-----|
| `digital_clock` / `analog_clock` | kind only (launcher ticks locally) | usually none (display) |
| `battery` | `batteryPercent` | usually none |
| `glyph` | `glyph` key (`lock`, …) | `tapAction` → lock / etc. |
| `glyph_toggle` | `glyph`, `toggleOn`, optional `dimmed` | `tapAction` → toggle |
| `peek_cycle` | [MetroTileData.peeks] + optional counter | cycle notifications only (no host icon) |
| `world_clock` | `worldClocks` (1–3 `MetroWorldClockFaceEntry` name+zoneId) | usually none (display; launcher ticks locally) |

Glyph keys: `MetroTileWidgetGlyph` (`torch`, `lock`, `battery_saver`). Drawables live in `metro-ui-android`.

### Temporal peek state (`MetroTilePeek.temporal`)

`MetroTilePeek` may carry an optional `MetroTileTemporalState` so the **launcher renders timers /
stopwatches / world clocks locally** — the source app must never broadcast a tile update per second.
Absent → text-only peek (backwards compatible). Values are timestamps/state, never ticking strings:

- `timer`: `running`, `targetElapsedRealtimeMillis` (monotonic, valid within a boot), `targetEpochMillis`
  (wall-clock recovery aid), `pausedRemainingMillis`, `finished`.
- `stopwatch`: `running`, `accumulatedElapsedMillis`, `runningSinceElapsedRealtimeMillis`,
  `runningSinceEpochMillis`.
- `world_clock`: `zoneId` (IANA); launcher reads wall time itself.
- `alarm`: `targetEpochMillis` (informational; alarms do not tick).

Rendering math lives in `MetroTileTemporalRender` (pure, tested): timers clamp at zero (never
`-00:01`), stopwatch/timer display `H:MM:SS` / `MM:SS`, `use24Hour` honored. `MetroClockFace.parts`
and `MetroClockFace.time` accept a `use24Hour` flag (default false).

`MetroWorldClockCatalog` (`com.metro.system`) is the shared, offline city catalog used by Clock and
the Widgets World Clock widget. `zoneId` is the source of truth (never fixed offsets).

### Metro sounds (`MetroSoundRole` / `MetroSoundContract`)

Metro Settings owns the bundled sound pack and installs it into MediaStore. Apps refer to **semantic
roles**, never filenames or asset paths:

`PHONE_RINGTONE`, `MESSAGE`, `MAIL`, `CALENDAR`, `REMINDER`, `SYSTEM_NOTIFICATION`, `ALARM`, `TIMER`.

- `MetroSoundDescriptor(id, title, role?, category)` + `MetroSoundCategory`
  (`RINGTONE` / `NOTIFICATION` / `ALARM` / `UI`).
- `MetroSoundContract.resolve(resolver, role)` → descriptor or null; `resolveUri(...)` →
  installed `content://` URI or null; `installedPackVersion(...)`.
- Provider (read-only, hosted by Settings): authority `com.metro.settings.sounds`, paths
  `role/<ROLE>` and `pack`. Consumers must fall back safely when it returns null.
- `MetroNotificationChannels.openChannelSettings(context, packageName, channelId)` — deep link to a
  channel's Android settings; `applyInitialSound(context, manager, channel, role)` — adopt the Metro
  sound **only when the channel is first created** (existing channels are user-owned; never mutate).
- Consumer apps must not bundle their own copies of the pack, and must not change system defaults
  (only Settings, with `WRITE_SETTINGS` + user intent).

### MetroBroadcasts

| Action | Payload |
|--------|---------|
| `com.metro.system.THEME_CHANGED` | `theme_mode`, `accent_color`, `font_scale`, `font_family` |

### ContentProvider

Authority: `com.metro.system` — **hosted by Settings** (`MetroSystemPreferencesProvider`)

| URI path | Returns |
|----------|---------|
| `/preferences` | All system preferences |
| `/preferences/{key}` | Single preference value |
| `/start_background` | Cropped Start background JPEG (`openFile`) |
| `/apps` | Installed metro apps registry |

## Rules

- No UI code in this module (types only from metro-ui-android for Color parsing).
- All preference keys are constants — never string literals in apps.
- Settings app is the **writer**; all other apps are **readers** + broadcast receivers.
- Theme change must propagate within 1 frame — use `Flow` or broadcast.

## Verify

```bash
./scripts/verify-toolkit.sh metro-system-sdk
```

## Tests required

- Read/write round-trip for each preference key
- Broadcast received when theme changes
- Intent filter resolution for `LAUNCH_APP`
