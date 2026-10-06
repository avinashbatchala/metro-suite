# Agent instructions — Music (`com.metro.music`)

**Tier 1** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

Music for metro-os. Application id: `com.metro.music`; source namespace stays
`com.music.vivi` (the vendored Vivi Music engine). It is a Metro-restyled YouTube Music
client: anonymous YT/YT Music streaming (no account required), background playback,
offline downloads, local library, playlists, search, queue, equalizer, sound recognition
and SponsorBlock. Lyrics, Last.fm, Spotify/JioSaavn import, animated canvas, Cast/Android
Auto, TV, widgets, in-app updater and Listen Together were removed in the port.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell (if Tier ≥ 1) | Yes |

This app is **AGP 9** and consumes the AGP 8 toolkits from `mavenLocal()`. Publish them
first (`build-apks.sh`/`verify-app.sh` do this automatically for `vivimusic`):

```
(cd ../../toolkits/metro-system-sdk && ./gradlew publishToMavenLocal)
(cd ../../toolkits/metro-ui-android && ./gradlew publishToMavenLocal)
```

## Screens to implement

| Screen | Navigation pattern | Reference |
|--------|-------------------|-----------|
| Root (panorama) | `MetroPanorama` — recent plays / collection / get music / now playing | `references/guides/blueprint.md` |
| Collection categories | Direct routes: artists / albums / songs / genres / playlists / radio (no pivot) | blueprint |
| Now playing | Root pane (not a route); vertical art swipe, up-next, transport | blueprint |
| Search | Subpage; one combined Collection + online selector | blueprint |
| Queue / Downloads | Subpages | blueprint |
| Equalizer / Recognition / Settings | Contextual (`…` / Settings), not panorama panes | blueprint |

## WP8.1 rules specific to this app

- One root brand: a single large `music`. Do not prepend `MUSIC` to every screen.
- No permanent view tabs / bottom navigation / Material chrome; context-sensitive app bars only.
- Collection membership is explicit; caching online metadata must not add it to the Collection.
- Keep the real in-app navigation stack (push/pop); preserve source context on Back.
- Use `com.metro.ui.*` chrome only; `androidx.compose.material3.*` is banned (metro lint).
- Flat `MetroTheme.colors.background`; never an album-art/gradient background (except historically
  appropriate artist context).
- App bar at bottom; no FAB; `Modifier.metroNavBarPadding()` on screen roots;
  `MetroBorderButton` must not `fillMaxWidth()`.

## Engine (do not rewrite)

Playback/library/search/downloads are the vendored Vivi engine under
`app/src/main/kotlin/com/music/vivi/{playback,db,models,viewmodels,eq,recognition,sponsorblock,innertube}`.
The Metro UI is `app/src/main/kotlin/com/music/vivi/ui/metro/`. `MainActivity` binds
`MusicService`, builds `PlayerConnection`, and provides composition locals
(`LocalPlayerConnection`, `LocalDatabase`, `LocalDownloadUtil`, `LocalSyncUtils`).

## License

GPL-3.0 (see `LICENSE`). Fork of [Vivi Music](https://github.com/vivizzz007/vivi-music).

## Verify

```
../../scripts/verify-app.sh vivimusic
```
