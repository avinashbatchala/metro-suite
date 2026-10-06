# Metro suite — Obtainium package

`metro-suite-obtainium.json` is an [Obtainium](https://github.com/ImranR98/Obtainium)
import file that configures every Metro suite app to install/update from this
repository's GitHub Releases.

## Why one file for many apps

Obtainium's GitHub source tracks **one app per repo URL**, but each app's identity also
includes its **"Filter APKs by Regular Expression"** setting. So all suite apps share this
repo URL, and each entry selects its own `<app>-release.apk` asset (e.g. `clock-release.apk`).
The app `id` is the Android package name, so Obtainium detects what is already installed.

## Import

1. Install Obtainium (`dev.imranr.obtainium`).
2. Obtainium → bottom **Import/Export** tab → **Import** → choose this JSON.
3. Tap **Update all**.

(Or download it from the latest GitHub Release, which also hosts the APKs.)

## Requirements for updates to work

- Every release must attach assets named `<app>-release.apk` (see `scripts/build-apks.sh --release`).
- Every release must be signed with the **same keystore** — Obtainium installs updates, so a
  changed signing key forces users to reinstall. Use a fixed upload keystore
  (`KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`) for distribution.

## Regenerate

```bash
python3 scripts/generate-obtainium-json.py [owner/repo]
```

The suite currently publishes all apps under one release tag, so they update together.
