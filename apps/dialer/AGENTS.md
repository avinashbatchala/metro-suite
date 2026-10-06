# Agent instructions — Phone (`com.metro.dialer`)

**Tier 2** | WP 8.1 / Update 1–2 Phone app. Reference: `references/guides/blueprint.md`.

**Android Telecom owns real cellular calling; Metro owns the WP8.1 presentation layer.** Never
register a Metro `PhoneAccount`/`ConnectionService`, fake a connection, or fake a call timer.
Outgoing calls use `TelecomManager.placeCall()`; non-default dialer hands off via `ACTION_DIAL`.

Root is a `MetroPivot` of `history | speed dial | voicemail`. Dial pad is a **subpage** opened from
the app bar (not a root pivot). Incoming/in-call are full-screen. Smart dial/T9 is a MetroSuite
extension, off by default. Read call log via `CallLog.Calls`; resolve callers via
`ContactsContract.PhoneLookup`.

Verify: `../../scripts/verify-app.sh dialer`
