# Weather — known reference gaps

Target: mature **Bing/Microsoft Weather for Windows Phone 8.1** (2014) — panorama
`today · daily · hourly · maps · favourites`.

## Copyright / provenance

- The original Bing Weather WP8/8.1 screenshots are **copyrighted and are not committed**.
- `images/` holds **self-authored schematic mockups (SVG)** approximated from the public
  descriptions in [`web-resources.md`](web-resources.md); they are **not** the originals.
- `guides/blueprint.md` is the authoritative spec.

## Fidelity exceptions (documented)

| Item | Status | Notes |
|------|--------|-------|
| Atmospheric Today background art | Partial | Today uses the flat Bing-blue surface + condition glyph; original licence-safe condition artwork (day/night scenes) is a follow-up. Microsoft Bing artwork is **not** redistributed. |
| Rich condition artwork (Daily/Hourly/Tile) | Partial | Uses the monochrome `WeatherGlyph`; a richer original art set mapped from `WeatherCondition` is planned. |
| Ski / mountain resort content | Exception | Historical Bing Weather had resort ski/mountain panes; the backend has no equivalent data. Not faked; documented as a backend limitation. |
| Maps animated viewer + real provider | Partial | Maps is a historical **category list** (Temperature / Doppler Radar / Precipitation / Cloud). A `WeatherMapProvider` abstraction + MapLibre viewer is planned; no provider chosen yet (RainViewer terms forbid commercial use). |
| Wide Live Tile current ↔ five-day face | Partial | The tile exports a `PEEK_CYCLE` text peek (current + 3 days). A structured weather face (art + high/low) rendered by the launcher is a follow-up. |
| Per-location tile deep links | Partial | Secondary tiles exist; primary peek-cycle taps open the app. `metro://weather/location/<id>` deep links are planned. |
| Material sheets/snackbars elsewhere | Partial | The shell no longer renders a Material snackbar (flat Metro status line). Some subpages (Main/BottomSheets, Locations/EditLocation) still use internal Material bottom sheets. |
| Golden screenshots | Missing | `screenshots/golden/` empty; `screenshot_diff` skipped. |

## Historical references to source

`today`, `daily`, `daily-detail`, `hourly`, `maps`, `favourites`, search/add city, context menu,
Live Tile front/back, lockscreen. Sources: Web Design Museum (2013 WP Bing Weather), All About
Windows Phone (2013–2014 Bing Weather articles), Windows Central (Bing Weather v2 tile/lockscreen).
Windows 10 Mobile MSN Weather is **only** a "what not to copy" reference.
