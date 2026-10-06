# Weather — reference materials

Target: the mature **Bing/Microsoft Weather app for Windows Phone 8.1** (2014) — panorama
`today · daily · hourly · maps · favourites` (favourites added in the Feb 2014 update). Read
[`guides/blueprint.md`](guides/blueprint.md) first — it is the authoritative page and
interaction spec. This folder informs the blueprint; it does not override it.

## Folder layout

```
references/
├── README.md               # This file — reference set index and usage rules
├── web-resources.md        # Historical sources (design authority) with URLs
├── known-gaps.md           # What is missing / approximated, and workarounds
├── guides/
│   └── blueprint.md        # Authoritative — panorama shell, panes, interaction
└── images/                 # Self-authored SVG schematics (see below)
```

## Images

> **These images are self-authored schematic mockups, not the copyrighted originals.**
> The real Bing Weather screenshots are not redistributed in this repository. The SVGs below
> are flat, text-and-shape approximations drawn from the public descriptions cited in
> [`web-resources.md`](web-resources.md). They document layout intent only; they are not
> pixel-accurate captures and must not be treated as golden references.

All mockups use the WP primary profile viewBox `768×1280`.

| File | Illustrates |
|------|-------------|
| `images/hourly_bing_weather_blue.svg` | Hourly pane: uppercase location label, `hourly  maps  today  daily` heading track (adjacent headings dimmed), ~6 tall flat rows `TIME \| ART \| TEMP \| PRECIP` with thin low-contrast separators on a deep-blue surface |
| `images/daily_bing_weather_blue.svg` | Daily pane: same visual language — day column, artwork, hi/lo, precipitation, flat rows |
| `images/today_bing_weather_blue.svg` | Today pane: hero temperature + condition, hi/lo, flat detail rows |
| `images/hourly_with_appbar_blue.svg` | Hourly pane with the bottom `MetroAppBar` (pin to start / search / current location + `…`) |

## Original source URLs

The originals these schematics approximate (not committed here):

- Windows Experience Blog — *Bing apps arrive on Windows Phone 8*:
  <https://blogs.windows.com/windowsexperience/2013/08/07/bing-apps-arrive-on-windows-phone-8/>
- Bing Search Blog — *Bing Apps Released for Windows Phone 8*:
  <https://blogs.bing.com/search/2013/08/07/bing-apps-released-for-windows-phone-8>
- All About Windows Phone — *Bing Weather offers forecasts with a Modern UI flavour*:
  <https://allaboutwindowsphone.com/flow/item/18107_Bing_Weather_offers_forecasts_.php>

See [`web-resources.md`](web-resources.md) for the full annotated list.

## Agent workflow

1. Read `guides/blueprint.md` for the authoritative layout and Metro rules.
2. Open the matching self-authored schematic in `images/` for visual intent.
3. Read `web-resources.md` for the historical behavior context.
4. Record in [`known-gaps.md`](known-gaps.md) anything the mockups cannot express.
