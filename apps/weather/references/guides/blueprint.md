# Weather — blueprint

**Authoritative spec.** Windows-Metro (WP8.1 Bing Weather–style) client.

## Shell

- Entry: `MetroSplash` → `MetroActivities` → `MetroSystemTheme` → `MetroAppPivotShell` →
  `WeatherShell`.
- `WeatherShell` keeps a persistent bottom `MetroAppBar` and swaps content via
  `MetroSubpageHost`; system Back pops subpages, exits on Home.

## Pages

### Home (pivot)
- `MetroPivot` titles `now · hourly · daily · places`, header = `MetroAppTitle("weather")`.
- **now:** monochrome `WeatherGlyph` + temperature (`HubTitle`) + condition; inline alerts;
  a **DETAILS** section of `MetroListItem` fact rows (wind/humidity/pressure/UV/visibility/
  precipitation/sunrise/sunset).
- **hourly:** horizontally-scrollable strip of flat `secondarySurface` tiles (time, glyph, temp).
- **daily:** dense rows (weekday, condition, hi/lo) with a flat range bar.
- **places:** saved locations as rows; add/edit/pin-to-start.
- Bottom `MetroAppBar`: Search, Settings, Refresh.

### Subpages
- Search, Settings, Units, Background updates, About, Terms/Privacy/License, Edit-location —
  house headers (`MetroAppTitle` + `MetroPageHeader`), no top bar.

## Live tile

`WeatherTileProvider` exports a `PEEK_CYCLE` widget face cycling four text faces: current
conditions, then the next three days (weekday + hi/lo + condition). Accent background.

## Out of scope (v1)

Material cards/gradients, colored condition art, Appearance/Language/Backup/Notifications
screens, weather widgets.
