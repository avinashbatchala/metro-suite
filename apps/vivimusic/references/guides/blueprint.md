# ViviMusic — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

ViviMusic is a Windows-Metro (WP8.1) recreation of the suite's music player, backed by the
Vivi Music engine. The UI must match `apps/music/` (shell, panorama hub, collection pivot,
now-playing pane, queue, search); the feature set is Vivi's (streaming, downloads, EQ,
recognition, SponsorBlock). No Material3 chrome, no album-art background; a flat
`MetroTheme.colors.background` page and the system accent throughout.

## Shell

- Entry: `MetroSplash` → `MetroActivities` → `MetroSystemTheme` → `MetroAppPivotShell` →
  `MetroMusicApp`.
- Root is a `MetroPanorama` with a large lowercase `metro music` brand title.
- `MetroSubpageHost` drives drill-ins; back pops the in-app stack, then the pivot shell
  exits to Start.

## Pages

### Page 1 — Hub (panorama)

- Panes: `collection`, `get music`, `now playing`, `downloads` (horizontal panorama).
- `collection`: vertical `MetroListItem` links (`songs`, `albums`, `artists`, `playlists`)
  using `MetroTextStyle.HubLink`.
- `get music`: accent tiles (search, recognition).
- `now playing`: see Page 2.
- `downloads`: accent tiles / recent downloads entry.

### Page 2 — Now playing (hub pane)

- Empty: "Nothing playing" + hint.
- Otherwise: square album art (flat placeholder), one-line title + "by {artist}",
  `MediaCircleSeekBar` (elapsed / -remaining, hairline track, hollow ring thumb),
  flush-left transport row (prev / play-pause / next) and toggle glyphs for shuffle /
  repeat / queue.
- Background: flat `MetroTheme.colors.background`.

### Page 3 — Collection

- `MetroAppTitle("MUSIC")` + `MetroPivot` `songs | albums | artists | playlists`.
- Dense `MetroListItem` rows, letter-grouped with sticky `MetroLetterTile` headers.
- Tap plays the list (`ListQueue`) / album (`LocalAlbumRadio`).

### Page 4 — Search

- `MetroAppTitle("MUSIC")` + `MetroTextBox`. Local results from the library DB; online
  results from YouTube search. Tap plays immediately.

### Page 5 — Queue

- `MetroAppTitle("queue")` on `secondarySurface`; current track in accent; tap seeks/plays.

### Page 6 — Downloads

- List downloaded songs; per-row remove; empty state `MetroEmptyState`.

### Page 7 — Equalizer

- Profile list from `EQViewModel`; select / delete / disable.

### Page 8 — Recognition

- `MetroBorderButton("listen")` → `MusicRecognitionService`; show status/result; history.

### Page 9 — Settings

- `MetroAppTitle("SETTINGS")` + `MetroPageHeader("vivimusic")`; navigation rows (Downloads,
  Recognition, Equalizer), About/version, SponsorBlock toggles (`MetroToggleSwitch`).

## Images

| Image | Page | Notes |
|-------|------|-------|
| _(captured during AVD verification)_ | | |

## Out of scope (v1)

Lyrics, Last.fm, Spotify/JioSaavn import, animated canvas, Cast/Android Auto, TV, widgets,
Listen Together, OTA updater, dynamic (Material-You) theming.
