# MetroWeather

A **Windows 10 Mobile (MSN Weather) style** weather app for Android — part of the
**MetroSuite** (`metro-os`).

MetroWeather is the weather companion to the **MetroSuite Start** launcher. It reimagines the
weather experience in the Windows 10 Mobile "Metro" design language: a flat, square,
accent-driven UI built on the shared suite design toolkit.

## Features

- **`now · hourly · daily · places` pivot** with a hero temperature and detail tiles.
- **Hourly temperature graph** — a curve with weather icons and precipitation.
- **Detail tiles** for wind, humidity, pressure, UV index, visibility, precipitation,
  sunrise and sunset.
- **Windows 10 Mobile settings pivots** (appearance / weather / notifications / about) with
  flat Metro dialogs, toggles and lists.
- **Live Tile** — exports a Start tile through the suite tile contract
  (`MetroTileContract` / `MetroTileProvider`) and pins it via
  `MetroIntents.requestPinTile`.
- **Multiple weather sources** (Open-Meteo, Met Norway, NWS, DWD, SMHI, FMI and more) with a
  per-location source picker.

## Design system

The UI is built on the shared suite toolkits:

- `com.metro.ui:metro-ui-android` — Windows Metro composables (theme, app bar, pivot,
  lists, controls).
- `com.metro.system:metro-system-sdk` — system contract (live tiles, pin requests, theme
  broadcasts).

Both live in `toolkits/` at the repo root and are consumed from `mavenLocal()` (this app is
AGP 9, which cannot `includeBuild` the AGP 8 toolkits). Publish them first:

```
(cd ../../toolkits/metro-system-sdk && ./gradlew publishToMavenLocal)
(cd ../../toolkits/metro-ui-android && ./gradlew publishToMavenLocal)
```

## Live tile

The launcher reads the weather tile from the exported `ContentProvider`
(`WeatherTileProvider`, authority `com.metroweather.app.tiles`, metadata
`com.metro.tile.provider`). The "pin to start" action in the Places pivot calls
`MetroIntents.requestPinTile`; `WeatherTileSync` broadcasts a refresh after each weather
update.

## Build

Requires JDK 21 and an Android SDK. Set `sdk.dir` in a local, gitignored `local.properties`.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## License

GPL-3.0. See [LICENSE](LICENSE).

## Attribution

MetroWeather is a fork of [WeatherMaster](https://github.com/PranshulGG/WeatherMaster) by
PranshulGG, licensed under GPL-3.0. The upstream weather-source integrations and forecast
data model originate there.
