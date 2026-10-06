# Phone — known reference gaps

Pages required by the blueprint that do not yet have a dedicated WP8.1 capture in `images/`.
Workarounds reference the nearest available image + blueprint section.

| Missing capture | Should show | Workaround |
|-----------------|-------------|------------|
| `voicemail_dark_blue.png` | Voicemail pivot pane (call voicemail / empty state) | Blueprint § Page 6 |
| `call_detail_dark_blue.png` | Single-contact call detail with per-call rows | `history_dark_blue.png` + blueprint § Page 2 |
| `history_selection_dark_blue.png` | GDR1 "select calls" multi-select with checkboxes | Blueprint § Page 1 interactions |
| `history_context_menu_dark_blue.png` | Long-press menu: details/delete/block/add to speed dial | Blueprint § Page 1 interactions |
| `incoming_locked_dark_blue.png` | Locked incoming call slide/reveal | `in_call_dark_blue.png` + blueprint § Page 7 |
| `text_reply_chooser_dark_blue.png` | Text Reply preset chooser + custom | Blueprint § Page 7 |
| `call_waiting_dark_blue.png` | Active call + incoming waiting call | Blueprint § Page 8 |
| `conference_dark_blue.png` | Merged conference participants | Blueprint § Page 8 |
| `phone_settings_dark_blue.png` | Phone settings / edit replies | Blueprint § Page 9 |
| `return_to_call_strip.png` | Green Metro return-to-call strip | Blueprint § 4 |

Do not substitute Windows 10 Mobile captures. When adding a capture, use the naming pattern
`<screen>_<theme>_<accent>.<ext>` and attribute it in `references/README.md`.
