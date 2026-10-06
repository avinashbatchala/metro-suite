# Agent instructions — Weather (`com.metroweather.app`)

**Tier 2** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

Weather for metro-os. Application id: `com.metroweather.app`; source namespace stays
`com.pranshulgg.weather_master_app` (the vendored MetroWeather/WeatherMaster engine). A
Windows-Metro (WP8.1 Bing Weather–style) client: `now · hourly · daily · places` pivot, a
monochrome hero, house fact rows, and a cycling Start live tile.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |

This app is **AGP 9** and consumes the AGP 8 toolkits from `mavenLocal()`. Publish them first
(`build-apks.sh`/`verify-app.sh` do this automatically for `weather`):

```
(cd ../../toolkits/metro-system-sdk && ./gradlew publishToMavenLocal)
(cd ../../toolkits/metro-ui-android && ./gradlew publishToMavenLocal)
```

## Screens

| Screen | Navigation pattern | Reference |
|--------|-------------------|-----------|
| Home | `MetroPivot` now / hourly / daily / places, persistent bottom `MetroAppBar` | `references/guides/blueprint.md` |
| Search / Settings / Units / Background updates / About / Licenses / Edit-location | `MetroSubpageHost` subpages | blueprint |

## Metro rules specific to this app

- Use `com.metro.ui.*` chrome only; `androidx.compose.material3.*` is banned (metro lint).
- Flat surfaces: no `Card`/`RoundedCornerShape`/elevation/gradients/alpha-area fills.
- Monochrome condition glyphs (`WeatherGlyph`); facts as `MetroListItem` rows.
- 12dp content margins (`MetroDimens.ScreenHorizontalMargin`).

## Engine (do not rewrite)

Weather data, sources and persistence live under
`app/src/main/java/com/pranshulgg/weather_master_app/{core,data,feature}`. The Metro UI is the
`feature/**` composables + `core/ui`.

## License

GPL-3.0 (see `LICENSE`). Fork of WeatherMaster by PranshulGG.

## Verify

```
../../scripts/verify-app.sh weather
```
