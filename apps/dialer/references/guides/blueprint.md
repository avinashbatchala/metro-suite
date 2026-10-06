# Phone — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Target: **Windows Phone 8.1 / Update 1 / Update 2** Phone app on a portrait phone
(768×1280 / xhdpi reference profile). Do **not** imitate Windows 10 Mobile Phone.

---

## 0. Architecture authority — Android Telecom owns the phone engine

Metro Phone is the **Windows Phone 8.1 presentation and interaction layer**. It is **not** a
telephony stack.

```
Android Telecom / SIM / carrier
             ↓  real Call objects
       MetroInCallService  (android.telecom.InCallService)
             ↓
   MetroCallSession  (multi-call domain model)
             ↓
Windows Phone 8.1 Phone UI
```

**Hard rules**

- Cellular/PSTN calls are placed with `TelecomManager.placeCall(telUri, extras)`.
- Metro **does not** register a cellular `PhoneAccount` or `ConnectionService`. The former
  `MetroConnectionService` / `metro_phone` account / 1.5-second fake ACTIVE connection were
  removed and must not be reintroduced.
- Metro does not fake answered calls, call duration, or ringing.
- Only `Call.STATE_ACTIVE` marks a real carrier call active.
- Metro only displays `Call`s delivered by `InCallService`.
- Emergency routing is entirely Android's. Metro never special-cases emergency numbers into its
  own provider and never delays them with pickers or confirmation screens.
- When Metro is **not** the default dialer, numbers are handed to the system dialer
  (`ACTION_DIAL`); Metro never shows its own call UI, timer, or return banner.

### CALL_PHONE + ROLE_DIALER

- `ROLE_DIALER` (`RoleManager`) is requested through the setup surface; the user may decline.
- `ACTION_DIAL` and `ACTION_CALL` + `tel:` filters and `InCallService` are declared so Metro can
  hold the role.

### Multi-SIM (Android adaptation)

- One call-capable account / system default → call directly.
- Multiple call-capable accounts and no system default → Metro SIM chooser
  (`call using / SIM 1 / <carrier> / SIM 2 / <carrier>`), then placeCall with that real handle.
- Metro never invents a SIM account and never claims exact WP8.1 Dual-SIM behaviour.

### Permissions philosophy

- `CALL_PHONE` always; `READ_CALL_LOG` for History; `READ_CONTACTS` for names/photos/speed dial;
  `WRITE_CALL_LOG` for deleting history; `READ_PHONE_STATE` for carrier/voicemail info.
- Features degrade independently: without contacts the app still dials, still shows raw numbers,
  and still shows the incoming number.

---

## 1. Stock WP8.1 vs MetroSuite extensions vs Android adaptations

| Capability | Classification |
|------------|----------------|
| History · Speed dial · Voicemail root pivot | **Stock WP8.1** |
| Dial pad opened from the app bar (not a root pivot) | **Stock WP8.1** |
| Call/Save inside the dial-pad grid | **Stock WP8.1** |
| Date-grouped history with `(n)` counts | **Stock WP8.1** |
| History tap = call; right icon = contact; long-press = details/delete/block/add to speed dial | **Stock WP8.1** |
| GDR1 "select calls" multi-delete | **Stock WP8.1 (Update)** |
| Text Reply with preset + custom replies | **Stock WP8.1** |
| Call waiting / hold / swap / add call / merge | **Stock WP8.1** |
| Incoming locked slide/reveal | **Stock WP8.1 (best-effort on Android)** |
| Smart dial / T9 suggestions | **MetroSuite extension** (off by default) |
| Bluetooth/car/wearable controls driving Metro UI | **Android adaptation** (Telecom forwards) |
| Modern audio endpoints (earbuds/car/hearing aid) | **Android adaptation** |
| Visual voicemail | **Carrier-dependent; omitted unless a public API exists** |
| VOIP/video-call handoff tile | **Omitted** — no enabled stub |

---

## 2. App shell

- Root title: **`phone`** (single `MetroAppTitle`, authentic WP8.1 typography).
- Pivot: **`history` · `speed dial` · `voicemail`** — horizontal swipe.
- Dial Pad is **not** a pivot. It is a subpage opened from the history/speed-dial app bar.
- App bar (bottom, `MetroAppBar`):
  - History: `dial pad`, `people`, `search`; overflow: `select calls`, `settings`.
  - Speed dial: `add ( + )`, `people`.
  - Voicemail: overflow `settings`.
- Selection mode swaps the bar for `delete` · `select all` · `close`.
- No Material FAB, cards, rounded keypad tiles, bottom sheets, snackbars, or top app bars.

---

## 3. Pages

### Page 1 — History (default pivot pane)

- Dated sections (`today`, `yesterday`, weekday, date) — **repeat calls group by caller identity +
  local calendar date**, never one forever-growing row.
- Row:
  - Primary line: contact display name (verbatim from ContactsContract) or formatted number;
    `(n)` suffix when the dated group has >1 call.
  - Missed-family rows use accent red `#E51400`.
  - Secondary line: type label + relative time.
  - Right-side **contact/info icon**.
- Interactions:
  - Tap row → **call** that number.
  - Tap right icon → known contact opens the People contact card (`metro://people/contact/<id>`);
    unknown number opens the **Save contact** flow.
  - Long press → context menu: `details`, `delete`, `block number`, `add to speed dial`.
    `add to speed dial` only when a callable number exists.
- Private / unknown / withheld numbers are never shown as `call`/`save`/`pin` candidates and never
  collapsed into one lifetime record.

### Page 2 — Call detail (history long-press → details)

- Header: name (upper) + number (accent) when named.
- Chronological individual call entries: direction, date/time, duration / missed label.
- App bar: `call`, `message`, `delete`.
- Back → History, preserving scroll position.

### Page 3 — Dial pad (subpage)

- Large left-aligned dialed number with backspace.
- Key grid (square keys, no rounded corners, 4dp–6dp gaps):
  - `1 / 2 ABC / 3 DEF`, `4 GHI / 5 JKL / 6 MNO`, `7 PQRS / 8 TUV / 9 WXYZ`, `* / 0 + / #`.
  - Long-press `0` inserts `+`.
  - Accent press flash (minimum 150 ms).
  - **call** tile (accent) and **save** tile inside the grid.
- Smart dial suggestions (MetroSuite extension) appear above the grid only when enabled in
  Phone Settings; disabled by default to match stock WP8.1.
- `call` → `TelecomManager.placeCall` (or SIM chooser / system handoff).
- `save` → Save-number flow: `new contact` (`ACTION_INSERT` with phone prefill) or
  `add to existing` (`ACTION_INSERT_OR_EDIT`).

### Page 4 — Save number

- Number, `new contact`, `add to existing`. Real ContactsContract editor; no stub.

### Page 5 — Speed dial (pivot pane)

- `+` app bar → contact chooser → number picker when the contact has multiple numbers.
- Entries store durable identity (`lookup key` / data id) + number fallback, so renames, photo
  changes and label changes render automatically. Unresolvable numbers show a recoverable
  unavailable state.
- Tap → call. Long press → `open contact`, `pin to start`, `remove from speed dial`.
- People can add to speed dial through the signature-protected
  `com.metro.action.ADD_SPEED_DIAL` broadcast.

### Page 6 — Voicemail (pivot pane)

- Basic: `call voicemail` using the carrier voicemail number when exposed.
- No visual-voicemail inbox is fabricated. When no number is available the pane degrades to a
  short explanation + Settings link.

### Page 7 — Incoming call

- Unlocked/interactive: Metro incoming surface only (photo, carrier label when meaningful, name,
  label + formatted number, `answer`, `ignore`, `text reply`).
- Locked / screen-off: full-screen-intent notification opens the same surface with a WP-style
  slide/reveal guard before answer/ignore/text-reply.
- `text reply` opens the Metro reply chooser (four configurable presets + custom message) and uses
  `Call.reject(rejectWithMessage = true, message)` when the platform supports it; otherwise it
  falls back transparently. Text reply is only offered for a valid, SMS-capable mobile number.
- Ringtone: the system ringtone (selected through Metro Settings/Android) is played by
  `IncomingRingtonePlayer`, honouring silent/vibrate and audio policy; it stops on
  answer/reject/disconnect.

### Page 8 — In-call

- Identity: contact photo (full-bleed), status line (`dialling…`, `ringing…`, live timer from the
  authoritative connected timestamp, `on hold`, `call ended`), name, label + number.
- Control grid (period-accurate, square tiles):
  - `speaker` · `mute/unmute` · `audio` (endpoint chooser when >1 endpoint)
  - `hold` · `keypad` (DTMF) · `add call` (only when `canAddCall`)
  - `end call`
- No enabled `video` stub.
- DTMF: keys call `Call.playDtmfTone()` so the remote party receives the tone; local feedback is
  restrained.
- Multi-call:
  - Incoming waiting call row: `answer` (holds current) / `ignore`, plus `text reply`.
  - Held call row: `swap`, and `merge` when both calls are conferenceable.
  - Conference: participant list with `private` (split from conference) and `end` (disconnect
    participant), driven by real Telecom capabilities. Removing one call never clears the session
    while another remains.

### Page 9 — Phone Settings

- `text reply` toggle + `edit replies`.
- `voicemail` → voicemail pane; `voicemail settings`, `call forwarding`, `call waiting` hand off to
  Android/carrier call settings (no fake switches, no hidden MMS/USSD).
- **MetroSuite**: `smart dial` toggle (off by default), clearly separated from stock settings.

---

## 4. Behaviour rules

- Call duration is derived from a stable per-call connected timestamp captured on the first
  observed `STATE_ACTIVE`; it is never based on when the Metro UI started, and activity/service
  recreation does not reset it.
- Mute/hold state always reflects actual Telecom state (`setMuted`, `Call.hold()/unhold()`,
  capabilities).
- **Do not** map plain Back to "end call". End call is explicit. Back navigates the page stack.
- Start/Home during a call minimises to the green return-to-call notification; tapping it restores
  exact call state.
- Successful actions need no transient confirmation. No Android Toasts.
- No technical Telecom terms (`PhoneAccountHandle`, `CallEndpoint.TYPE_*`, `ROLE_DIALER`) are shown.

---

## 5. Images & gaps

| Page | Image | Notes |
|------|-------|-------|
| History | `images/history_dark_blue.png` | Grouped call log |
| Speed dial | `images/speed_dial_dark_blue.png` | Pinned contacts |
| Dial pad | `images/dialpad_dark_blue.jpg` | Keypad + autocomplete |
| In-call | `images/in_call_dark_blue.png` | Full-screen call UI |

Additional pages (voicemail, call detail, selection mode, incoming locked reveal, text-reply
chooser, call waiting, conference, Phone Settings, green return strip) are **not yet captured**;
see [`known-gaps.md`](known-gaps.md).

## 6. Supplementary guides

- [`phone-hub.md`](phone-hub.md) — history grouping + speed dial
- [`dial-pad.md`](dial-pad.md) — keypad, DTMF, in-call
