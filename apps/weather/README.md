# Weather

**Package:** `com.metroweather.app` (namespace `com.pranshulgg.weather_master_app`) · **Tier 2**

## Status

MetroSuite **Weather** — a recreation of the mature **Windows Phone 8.1 Bing/MSN Weather**
experience (early–late 2014) on top of the **WeatherMaster** Android weather engine.

Presentation target: **WP8.1 Bing Weather** (`today · daily · hourly · maps · favourites`
panorama, atmospheric Today, rectangular forecast rows, weather-rich favourites, weather-art
Live Tile). **Not** Windows 10 Mobile MSN Weather.

## Architecture

```
weather providers (Open-Meteo, Met Norway, DWD, SMHI, NWS, …)
        ↓
normalized Weather model (Weather / WeatherCurrent / WeatherHourly / WeatherDaily)
        ↓
WP8.1 presentation layer (Metro)
```

The backend is preserved: Room cache, provider adapters, forecast normalization, units,
background refresh, location search, WorkManager refresh, alerts, and the Metro live-tile
provider. The UI does not branch per provider except for attribution/capability surfaces.

## Features

- **Root panorama** `today · daily · hourly · maps · favourites`.
- **Today** — atmospheric condition background, current temperature/condition, today/tonight,
  and a compact two-column metric grid (feels-like, humidity, pressure, visibility, wind, UV,
  precipitation, sunrise/sunset) with correct units.
- **Daily** — rectangular Bing-blue forecast rows (day · art · high · low · precipitation%) that
  drill into a detailed-day view with an hourly graph.
- **Hourly** — vertical list `time · weather art · temperature · precipitation`.
- **Maps** — historical category list (regional temperature / precipitation / radar) with a
  provider abstraction; animated viewer where a licensed provider exists.
- **Favourites** — weather-rich saved-location cards; long-press → remove / set home / pin to start.
- **Live Tile** — structured current ↔ five-day weather face rendered by the launcher.
- Multiple weather providers (default chosen automatically; advanced override only).

## Design system

`com.metro.ui:metro-ui-android` (Metro composables) and `com.metro.system:metro-system-sdk`
(live tiles, pin requests). Both live in `toolkits/` and are consumed from `mavenLocal()` (this
app is AGP 9 and cannot `includeBuild` the AGP 8 toolkits). Publish them first:

```
(cd ../../toolkits/metro-system-sdk && ./gradlew publishToMavenLocal)
(cd ../../toolkits/metro-ui-android && ./gradlew publishToMavenLocal)
```

No Material UI is shown on any visible screen (no bottom sheets, snackbars, dialogs, cards,
switches or radio buttons). No Google Play Services dependency.

## Live tile

The launcher reads the weather tile from the exported `ContentProvider` (`WeatherTileProvider`,
authority `com.metroweather.app.tiles`, metadata `com.metro.tile.provider`). Each saved location
can be pinned independently via `MetroIntents.requestPinTile`; `WeatherTileSync` refreshes after
a forecast update. Pinned locations deep-link to `metro://weather/location/<id>`.

## Build

Requires JDK 21 and an Android SDK (`local.properties` → `sdk.dir`).

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
cd ../.. && ./scripts/verify-app.sh weather
```

## Attribution

Fork of [WeatherMaster](https://github.com/PranshulGG/WeatherMaster) by PranshulGG (GPL-3.0).
Provider attribution is shown in the app per each provider's terms.
