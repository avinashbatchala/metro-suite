# Weather — blueprint

**Authoritative spec.** Windows Phone **8 / 8.1 Bing Weather** fidelity — a
`today · daily · hourly · maps` panorama over the MetroWeather engine.

## Shell

- Entry: `MetroSplash` → `MetroActivities` → `MetroSystemTheme` → `MetroAppPivotShell` →
  `WeatherShell`.
- `WeatherShell` is a **`MetroPanorama`** with the pane order `today · daily · hourly · maps`.
  Section headings sit in the top heading track and **adjacent headings peek** into view
  (`MetroPanoramaContentPeek`); tapping a heading jumps to that pane.
- Above the heading track, an **uppercase location label** (e.g. `SEATTLE, WA`) in small
  secondary text. The label reflects the currently selected location, not the pane.
- A persistent bottom **`MetroAppBar`** with icon buttons: **locations**, **search**,
  **current location**, **refresh**, **ellipsis** (overflow). The bar uses the suite chrome
  overlay; system Back pops any subpage, then exits on Home.

## Visual language (all panes)

- **Flat deep-blue Bing surface.** One continuous background fill; no panels, cards, gradients
  or elevation.
- Rows are full-bleed with **thin, low-contrast separators**; tall rows; artwork centered.
- Type hierarchy: secondary labels in dim secondary text; the primary value is the strongest
  element (large and light). Precipitation is **right-aligned** with a small drop glyph.
- Only `com.metro.ui.*` chrome; `androidx.compose.material3.*` is banned (metro lint).
- 12dp content margins (`MetroDimens.ScreenHorizontalMargin`).

## Panes

### Today
- Uppercase location label, then the `today` heading.
- Hero: large current temperature + condition text; secondary hi/lo line.
- A flat section of detail rows (feels-like, wind, humidity, precipitation, UV, visibility,
  sunrise/sunset) as `MetroListItem`-style full-bleed rows with thin separators.

### Daily
- Same language as hourly. A 10-day list; each flat row reads
  **`DAY | WEATHER ART | HI/LO | PRECIPITATION`**.
- Day column left-aligned, artwork centered, hi/lo the strongest text, precipitation
  right-aligned with a drop glyph; thin separators, no cards.

### Hourly
- Flat deep-blue surface. Rows read
  **`TIME | WEATHER ART | TEMPERATURE | PRECIPITATION`**.
- Tall rows, thin low-contrast separators, no cards / pills / rounding.
- Time left-aligned; artwork centered; **temperature the strongest element (large, light)**;
  precipitation right-aligned with a small drop glyph.
- Coverage is the remainder of the current day (hour by hour), generic condition + rain
  probability per the historical app.

### Maps
- Animated map categories for the next 24 h (temperature, precipitation, cloud cover,
  satellite/radar) in the historical app.
- **v1: no map SDK.** Render a flat placeholder surface in the same deep-blue language.
  Do **not** add a map provider dependency.

## Explicitly out of scope

- **No** Material cards, tabs, FAB, pills, ripples or elevation.
- **No** large modern temperature graph on the hourly pane — hourly is a flat row list, not a
  chart.
- Colored/high-detail condition art; Appearance / Language / Backup / Notifications screens;
  home-screen widgets.

## Live tile

`WeatherTileProvider` exports a `PEEK_CYCLE` widget face cycling four text faces: current
conditions, then the next three days (weekday + hi/lo + condition). Accent background.
