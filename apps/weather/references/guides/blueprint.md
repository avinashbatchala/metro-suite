# Weather — blueprint

**Authoritative spec.** Windows Phone **8 / 8.1 Bing Weather** fidelity — a
`today · daily · hourly · maps` panorama over the MetroWeather engine.

## Shell

- Entry: `MetroSplash` → `MetroActivities` → `MetroSystemTheme` → `MetroAppPivotShell` →
  `WeatherShell`.
- `WeatherShell` is a **`MetroPanorama`** with the pane order
  `today · daily · hourly · maps · favourites`. Section headings sit in the top heading track and
  **adjacent headings peek** into view (`MetroPanoramaContentPeek`); tapping a heading jumps to
  that pane.
- Above the heading track, an **uppercase location label** (e.g. `SEATTLE, WA`) in small
  secondary text. The label reflects the currently selected location, not the pane.
- A persistent bottom **`MetroAppBar`**. Primary icons: **pin to start**, **search**,
  **current location**; the `…` overflow holds `refresh`, `settings`. System Back pops any
  subpage, then exits on Home. Refresh is **not** a permanent primary command.

## Visual language (all panes)

- **Flat deep-blue Bing surface.** One continuous background fill; no cards, elevation or
  Material chrome. Forecast rows may use a **lighter Bing-blue rectangular surface** (the
  historical Daily row), which is not a "card".
- Rows are full-bleed with **thin, low-contrast separators**; tall rows; artwork centered.
- Type hierarchy: secondary labels in dim secondary text; the primary value is the strongest
  element (large and light). Precipitation is **right-aligned** with a small drop glyph.
- Only `com.metro.ui.*` chrome; `androidx.compose.material3.*` is banned on visible screens.
- 12/24-hour follows the **system** clock (`DateFormat.is24HourFormat`), never a Weather-local
  preference.

## Panes

### Today (atmospheric dashboard, not a settings list)
- Uppercase location label, then the `today` heading.
- **Atmospheric condition background/art** is the primary visual (original licence-safe art;
  never Microsoft Bing artwork).
- Current temperature (large) + condition; a separator; then `Today` (high/condition) and
  `Tonight` (low/condition).
- A **compact two-column metric grid** (feels-like, humidity, visibility, pressure, wind,
  UV index, precipitation) — not a vertical list of rows. Only valid data is shown.
- Subtle update line: `Updated 14:05 · <active provider>`.

### Daily
- 10-day list. Each row is a **lighter Bing-blue rectangular surface**:
  **`DAY | WEATHER ART | HIGH / LOW | PRECIPITATION % + droplet`**.
- Day left, art centered, high visually stronger than low, precipitation right-aligned.
- Tapping a day opens the **detailed-day** panoramic subpage (below).

### Daily detail (subpage)
- Top track of dates (`fri 9 · sat 10 · sun 11 …`); horizontal swipe moves forecast days.
- Day / night columns with art, temperature and condition, plus wind/humidity and
  sunrise/sunset/precipitation.
- **HOURLY FORECAST** graph: temperature line + points, hour labels, precipitation bars/percent
  (custom Canvas). This is the only place the graph appears.

### Hourly
- Vertical list `TIME | WEATHER ART | TEMPERATURE | PRECIPITATION` for the remainder of the
  current day. Rich condition art; actual hour labels (no `now` unless a reference proves it).

### Maps
- A **scrollable list of map categories** (Regional Temperature, Doppler Radar, Regional
  Precipitation, Cloud, Satellite) on lighter Bing-blue rows with a preview thumbnail.
- A `WeatherMapProvider` abstraction supplies layers/previews/frames; layers unavailable for a
  location are hidden or shown disabled. Tapping opens an animated Metro map viewer (MapLibre or
  another de-Googled renderer). No Google Maps.

### Favourites
- Saved-location cards with condition art, name, temperature and condition; the home/current
  location is subtly indicated.
- Tap selects the location; **long-press** opens a Metro context menu
  (`remove · set home · pin to start`). Not a Settings-style list.

## Explicitly out of scope

- **No** Material cards, tabs, FAB, pills, ripples, bottom sheets, snackbars or elevation.
- **No** daily range bars (the modern visualization is not in the target).
- Ski/mountain resort content (backend has no equivalent — documented fidelity exception).
- Colored/high-detail condition art is **replaced by original licence-safe weather art** (not
  Microsoft Bing artwork).

## Live tile

Structured Weather face (SDK), launcher-rendered:
- **Wide, face 1** — current: location, temperature, condition, high/low, precipitation, wind,
  weather art.
- **Wide, face 2** — five-day forecast: weekday, art, high/low.
- **Medium** — location + temperature + condition + high/low. **Small** — temperature + art.
- Weather uses its own **blue/weather-art identity**, not the suite accent. Each saved location
  pins independently and deep-links to `metro://weather/location/<id>`.
