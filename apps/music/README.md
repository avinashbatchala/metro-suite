# Music

**Music** is the MetroSuite's extended music player: a Windows-Metro (WP8.1) UI over the
Vivi Music engine. It streams YouTube / YouTube Music anonymously (no account needed), plays
in the background, downloads tracks for offline use, and manages a local library, playlists,
an equalizer, sound recognition and SponsorBlock.

- Application id: `com.metro.music`
- Source namespace: `com.music.vivi` (vendored engine)
- Toolchain: AGP 9.1.1 / Kotlin 2.3.10 / compileSdk 37 / JVM 21; toolkits from `mavenLocal()`
- Design system: `com.metro.ui:metro-ui-android` + `com.metro.system:metro-system-sdk`

## Features

- Anonymous YouTube / YT Music streaming; no Google account required.
- Background playback via a Media3 `MediaSessionService`.
- Offline **downloads** (Media3 `DownloadService`).
- Local library (songs / albums / artists / playlists), search, queue.
- Parametric **equalizer**.
- **Sound recognition** (ShazamKit).
- **SponsorBlock** segment skipping.

## Removed from the upstream port

Lyrics, Last.fm scrobbling, Spotify/JioSaavn import, animated canvas visualizers, artist
video, Android Auto / Cast / GMS, TV/leanback, home-screen widgets, Listen Together,
GitHub OAuth, the in-app OTA updater, and the Material-You dynamic theme.

## Architecture

The engine is retained from upstream (`playback/`, `db/`, `models/`, `viewmodels/`, `eq/`,
`recognition/`, `sponsorblock/`, `innertube`, `kizzy`, `shazamkit`). The UI was replaced with
`ui/metro/` (Metro shell + screens), and `MainActivity` hosts it inside
`MetroSystemTheme { MetroAppPivotShell { MetroMusicApp() } }` while providing
`LocalPlayerConnection` / `LocalDatabase` / `LocalDownloadUtil` / `LocalSyncUtils`.

## Build

Requires JDK 21 and an Android SDK. Toolkits must be in `mavenLocal` (see `AGENTS.md`).

```
./gradlew :app:assembleDebug
./gradlew :app:test
```

## Design references

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

## License

GPL-3.0. See [LICENSE](LICENSE). Fork of
[Vivi Music](https://github.com/vivizzz007/vivi-music) by vivizzz007.
