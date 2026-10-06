# Widgets

**Package:** `com.metro.widgets`  
**Tier:** 2

## Status

Implemented — Start-style 4-column widget catalog (Time, Battery, Notifier, Analog clock, Torch, Lock, World clock). Long-press pins to Start; World clock opens a city-selection page on tap.

## App role

Catalog of custom homescreen widgets rendered as live tiles. Opens to a black Start-like grid (4 columns only). Footprints: 1×1, 2×2, 2×4 (wide).

## Screen inventory

1. **Widget catalog** — `MetroAppTitle` `widgets` + packed tile grid  
   - Time (2×4 digital clock — AM/PM + square colon; no weather)  
   - Battery (1×1 vertical glyph + digits — charge level)  
   - Notifier (2×2 Start-style flip through tray notifications; tap opens the visible app)  
   - Analog clock (1×1 flat dial)
   - Torch (1×1 LED flashlight toggle)
   - Lock (1×1 padlock — tap locks device)
   - World clock (2×4 wide — up to 3 city rows, ticking locally)

2. **World Clock configuration** — `choose cities`; search + Metro checkmarks, 1–3 cities

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

## World Clock widget

- **Catalog item:** `WorldClock` (`id = world_clock`, footprint 2×4 wide, grid row 4). Tap opens the
  configuration page; long-press pins to Start.
- **Cities:** shared offline catalog `MetroWorldClockCatalog` (`com.metro.system`) — IANA `ZoneId`
  is the source of truth (DST automatic). Search matches city / country / zone / alias.
- **Selection 1–3, de-duplicated:** pure rules in `WorldClockSelectionLogic`; persisted in
  `WorldClockWidgetStore` (app-private `SharedPreferences` `widgets_world_clock`, `cities` CSV).
  Invalid ids dropped; empty falls back to the catalog defaults (London / New York / Tokyo).
- **Tile contract:** `MetroTileWidgetFace(kind = WORLD_CLOCK, worldClocks = [ …≤3 name+zoneId ])`.
  The **launcher renders and ticks** the city times locally — Widgets does **not** push a per-minute
  update. Tap the pinned tile is display-only; edit cities from the catalog.
- **12/24-hour:** honors the device setting (`DateFormat.is24HourFormat`); `MetroClockFace.time`
  and `MetroClockFace.parts` take a `use24Hour` flag (existing Time / Analog faces unaffected).

## System functions and contracts

- Theme via `MetroSystemTheme` / `MetroPreferences`
- Battery: `ACTION_BATTERY_CHANGED`
- Clock: `ACTION_TIME_TICK` / time / timezone changed
- Notifier: `NotificationListenerService` → tray peek queue (Start flip timing); tap opens the notifying app for the visible peek (access settings when denied)
- Torch: `CameraManager.setTorchMode` (runtime `CAMERA`; optional flash feature)
- Lock: `MetroLockscreen.requestLock` → lockscreen a11y `GLOBAL_ACTION_LOCK_SCREEN`
- Pin: long-press → `MetroIntents.requestPinTile` (`com.metro.widgets` + widget id + catalog size)
- Start faces: `MetroTileWidgetFace` via `WidgetsTileProvider` (launcher renders); taps via `WidgetsTileActions.ACTION_TAP`
- World clock: `MetroWorldClockCatalog` (shared), `WorldClockSelectionLogic` + `WorldClockWidgetStore`; face kind `WORLD_CLOCK` ticks locally in the launcher

## UI guardrails

- Toolkit-first (`MetroAppTitle`, `MetroTheme`, `MetroText`)
- No Material components
- Match Start gutters (12dp) and gap (8dp)
- `Modifier.metroNavBarPadding()` on root

## Data and state model

- `WidgetTileSize`: OneByOne / TwoByTwo / TwoByFour
- `WidgetCatalog`: fixed placements for the six faces
- `BatterySnapshot` + `ClockFaceParts` (digital + hand angles) unit-tested
- `TorchController` for LED availability / toggle

## Commands

```bash
cd apps/widgets

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

../../scripts/verify-app.sh widgets
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Stock Time Start tile | None shipped | Digital clock face from third-party WP8.1 precedent |
| Stock Torch Start tile | Action Center quick action only | 1×1 catalog toggle via `CameraManager` |
| Stock Lock Start tile | None | 1×1 padlock via lockscreen a11y `GLOBAL_ACTION_LOCK_SCREEN` |

## Agent postmortem

_None._
