# ViviMusic — known gaps

Implementation debt and platform caveats for the MetroSuite port.

## 16 KB page size

On 16 KB-page devices (e.g. Pixel 10a) the debug build shows an "Android app compatibility"
warning because three prebuilt native libraries are not 16 KB-aligned:

- `lib/arm64-v8a/libquickjs.so` (NewPipe Extractor)
- `lib/arm64-v8a/libdatastore_shared_counter.so`
- `lib/arm64-v8a/libandroidx.graphics.path.so`

The app still runs. Before a release on 16 KB devices, upgrade the offending dependencies
(NewPipe Extractor / datastore / androidx.graphics) or strip+realign the `.so` files so the
RELR segments are 16 KB aligned.

## Reference images / golden screenshots

`references/images/` is empty and `screenshots/golden/` has no baselines yet. The UI mirrors
`apps/music/` (which is the accepted visual reference); capture golden screenshots from the
running app on the lumia-925 profile when convenient.

## Deliberately removed features

Lyrics, Last.fm scrobbling, Spotify/JioSaavn import, animated canvas visualizers, artist
video, Android Auto / Cast / GMS, TV/leanback, home-screen widgets, Listen Together, GitHub
OAuth, in-app OTA updater, and Material-You dynamic theming were removed by design (see
`README.md`). They will not be re-added.

## Minimal feature screens

The Equalizer and Recognition screens are functional but intentionally minimal (profile
list / single recognize action + history) rather than ports of Vivi's richer M3 screens, to
match the Metro design language.

## Out of scope for v1

- Per-app theme selection (the suite theme/accent from `MetroPreferences` is used).
- Lyrics/now-playing metadata broadcast (the launcher reads the Media3 session via its
  notification listener; `com.metro.vivimusic` is in `MetroConnectedApps.DEFAULT_MUSIC_PACKAGES`).
