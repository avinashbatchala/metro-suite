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
- Offline **downloads** (Media3 `DownloadService`), including per-playlist **keep offline**.
- Collection (artists / albums / songs / genres / playlists / radio), search, queue.
- Parametric **equalizer**; **sound recognition** (ShazamKit); **SponsorBlock** segment skipping.

## UI model (mature WP8.1 Music)

The app opens to a **root panorama** with a single `music` brand and four panes:
**recent plays · collection · get music · now playing**. Navigation is a real in-app stack.

- `collection` links (artists / albums / songs / genres / playlists / radio) each open a dedicated
  page — no nested Collection pivot.
- `search` is one combined experience over the Collection **and** the online catalogue with a single
  type selector (`all · songs · albums · artists · playlists`).
- `now playing` keeps the **vertical album-art swipe**, an `Up next` line, shuffle/repeat/queue, and
  moves like/download/add-to/radio/share into the app-bar ellipsis (no five-icon rail).
- Downloads, recognition and the equalizer are contextual (`…` / Settings), not panorama panes.
- Collection membership is explicit: caching online metadata for playback does not add it to the
  Collection.

See [`references/guides/blueprint.md`](references/guides/blueprint.md) for the full spec.

## Fidelity boundary

- **Stock WP8.1** surfaces: recent plays, collection, get music, now playing, queue, playlists,
  downloads/offline, live tile.
- **Period-compatible extension**: the online catalogue (YouTube Music) powering discovery, plus the
  download manager and deep links.
- **Modern engine features** (kept out of primary navigation): SponsorBlock, recognition, equalizer,
  music videos.

## Local device music

The port streams and downloads; it does **not** scan Android `MediaStore` audio. Full local-library
support is not claimed.

## Architecture

The engine is retained from upstream (`playback/`, `db/`, `models/`, `viewmodels/`, `eq/`,
`recognition/`, `sponsorblock/`, `innertube`, `kizzy`, `shazamkit`). The UI is `ui/metro/`
(`MetroMusicApp.kt` shell + stack, `MetroNowPlaying.kt`, `MetroSearch.kt`, `MetroGenres.kt`,
`MetroOnlineDetail.kt`, `MetroQueue.kt`, `DownloadsScreen.kt`, `RecognitionScreen.kt`, `EqScreen.kt`,
`SettingsScreen.kt`). `MainActivity` hosts it inside
`MetroSystemTheme { MetroAppPivotShell { MetroMusicApp() } }` while providing
`LocalPlayerConnection` / `LocalDatabase` / `LocalDownloadUtil` / `LocalSyncUtils`.

## Removed from the upstream port

Lyrics, Last.fm scrobbling, Spotify/JioSaavn import, animated canvas visualizers, artist
video, Android Auto / Cast / GMS, TV/leanback, home-screen widgets, Listen Together,
GitHub OAuth, the in-app OTA updater, and the Material-You dynamic theme.

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
