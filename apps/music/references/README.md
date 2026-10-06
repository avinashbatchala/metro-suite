# Music — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and interaction
spec.

Agents must read the blueprint before changing UI in `apps/music/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md       # External URLs + historical behavior notes
├── guides/
│   └── blueprint.md       # Authoritative — pages, layout, interactions
├── images/                # Authentic WP8.1 Music captures (currently empty — see known-gaps.md)
└── known-gaps.md          # Implementation debt / missing captures
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `web-resources.md` — historical behavior
4. `images/` — visual reference (does not override the blueprint)

## Screens

| Screen | Notes |
|--------|-------|
| Root panorama (recent plays / collection / get music / now playing) | Four panes; single `music` brand |
| Collection category pages | artists / albums / songs / genres / playlists / radio |
| Genres | Catalogue moods & genres (period-compatible extension) |
| Radio | Stations from recent playback |
| Search | Single combined selector (Collection + online) |
| Now playing | Art swipe, up-next, transport |
| Album / Artist / Playlist detail | Shared visual grammar (local + online) |
| Queue / Downloads | Up-next; download management |
| New playlist / playlist settings | `NEW PLAYLIST` + `playlist settings`, keep offline |
| Live tile | Launcher-rendered |

## Images

`images/` is intended for authentic WP8.1 Music captures. It is currently **empty** (no capture
tooling in this environment). Implementation follows `guides/blueprint.md` + the behavior notes in
`web-resources.md`. Golden screenshots are captured from the running app into `../../screenshots/`
during AVD verification.
