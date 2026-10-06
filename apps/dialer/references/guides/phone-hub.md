# Phone hub — history & speed dial

Supplementary guide. Authoritative layout: [`blueprint.md`](blueprint.md).

## History pane

WP 8.1 Phone opens to **history**.

### Grouping rules

1. Group calls by **caller identity** (contact lookup key, else normalized number) **and local
   calendar date**. Repeated calls from the same caller on the same day collapse to one row with
   an `(n)` count; the same caller on another day is a separate row.
2. Show the ContactsContract display name verbatim when resolved; the number otherwise.
3. Sort groups by most recent call timestamp descending; render dated section headers
   (`today`, `yesterday`, weekday, date).
4. Keep individual `CallEntry` rows for the Call-detail page.
5. Never group private/unknown callers into a single lifetime record.

### Row semantics

| Call type | Primary color | Secondary label |
|-----------|---------------|-----------------|
| Incoming (answered) | White | `incoming · <relative time>` |
| Outgoing | White | `outgoing · <relative time>` |
| Missed / rejected / blocked | Accent red `#E51400` | `missed` / `rejected` / `blocked` |
| Voicemail / answered externally | White | type label + time |

### Interactions (corrected)

- Tap the name/number → **call**.
- Tap the right-side contact icon → contact card (known) or Save flow (unknown). It does **not**
  call.
- Long press → `details`, `delete`, `block number`, `add to speed dial`.

### Search / select

- Search filters the visible groups by name/number substring.
- Overflow `select calls` enters multi-select (delete / select all / cancel).

## Speed dial pane

- Contacts are **explicitly added** via `+` in the app bar (or People's `add to speed dial`).
- Entries are stored by durable contact identity (lookup key / data id) plus a number fallback, so
  renames, photo changes and label changes render automatically.
- Tap → call. Long press → `open contact`, `pin to start`, `remove from speed dial`.

## Bottom app bar icons

| Pane | Icons | Overflow |
|------|-------|----------|
| History | dial pad, people, search | select calls, settings |
| Speed dial | add, people | settings |
| Voicemail | — | settings |
