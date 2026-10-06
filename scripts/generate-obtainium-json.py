#!/usr/bin/env python3
"""Generate an Obtainium import file for the Metro suite.

Obtainium's GitHub source tracks one app per repo URL, but each app's identity also
includes its "Filter APKs by Regular Expression" setting. This emits one app entry per
suite app, all pointing at the same repo, each selecting its own `<app>-release.apk`
asset. The app `id` is the Android package name so Obtainium can detect installed apps.

Usage:
    python3 scripts/generate-obtainium-json.py [owner/repo]
Writes:
    dist/obtainium/metro-suite-obtainium.json
"""
import json
import os
import sys

# (package, display name, asset key)  — asset key matches deploy/apks/<key>-release.apk
APPS = [
    ("com.metro.launcher", "Launcher", "launcher"),
    ("com.metro.statusbar", "Status bar", "statusbar"),
    ("com.metro.notifications", "Notifications", "notifications"),
    ("com.metro.navbar", "Navigation bar", "navbar"),
    ("com.metro.volume", "Volume", "volume"),
    ("com.metro.lockscreen", "Lock screen", "lockscreen"),
    ("com.metro.keyboard", "Keyboard", "keyboard"),
    ("com.metro.settings", "Settings", "settings"),
    ("com.metro.people", "People", "people"),
    ("com.metro.dialer", "Phone", "dialer"),
    ("com.metro.messaging", "Messaging", "messaging"),
    ("com.metro.conversations", "Conversations", "conversations"),
    ("com.metro.photos", "Photos", "photos"),
    ("com.metro.calendar", "Calendar", "calendar"),
    ("com.metro.clock", "Clock", "clock"),
    ("com.metroweather.app", "Weather", "weather"),
    ("com.metro.music", "Music", "music"),
    ("com.metro.files", "Files", "files"),
    ("com.metro.calculator", "Calculator", "calculator"),
    ("com.metro.widgets", "Widgets", "widgets"),
    ("com.metro.news", "News", "news"),
]


def entry(repo_url, author, pkg, name, key):
    regex = "^%s-release\\.apk$" % key
    return {
        "id": pkg,
        "url": repo_url,
        "author": author,
        "name": name,
        "installedVersion": None,
        "latestVersion": None,
        "apkUrls": "[]",
        "otherAssetUrls": "[]",
        "preferredApkIndex": 0,
        "additionalSettings": json.dumps(
            {"apkFilterRegEx": regex, "invertRegEx": False}
        ),
        "lastUpdateCheck": None,
        "pinned": False,
        "categories": [],
        "releaseDate": None,
        "changeLog": None,
        "releaseUrl": None,
        "overrideSource": None,
        "allowIdChange": False,
        "pendingRepoRenameUrl": None,
    }


def main():
    repo_full = sys.argv[1] if len(sys.argv) > 1 else "avinashbatchala/metro-suite"
    repo_url = "https://github.com/" + repo_full
    author = repo_full.split("/")[0]
    payload = {
        "schemaVersion": 2,
        "exportedAt": "2026-10-06T00:00:00.000Z",
        "apps": [entry(repo_url, author, p, n, k) for (p, n, k) in APPS],
    }
    out_dir = os.path.join(os.path.dirname(__file__), "..", "dist", "obtainium")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.abspath(os.path.join(out_dir, "metro-suite-obtainium.json"))
    with open(out, "w") as f:
        json.dump(payload, f, indent=2)
        f.write("\n")
    print("wrote %s (%d apps) for %s" % (out, len(APPS), repo_url))


if __name__ == "__main__":
    main()
