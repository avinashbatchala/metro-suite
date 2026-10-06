# Music — known gaps

Implementation debt and platform caveats for the MetroSuite port.

## 16 KB page size

The app ships three prebuilt native libraries whose ELF LOAD segments are not 16 KB
page-aligned (built by their upstream projects with a 4 KB linker page size):

- `lib/arm64-v8a/libquickjs.so` — `io.github.dokar3:quickjs-kt` (via `innertubex`)
- `lib/arm64-v8a/libdatastore_shared_counter.so` — `androidx.datastore:datastore-core`
- `lib/arm64-v8a/libandroidx.graphics.path.so` — `androidx.graphics:graphics-path`

Android shows the "app isn't 16 KB-compatible" dialog only for **debuggable** builds; the
`debug` build type is therefore marked `isDebuggable = false` (unminified, debug-signed) to
suppress the dev-only nag while keeping the app fully functional. **Before shipping a release
to 16 KB-page devices**, rebuild these libraries with `-Wl,-z,max-page-size=16384` (or swap to
16 KB-aligned versions) so real 16 KB pages are supported.

(Not related to NewPipe Extractor, which is pure JVM and unused at runtime in this port.)

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
  notification listener; `com.metro.music` is in `MetroConnectedApps.DEFAULT_MUSIC_PACKAGES`).
