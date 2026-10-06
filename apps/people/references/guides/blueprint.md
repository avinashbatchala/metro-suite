# People — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 Update / Update 2** People hub (Lumia 640 era) on a portrait phone
(768×1280 / xhdpi reference profile). The mature Hub contains three sections:
`contacts · what's new · rooms` (Rooms also holds **Groups**). Do **not** use the WP7/WP8 huge
Panorama `all` design, and do not use Windows 10 Mobile People styling.

## App shell

- **Control model:** `MetroPanorama` for the hub landing (horizontal sections
  `contacts · what's new · rooms`). Use the compact WP8.1 Hub-section typography, not a 64sp
  classic Panorama heading.
- **Theme:** WP8.1 supports dark and light; use `MetroTheme` colors (no hard-coded `Color.Black`
  where the system theme owns the surface). Contact photos may provide a full-bleed header.
- **Typography:** suite Metro face. Contact names large and left-aligned; section headings compact.
- **App bar:** round icon buttons on the contacts section (`+ new`, `search`) plus `…` overflow
  (`settings`). Standard round icon buttons on subpages.
- **No Material:** No FAB, chips, rounded avatar cards, bottom sheets, Material snackbars/dialogs.

## WP 8.1 deltas (vs WP 8.0)

| WP 8.0 | WP 8.1 (build this) |
|--------|---------------------|
| Opens to What's new | Opens to **contacts** (contact list) |
| Recent pane (last 8 people) | **Removed** — do not implement |
| Together pane | **Rooms** section, containing **Groups** — implement |
| In-hub Facebook like/comment | **Read-only feed**; tap opens external app |
| Tap contact row → profile | Tap contact **name/content** → **call**; avatar → contact card |
| Built-in Facebook/Twitter sync | App-linked accounts via Add an account |

## Pages

### Page 1 — People hub · contacts (default landing)

- **Layout:**
  - Section title: lowercase `contacts`.
  - Peek of the next section (`what's new`) on the right edge (~40dp).
  - Top of scrollable content (in order):
    1. **Me row** — user's own square photo + display name (`ContactsContract.Profile` or a
       designated contact fallback).
    2. **Showing filter row** — accent label (`showing only contacts with phone numbers`); tap opens
       filter contacts.
    3. **Alphabet jump tile** — sticky per section; tap opens jump list overlay.
    4. **Contact list** — grouped by locale-aware sort key. Each row:
       - Square avatar (48dp), left; **tap → contact card**.
       - Display name; **tap name/content → call the primary/super-primary number** (falls back to
         the contact card when no callable number exists).
       - **No** large right-facing arrow.
  - Thin low-contrast separators; no card elevation.
- **Navigation:** horizontal swipe → `what's new` / `rooms`. App-bar `+` → new contact; `search` →
  search; `…` → settings.
- **Interactions:** long-press → context menu (pin / edit / delete / add to speed dial).
- **Background:** theme surface.

### Page 2 — People hub · what's new

- Section title `what's new`. Read-oriented feed (external-app deep links). No private
  message content (Signal/WhatsApp messages are never social posts). Restrained empty state when
  no source is connected; the section remains.

### Page 3 — People hub · rooms

- Section title `rooms`. Hosts **Groups** (ContactsContract.Groups or an app-private group store
  keyed by LOOKUP_KEY). Microsoft's Rooms cloud backend is discontinued and is **not** faked.

### Contact card (Pivot)

```text
JOHN SMITH
Google

profile   connect   what's new   history
```

- **profile** — native capabilities: square photo, `call mobile`, `text`, `send email`,
  `map address`, `view website`, notes. No Signal/WhatsApp entries here.
- **connect** — external app services (Signal / WhatsApp) as large square service tiles; tap =
  message, long-press = capability menu (message / voice / video).
- **what's new** — the resident feed filtered to the contact.
- **history** — communication history from safe suite contracts; empty state otherwise.
- ApplicationBar: `pin · link · edit · …` (overflow: delete / share).

### People Settings

`add contacts`, `filter contact list`, `import contacts`, `sort list by`, `display names by`.

### Filter contacts

`Hide contacts without phone numbers` toggle + `show contacts from my` account checkboxes.
ApplicationBar save = **check**, cancel = **X**. Search ignores the phone-number visibility filter.

### Jump list overlay

Toolkit `MetroJumpList`: `#`, locale sections, and an accent **globe**. Active letters (with
contacts) use accent fill; inactive use dark grey. Tap → scroll to section + dismiss; scrim/Back →
dismiss.

### Me / own contact

Use `ContactsContract.Profile` when permitted; otherwise a user-designated contact (store only its
LOOKUP_KEY); otherwise a restrained setup row. Do not duplicate contact data or request profile
permissions on startup.

### New / edit contact

Root `+` → new contact. If several writable destinations exist, choose account first (device-local
is valid on Android). Edit is progressive (name, photo, phones, emails, addresses, company/title,
birthday, website, notes, ringtone). Save through `ContactsContract`; WRITE_CONTACTS is requested
only when editing/creating/saving. Never edit read-only connector raw contacts.

### Link contacts

`link` in the card ApplicationBar → suggest duplicates (`ContactsContract.AggregationExceptions`) →
tap to link; manual search; unlink supported. Never duplicate records to simulate linking.

### Delete contact

`… → delete` on writable cards only; conservative when only some raw contacts are writable.

### Import contacts (VCF — MetroSuite extension)

Entry: `settings → add contacts → import contacts`, plus inbound `ACTION_VIEW` of a `.vcf`. Existing
flow preserved (picker → preview → progress → result). Writes to device-local `ContactsContract`.
Imported rich fields (addresses, organization, title, birthday, notes, websites, photos, multiple
phones/emails) must surface in the Profile card.

### Rooms / Groups

`rooms` hosts **Groups** (ContactsContract.Groups / app-private group store keyed by LOOKUP_KEY).
Create/rename/add/remove members/delete/pin/send message/send email. Microsoft's Rooms cloud backend
is discontinued and is **not** faked.

### Live tile

People Start tile = animated contact-photo mosaic with accent fallback cells (launcher-owned).
Contact secondary tiles: photo front, accent+identity fallback, deep-link to the exact contact.
Group tiles use a member-photo mosaic. Prefer LOOKUP_KEY identity; keep legacy numeric deep links
working.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `people panorama.jpg` / `people.jpeg` | Hub | Panorama reference |
| `jumplist_dark_blue.png` | Jump list | Active/inactive tiles + globe |
| `pivot_dark_blue.jpg` | Filter contacts | Toggles + account checkboxes |
| `detail_dark_blue.jpg` | Profile pivot | Primary contact card |
| `detail_connect_dark_blue.jpg` | Connect pivot | Linked app service tiles |
| `detail_whatsnew_dark_blue.jpg` | What's new pivot | Per-contact feed |
| `accounts_dark_blue.jpg` | Add contacts | Account setup |
| `live tile.png` / `pin to start.png` | Tiles | Mosaic + pin |

## Data model (implementation hint)

- `PersonSummary(id, displayName, photoUri, hasPhone, defaultPhone, defaultEmail, sourceLabel, sortKey)`
- `PersonDetail(summary, phones, emails, ..., sources, whatsApp)`
- `ContactMethod(type, label, value)` — real ContactsContract types/custom labels
- `PeopleFilter(hideWithoutPhone, visibleAccounts)`
- `ConnectedService(service, packageName, actions)` — Signal / WhatsApp in **Connect**
- Hub sections: `contacts · what's new · rooms`

## Out of scope / exceptions

- Microsoft Rooms cloud backend (discontinued) — Groups only.
- Cortana Inner Circle (no Cortana-equivalent system).
- Inline social like/comment/reply (feed deep-links to source apps).
- Signal network probing (use Signal's own ContactsContract rows only).
