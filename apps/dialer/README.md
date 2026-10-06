# Phone

**Package:** `com.metro.dialer`
**Tier:** 2

## Status

Android app implementing the WP 8.1 / Update 1 / Update 2 **Phone** app per
`references/guides/blueprint.md`, on top of Android Telecom as the call engine.

## App role

Metro-native replacement for the WP 8.1 **Phone** app: date-grouped call history, speed dial,
voicemail pane, keypad with an optional smart-dial extension, and full-screen incoming/in-call UI.
**Android Telecom owns real cellular calling; Metro owns the presentation layer.**

## Architecture

```
Android Telecom / SIM / carrier
             ↓ real Call objects
       MetroInCallService
             ↓
   MetroCallSession (multi-call model)
             ↓
Windows Phone 8.1 Phone UI
```

- Outgoing: `TelecomManager.placeCall(telUri, extras)` (real SIM/carrier). Multi-SIM chooser uses
  real `PhoneAccountHandle`s.
- Incoming: `InCallService.onCallAdded`; caller presentation resolved directly via
  `ContactsContract.PhoneLookup` (never by scanning call history).
- No Metro `PhoneAccount`, no `ConnectionService`, no simulated connection, no fake timer.
- Non-default dialer → the number is handed to the system dialer (`ACTION_DIAL`).

See `references/guides/blueprint.md` § 0.

## Screen inventory

Authoritative spec: [`references/guides/blueprint.md`](references/guides/blueprint.md)

1. **History** — dated, grouped call log (`today`, `yesterday`, …)
2. **Call detail** — per-contact chronological calls (history long-press → details)
3. **Dial pad** — subpage launched from the app bar; call/save in the grid
4. **Save number** — new contact / add to existing (real ContactsContract editor)
5. **Speed dial** — durable, contact-resolved pinned numbers
6. **Voicemail** — basic carrier voicemail calling
7. **Incoming call** — photo, answer/ignore/text reply, locked reveal
8. **In-call** — identity, live timer, speaker/mute/audio/hold/keypad/add call/end
9. **Phone Settings** — text reply, edit replies, voicemail, carrier handoffs, smart dial

## System functions and contracts

- `CallLog.Calls` for history; `ContactsContract.PhoneLookup` for names/photos/lookup keys.
- `TelecomManager.placeCall()`; `InCallService` for observation.
- `com.metro.action.ADD_SPEED_DIAL` — signature-protected
  (`com.metro.dialer.permission.INTERNAL`) People → Phone speed-dial contract.
- People contact deep link: `metro://people/contact/<contactId>`.

## UI and interaction guardrails

- Root pivot is `history | speed dial | voicemail`; Dial Pad is **not** a root pivot.
- `MetroPivot`/`MetroHubTitleRow`, `MetroAppBar`, `MetroContextMenuPopup`, `MetroMessageDialog`.
- Call/save live in the dial-pad grid.
- No Material FAB, cards, chips, rounded keypad tiles, bottom sheets, or snackbars.
- No Android Toasts.

## Data and state model

- `CallEntry` / `CallGroup` / `CallType` / `HistorySection`, `SpeedDialEntry`, `PhoneContact`.
- `DialerViewModel` loads history/contacts off the main thread and observes `CallLog` +
  `ContactsContract` for live refresh.
- Live call state: `MetroCallSession` (`MetroTelecomCall`, `MetroCallSessionState`).

## Commands

```bash
cd apps/dialer

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh dialer
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|--------------------|------------|
| Call screen while locked | Background activity starts blocked when locked/screen-off | Full-screen-intent notification only when locked/screen-off; unlocked opens Metro incoming UI directly |
| Green return-to-call banner | Requires notification-listener access in the shell | Phone posts an ongoing call notification; the shell presents the green strip when notification access is granted |
| Proximity screen-off | Devices without a proximity sensor | `PROXIMITY_SCREEN_OFF_WAKE_LOCK` while in-call on an earpiece endpoint; no-ops otherwise |
| Locked incoming slide/reveal | Android lockscreen constraints | Reveal interaction approximated; full answer/ignore/reply on reveal |
| Visual voicemail | Carrier-specific, no public third-party API | Omitted; basic voicemail calling only |
| Exact WP8.1 Dual-SIM UI | Multi-SIM APIs vary by OEM | Metro SIM chooser using real call-capable accounts |
| Carrier call forwarding/waiting config | No public configuration API | Hand off to Android/carrier call settings |
| Video / VOIP handoff tile | No safe cross-app contract yet | Hidden (no enabled stub) |

## Reference and golden expectations

- `references/guides/blueprint.md` — read first
- `references/known-gaps.md` — missing captures
- `references/images/`

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Agent postmortem

_None._
