# Music — known gaps

Implementation debt, deferred work and platform caveats for the MetroSuite music port.

## Reference images / goldens

`references/images/` is empty and `screenshots/golden/` has no baselines. Capturing authentic
WP8.1 Music screenshots and generating goldens requires a capture environment that is not available
here. The implementation follows `guides/blueprint.md` and the historical behavior notes in
`web-resources.md`. Capture goldens from the running app on the lumia-925 profile when convenient.

## Deferred (presentation extensions beyond this pass)

- **`add to…` chooser**: implemented for songs (local + online), albums (local + online) and via the
  playlist screen; still to add on artist/playlist surfaces and a per-track album context menu.
- **Recognition history rows** are still display-only (result actions exist for the live result).
- **Online detail back affordance** relies on system Back (no on-screen back button on those pages).
- **Radio** exposes recent-playback album stations; artist/song-seeded radio stations are reachable
  from context menus but there is no dedicated station manager.
- **Genres** are the catalogue's moods & genres (the DB has no local genre column since the Vivi
  port).

## Listen history threshold

Recent Plays is fed by the engine's `event` history. The threshold was lowered from 30 s to **1 s**
(`MusicService.onPlaybackStatsReady`) so a played track appears quickly; the currently-playing track
is also shown immediately in the Recent Plays pane.

## Local device music

The port is stream/download oriented and does **not** scan Android `MediaStore` audio (no
`READ_MEDIA_AUDIO`). README/blueprint do not claim full local-library support. GrapheneOS compatible;
no broad filesystem access.

## 16 KB page size

Three prebuilt native libraries have non-16 KB-aligned LOAD segments
(`libquickjs.so`, `libdatastore_shared_counter.so`, `libandroidx.graphics.path.so`). The `debug`
build type is marked `isDebuggable = false` to suppress the dev-only nag. Rebuild these with
`-Wl,-z,max-page-size=16384` before shipping release to 16 KB-page devices.

## Deliberately removed features

Lyrics, Last.fm scrobbling, Spotify/JioSaavn import, animated canvas visualizers, artist video,
Android Auto / Cast / GMS, TV/leanback, home-screen widgets, Listen Together, GitHub OAuth, in-app
OTA updater, and Material-You dynamic theming (see `README.md`).
