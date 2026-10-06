# People — known reference gaps

Target: **WP8.1 Update / Update 2 People Hub** (Lumia 640 era) — sections
`contacts · what's new · rooms`.

## Deferred / partial (documented)

| Item | Status | Notes |
|------|--------|-------|
| Connect service model (Signal / WhatsApp) | Partial | WhatsApp actions still live on the contact **profile** (retained). The generic `ConnectedService` model + Signal detection on the contact card **Connect** pivot is a follow-up. |
| Contact card Pivot `profile · connect · what's new · history` | Partial | The card shows native **profile** actions + a pin ApplicationBar; the Connect / What's New / History pivots are not yet split out. |
| History (calls/SMS/mail) | Not implemented | Requires suite-owned sibling contracts (Dialer/Messaging/Mail) — not yet defined in `metro-system-sdk`. |
| Rooms / Groups | Empty state only | The `rooms` section exists; Groups CRUD (ContactsContract.Groups / app-private store) is a follow-up. Microsoft Rooms cloud is discontinued and not faked. |
| Me / own contact | Not implemented | Uses `ContactsContract.Profile` when permitted, else a designated contact — follow-up. |
| Sort / display-name settings | Not implemented | Requires provider sort-key + StructuredName queries (no `substringAfterLast`). |
| New / Edit / Delete / Link contact | Platform-intent interim | `+` opens the platform contact editor (`ACTION_INSERT`); Metro edit/delete/link UIs are follow-ups. |
| LOOKUP_KEY identity | Not implemented | Tiles/deep links still use numeric contact ids; legacy links must keep working during migration. |

## Missing screenshots

| File | What we need | Workaround |
|------|--------------|------------|
| `hub_contacts_dark_blue.jpg` | WP8.1 **contacts** pane (Me row, showing filter, jump tile, contact list) | `people panorama.jpg` / `people.jpeg` + `guides/blueprint.md` |
| `hub_whatsnew_dark_blue.jpg` | Hub-level **what's new** pane | `detail_whatsnew_dark_blue.jpg` + blueprint |
| `rooms_dark_blue.jpg` | **rooms** / Groups section | blueprint § Rooms |

## Sourced externally (attribution)

`images/` captures are from [All About Windows Phone](https://allaboutwindowsphone.com/) (Steve
Litchfield, Lumia 640, WP8.1) and Windows Central coverage — community reference; the corrected
blueprint is authoritative on layout, and Windows 10 Mobile People is **not** used for geometry.
