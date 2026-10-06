# Agent instructions — People (`com.metro.people`)

**Tier 2** | Contact hub. Reference: `references/guides/blueprint.md`.

`MetroPanorama` (`all` + `what's new`). `MetroPivot` on contact detail. Filter is a separate page. Read contacts via `ContactsContract`. WP 8.1: tap name → call.

VCF import: app-bar `…` → `import contacts` (SAF `ACTION_OPEN_DOCUMENT`) or inbound `ACTION_VIEW` vCard. Pipeline lives in `data/import/` (`VCardParser` via ez-vcard → `ContactDuplicateDetector` → `ContactWriter` → `ContactsContract`, device-local). No separate Metro contacts DB. `WRITE_CONTACTS` is requested only when an import starts; never log contact data. UI in `ui/ImportScreens.kt`.

Verify: `../../scripts/verify-app.sh people`
