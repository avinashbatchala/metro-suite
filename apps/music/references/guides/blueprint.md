# Music — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Target: the mature 2014–2015 **Windows Phone 8.1 Music / Xbox Music** experience, with the
Vivi/Media3 engine underneath. Not Windows 10 Mobile Groove. No Material chrome.

## Fidelity boundary

- **Stock WP8.1**: recent plays, collection (artists/albums/songs/genres/playlists/radio), get music,
  now playing, queue/up-next, shuffle/repeat, downloads/offline, add-to-collection, playlist
  creation, live tile.
- **Period-compatible extension**: online catalogue (YouTube Music) as the backend, online
  album/artist/playlist discovery, download manager, deep links.
- **Modern engine feature** (kept out of primary navigation): SponsorBlock, recognition, parametric
  EQ, music videos. These live in `…`/Settings, not as equal-level hub links.

## Shell

- Entry: `MetroSplash` → `MetroActivities` → `MetroSystemTheme` → `MetroAppPivotShell` →
  `MetroMusicApp`.
- **One root brand**: a single large lowercase `music`. No `MUSIC` overline, no `metro music`.
- Navigation is a **real in-app stack** (`mutableStateListOf<MetroRoute>`): push on drill-in, pop on
  Back, preserving source context (Search → Artist → Album → Back = Artist → Back = Search state).
- `MetroSubpageHost` renders the current route; Back pops the stack, then exits to Start at the root.

## Root panorama

Four panes (`MetroPanorama`): **recent plays · collection · get music · now playing**.
No downloads pane, no settings pane.

### recent plays

Aggregated (not raw play-log noise): recently played **albums** and **artists** (deduped by id,
most-recent first, capped). Tap opens the album/artist. "Nothing played yet." when empty.

### collection

Large typography-first HubLinks, each opening a dedicated page (no nested pivot):
`artists · albums · songs · genres · playlists · radio`.

- **artists** → letter-grouped list → Artist detail.
- **albums** → letter-grouped list → Album detail (tap opens detail, does not auto-play).
- **songs** → letter-grouped list; tap plays the list; long-press context menu.
- **genres** → catalogue moods & genres (period-compatible extension) → station (playlist page).
- **playlists** → editable playlists; app-bar `+` creates a playlist.
- **radio** → stations built from recent playback (album radio), continuous backend queue.

### get music

Discovery (no Settings/Recognition tiles): `search · browse by genre · new releases`.
Backend is the streaming catalogue; the product name shown is Metro Music. Search stays on the app bar.

### now playing

See below.

## Now playing

- Empty: "Nothing playing" + hint.
- Playing: `title`, `by Artist`, album art, then a row with **shuffle / repeat** (accent when on)
  and a **queue** affordance; a full-width scrubber; `Up next: <track> — <artist>`; transport
  **previous / play-pause / next**.
- **Vertical album-art swipe** changes track (up = next, down = previous) — the authentic late-WP8.1
  gesture. No bouncy Material animation.
- **No five-icon side rail.** Like / download / add-to… / start radio / share live in the app-bar
  ellipsis for this pane.

## Collection vs online

- Collection membership is explicit: caching an entity for playback does **not** add it to the
  Collection. Online album → `add to collection` (`inLibrary`); artist → subscribe (`bookmarkedAt`);
  playlist → save (`bookmarkedAt`).
- Local and online Album/Artist/Playlist details share the same visual grammar (artwork, title,
  subtitle, `play`/`shuffle` text buttons, `…` for secondary commands).

## Search

One coherent search over **Collection + online**, with a single type selector
`all · songs · albums · artists · playlists` (videos are not a primary Music pivot). In `all`, local
results appear under `in your collection`, online under `online results`. Debounced (400 ms);
preserves query/filter when drilling into a result. No "local vs online" pre-choice.

## App bars

Context-sensitive; there is no global `search / queue / settings` bar on every page.

- Root, non-now-playing panes: `search` icon + `…` (`downloads`, `identify song`, `equalizer`,
  `settings`).
- Root, now-playing pane: `queue` icon + `…` (`download`/`remove download`, `identify song`,
  `equalizer`, `settings`).
- Playlists: `+` (new playlist) + `search`.
- Now playing / album / artist / playlist: primary `play`/`shuffle` text buttons + `…`.

## Playlists

- `+` → `NEW PLAYLIST` overline + `playlist settings` title, name field, **keep playlist offline**
  toggle, save/check.
- Playlist detail: play/shuffle; `…` → keep offline (downloads its songs) — app-local preference, not
  a Room column, so no migration.
- Offline is a download concept backed by Media3; disabling it never silently deletes audio.

## Downloads

First-class capability, **not** a panorama pane: reached from `… → downloads`, plus contextual
download commands. The screen manages downloaded/in-progress/failed items.

## Recognition · Equalizer · Settings

- Recognition: `… → identify song`; results are actionable (play / add / search artist); history rows
  are tappable. Microphone permission on demand.
- Equalizer: `… → equalizer` (technically valuable engine feature; WP8.1 pointed to system audio).
- Settings: configuration only (playback, downloads, SponsorBlock, audio/EQ link, recognition
  preferences, pin to Start, about). It does not duplicate the app hierarchy.

## Live tile / MediaSession

Metro live tile + Media3 MediaSession preserved: track/artist/artwork/transport flow to the launcher,
Metro Volume and lockscreen. The launcher owns tile rendering; no per-second tile updates.

## Local device music

The port is stream/download oriented; it does not (yet) scan Android MediaStore audio. This is not
claimed as full local-library support. GrapheneOS-compatible; no broad filesystem access.

## Engine boundary

Media3 playback, DownloadService, Innertube extraction, SponsorBlock, Room, queues, PO-token
handling, stream resolution and Shazam are used as-is. Presentation consumes stable domain adapters.

## Images

`images/` is the visual reference set (WP8.1 Music). It does not override this file.

## Out of scope

Lyrics, Last.fm, Spotify/JioSaavn import, animated canvas, Cast/Android Auto, TV, widgets, Listen
Together, OTA updater, dynamic theming, Windows 10 Groove styling.
