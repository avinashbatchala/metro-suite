# Weather — web resources

Historical **Bing Weather for Windows Phone 8 / 8.1** design authority. These are the sources
used to reconstruct the `today · daily · hourly · maps` panorama; the original screenshots
they contain are **not** redistributed in this repo (see `known-gaps.md`).

Supplementary in-repo contract: `guides/blueprint.md` is authoritative; the Metro design
language lives in [`../../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](../../../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md).

## Primary design authority

| Source | URL | What it establishes |
|--------|-----|---------------------|
| Windows Experience Blog — *Bing apps arrive on Windows Phone 8* (guest post, Bing PM Steve Chang; cross-posted from Bing Blogs; 7 Aug 2013) | <https://blogs.windows.com/windowsexperience/2013/08/07/bing-apps-arrive-on-windows-phone-8/> | The four Bing apps launch on WP8; weather features: current conditions, precipitation, wind, humidity, historical weather, dynamic maps (satellite / temperature / precipitation / cloud cover / Doppler radar), hourly and 10-day forecasts, Start tile |
| Bing Search Blog — *Bing Apps Released for Windows Phone 8* (Steve Chang, 7 Aug 2013) | <https://blogs.bing.com/search/2013/08/07/bing-apps-released-for-windows-phone-8> | Canonical announcement; same feature list; store links per app |
| All About Windows Phone — *Bing Weather offers forecasts with a Modern UI flavour* (Rafe Blandford, 8 Aug 2013) | <https://allaboutwindowsphone.com/flow/item/18107_Bing_Weather_offers_forecasts_.php> | Names the panorama panes (`today, daily, hourly, maps`), the toolbar/places access, current-location button, per-day detail view (day/night, rain, wind, humidity, UV, sunrise/sunset), Foreca data source, WP8-only availability |

## Supporting / behavior detail

| Source | URL | What it adds |
|--------|-----|--------------|
| All About Windows Phone — *Bing Weather adds mountain information and double wide Live Tile support* | <https://allaboutwindowsphone.com/flow/item/18577_Bing_Weather_adds_mountain_inf.php> | Live Tile variants and Store description evolution |
| All About Windows Phone — *Bing apps updated with app data sync and UI tweaks* | <https://allaboutwindowsphone.com/flow/item/19337_Bing_Apps_updated_with_app_dat.php> | Later UI tweaks / Microsoft-account data sync |
| Windows Central — *Bing Weather for Windows Phone 8 Version 2 gets lockscreen support, wide tile* | <https://www.windowscentral.com/bing-weather-windows-phone-v2-lockscreen-support-wide-tile> | v2 additions; includes device screenshots (not redistributed here) |
| CNET — *Bing Weather (Windows Phone) review* | <https://www.cnet.com/reviews/bing-weather-windows-phone-review/> | Independent description of the flat "Modern UI" look and simplicity; includes screenshots (not redistributed here) |

## Historical hourly screenshots (Wealden / Seattle)

Publicly circulated WP8 Bing Weather hourly captures — e.g. the **Wealden, UK** location used
by AAWP and the default **Seattle** location used in Microsoft's marketing shots — are the
reference points for the deep-blue hourly surface, tall flat rows, and the large light
temperature. The specific image files are **not committed**; reconstruct layout from
`guides/blueprint.md` and the self-authored schematics in `images/`.
