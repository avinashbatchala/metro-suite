# Music — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and
interaction spec.

Agents must read the blueprint before changing UI in `apps/music/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md       # External URLs (supplementary)
├── guides/
│   └── blueprint.md       # Authoritative — pages, layout, interactions
├── images/                # Visual reference only (does not override blueprint)
└── known-gaps.md          # Optional — track implementation debt
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `images/` — visual polish and inspiration
4. `web-resources.md` — external docs when needed

## UI source of truth

The Metro UI mirrors the suite's music player at `apps/music/` (shell, panorama hub,
collection pivot, now-playing pane, queue, search). Match its components, spacing and
motion; the engine and feature set come from the Vivi port described in the blueprint.

## Images

`images/` maps to the pages in `guides/blueprint.md`. Screenshots of the running app are
captured into `../../screenshots/` during AVD verification.
