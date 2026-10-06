# People

**Package:** `com.metro.people`  
**Tier:** 2

## Status

Android project scaffolded. Implements WP8.1 People hub v1 per `references/guides/blueprint.md`, plus read/write **VCF (vCard) contact import** into Android's real `ContactsContract`.

## App role

This app recreates the WP8.1 **People** hub: a contact-centered experience with broad overview sections, all-contacts browsing, and filtered navigation.

The app should feel like a Metro hub, not a generic Android contacts manager. The social integration surface mentioned in project scope should be treated as a carefully bounded area, not an excuse to invent new network features.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Contacts permission and local-data strategy approved

## Screen inventory

Authoritative spec: [`references/guides/blueprint.md`](references/guides/blueprint.md)

### 1. People hub · all (default)

- Panorama landing pane: Me row, add account, contact list, jump list
- Reference: `references/images/hub_dark_blue.jpg` (not yet sourced — see `references/known-gaps.md`)

### 2. People hub · what's new

- Aggregated social feed pane; read-only in v1 with external deep-link stubs
- Supplementary: `references/guides/people-hub.md`

### 3. Filter contacts

- Full-page filter (hide no-phone, per-account checkboxes)
- Reference: `references/images/pivot_dark_blue.jpg`

### 4. Contact detail (pivot)

- Pivots: profile, connect, what's new, history
- Reference: `references/images/detail_dark_blue.jpg`
- Supplementary: `references/guides/contact-detail.md`

### 5. Import contacts (VCF)

- Reached from the hub app-bar `…` menu (`import contacts`) or by opening a `.vcf` from
  Metro Files / another app (`ACTION_VIEW`).
- Pages: file picker → import preview (counts + browsable list) → progress → result.
- Reference: no WP8.1 original (WP had no public VCF-URL UI); follows suite Metro patterns.

## VCF contact import

Imports `.vcf` / vCard files into **Android's real `ContactsContract`** so imported people are
immediately visible to People, Metro Dialer, Metro Messaging, WhatsApp bindings, and any other
contact-aware app. There is **no separate Metro-only contacts database**.

Pipeline: `.vcf` → `VCardParser` → `List<ImportContact>` → `ContactDuplicateDetector` (preview) →
`ContactWriter` (`ContactsContract`) → existing `ContactsRepository`.

- **Versions:** vCard 2.1, 3.0, and 4.0 (as supported by the parser).
- **Parser:** [`ez-vcard`](https://github.com/mangstadt/ez-vcard) `0.12.2` (BSD / "FreeBSD"
  license — see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)). No regex-based parsing.
- **File access:** Storage Access Framework (`ACTION_OPEN_DOCUMENT`). No storage permissions; the
  importer reads the provider `content://` URI through `ContentResolver` (never a filesystem path).
  The picker also includes a wildcard MIME entry so files misreported as `application/octet-stream`
  / `text/plain` can still be chosen; content is validated by the parser.
- **Field mapping:** `FN`/`N` → `StructuredName`; `TEL` → `Phone`; `EMAIL` → `Email`; `ADR` →
  `StructuredPostal`; `ORG`/`TITLE` → `Organization`; `BDAY` → `Event` (`TYPE_BIRTHDAY`); `NOTE` →
  `Note`; `URL` → `Website`; base64 `PHOTO` → `Photo`. Multiple values and `home`/`work`/`mobile`/
  `fax`/`pager`/`other` labels are preserved; unknown labels use Android custom labels.
- **Write architecture:** one `RawContacts` row per person plus typed `Data` rows, applied with
  bounded `ContentProviderOperation` batches (~50 contacts). A failing batch falls back to
  per-contact batches, so one bad card cannot lose an entire import; results are reported per
  contact.
- **Destination account:** device-local contacts (`ACCOUNT_NAME`/`ACCOUNT_TYPE` = `null`). The
  writer is structured so an account picker could be added later (out of scope).
- **Duplicate policy:** phone (normalized, tolerant of `+49 170 1234567` / `0170 1234567`) and email
  (case-insensitive) are the primary signals; a matching display name alone is a *possible*
  duplicate. `NEW` and `POSSIBLE_DUPLICATE` are imported; `EXACT_DUPLICATE` is skipped by default.
  No automatic destructive merging.
- **Permissions:** `READ_CONTACTS` gates normal browsing. `WRITE_CONTACTS` is requested **only** when
  an import begins; denial shows a Metro error and imports nothing (no partial import).
- **Safety limits:** 8 MB input cap, 5,000-contact cap, per-field/URL caps, 10 KB text fields, 2 MB
  decoded photo cap. A bad photo or field is skipped with a warning; it never aborts the import.
- **Privacy:** contact data is never logged, never uploaded, and no network access is needed.

## System functions and contracts

- Use `ContactsContract` for local contacts (read + VCF import write); no Metro-only contacts store
- Define sorting, grouping, and display-name fallback rules explicitly
- Social integration is part of scope language but should be treated as stubbed or informational until a real backend exists
- Keep avatar loading/fallbacks simple and deterministic

## UI and interaction guardrails

- `MetroPanorama` for hub landing (`all` + `what's new` panes)
- `MetroPivot` on contact detail only
- WP 8.1: tap contact name → call; profile icon → detail
- No Material contact chips, floating add buttons, or rounded avatar card grids
- Large names and left-aligned layout should dominate

## Data and state model

- `PersonSummary`, `PersonDetail`, `ContactMethod`, `PeopleFilter`
- Import: `ImportContact` (+ `ImportPhone`/`ImportEmail`/`ImportAddress`), `ImportPreview`,
  `ImportResult`, `ImportUiState`
- Track selected filter, permission state, loaded contacts, and section loading state

## Primary implementation order

1. Define contacts repository and permission flow
2. Build hub sections
3. Build contact list/filter navigation
4. Build person detail
5. Add any approved social surface stubs

## Test-critical user flows

1. Grant/deny contacts permission and show correct state
2. Load and browse contacts
3. Filter contacts
4. Open person detail and return without losing list context

## Reference and golden expectations

- `references/guides/blueprint.md` — read first
- `references/images/pivot_dark_blue.jpg`
- `references/images/detail_dark_blue.jpg`
- `references/images/detail_connect_dark_blue.jpg`
- `references/images/detail_whatsnew_dark_blue.jpg`
- `references/images/accounts_dark_blue.jpg`
- `references/known-gaps.md` — hub contact-list screenshot still needed

## Commands

```bash
cd apps/people

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh people
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Social integration surfaces backed by Microsoft/social services | Out of current v1 backend scope | Focus on local contacts first and stub or omit external social feeds with explicit documentation |
| Skype / social call-chat rows on profile | WhatsApp is the common Android messaging binding | When WhatsApp (or Business) is installed and has synced the contact, show `call WhatsApp` / `text WhatsApp` via ContactsContract data rows (or `wa.me` for text if only a raw-contact sync exists) |

## Agent postmortem

_None._
