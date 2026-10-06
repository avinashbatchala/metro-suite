# Weather — known reference gaps

Target: historical **Bing Weather for Windows Phone 8 / 8.1** (panorama
`today · daily · hourly · maps`).

## Copyright / provenance

- The original Bing Weather WP8/8.1 screenshots are **copyrighted and are not committed** to
  this repository.
- The files under `images/` are **self-authored schematic mockups (SVG)** — flat rectangles,
  text and simple weather glyph shapes — approximated from the public descriptions cited in
  [`web-resources.md`](web-resources.md). They are **not** the originals and are **not**
  pixel-accurate captures.
- `guides/blueprint.md` is the authoritative spec; the schematics only illustrate its layout.

## Low-fidelity / missing

| Missing / low-fidelity | Should show | Workaround |
|------------------------|-------------|------------|
| No original hourly capture (Wealden / Seattle) committed | Deep-blue hourly pane, tall flat `TIME \| ART \| TEMP \| PRECIP` rows | `images/hourly_bing_weather_blue.svg` schematic + blueprint § Hourly pane |
| No original daily capture committed | 10-day flat rows: day, art, hi/lo, rain probability | `images/daily_bing_weather_blue.svg` schematic + blueprint § Daily pane |
| No original today capture committed | Hero temperature/condition + flat detail rows | `images/today_bing_weather_blue.svg` schematic + blueprint § Today pane |
| No original app bar capture committed | Bottom application bar icon row | `images/hourly_with_appbar_blue.svg` schematic + blueprint § Shell |
| **Maps pane has no map SDK** | Animated temperature / precipitation / cloud-cover / radar map tiles for the next 24 h | Out of v1 scope — render a static placeholder surface; do **not** add a map provider dependency |
| Live Tile faces not captured | 2×2 / 4×2 tile states per the WP8 app | Document as implementation debt; no golden images |
