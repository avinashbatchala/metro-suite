# Weather — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and
interaction spec.

Agents must read the blueprint before changing UI in `apps/weather/`.

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

The Metro UI mirrors the suite's other apps (`apps/calendar`, `apps/music`); match their
components, spacing and motion.
