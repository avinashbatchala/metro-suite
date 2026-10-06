# Dial pad & in-call

Supplementary guide. Authoritative layout: [`blueprint.md`](blueprint.md).

## Keypad layout (WP 8.1)

WP 8.1 moved **call** and **save** from the bottom app bar into the tile grid.

```
[ 1   ] [ 2   ] [ 3   ]
        ABC
[ 4   ] [ 5   ] [ 6   ]
        DEF   GHI
[ 7   ] [ 8   ] [ 9   ]
        PQRS  TUV  WXYZ
[  *  ] [ 0 + ] [  #  ]
[ call (accent) ] [ save ]
```

- Tiles are **square**, separated by ~6dp gaps; no rounded corners.
- Digit: 36sp centred/left; letter hints: 13sp grey.
- `0` shows a small `+` hint; long-press inserts `+`.
- **Call** tile: accent background, white `call` label.
- **Save** tile: dark tile with save glyph + `save` label — opens the Save-number flow.

### Smart dial / T9 (MetroSuite extension)

- Off by default to match stock WP8.1. Enable in Phone Settings → smart dial.
- When enabled, matching contacts (T9 name prefixes + number prefixes) appear above the grid; tap
  fills the field, tap again calls.

Reference: `images/dialpad_dark_blue.jpg`.

## In-call screen

- Full-bleed contact photo when available.
- Status line: `dialling…` / `ringing…` / live `m:ss` timer / `on hold` / `call ended`.
- **End call** is a large accent control at the bottom of the grid.
- Controls: `speaker`, `mute`, `audio` (endpoint chooser when multiple endpoints exist), `hold`,
  `keypad`, `add call` (only when `InCallService.canAddCall()`), `end call`.
- No enabled video stub.

### DTMF keypad

- Digits call `Call.playDtmfTone()` so the remote party receives the tone; the local tone is
  restrained.

### Multi-call

- Waiting call row: `answer` (holds current) / `ignore`.
- Held call row: `swap`, and `merge` when both calls are conferenceable.

### End-call behavior

- End is explicit (End-call control or notification action). **System Back does not end a call** —
  it navigates (e.g. hides the keypad) or minimises to the return-to-call notification.
- On real Telecom disconnect the timer stops; no user action is required.

Reference: `images/in_call_dark_blue.png`.
