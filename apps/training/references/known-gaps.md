# Training — known reference gaps

Training is a **MetroSuite original** (Microsoft shipped no WP8.1 workout tracker). WP8.1 captures
therefore cover *layout grammar* (Pivot, ApplicationBar, lists, dialogs, type ramp) rather than
literal Training screens. The blueprint is authoritative for the app's own surfaces.

| Surface | Reference basis | Gap |
|---------|-----------------|-----|
| Root Pivot (`today/routines/history/progress`) | WP8.1 Pivot grammar | No literal capture; follows suite Pivot |
| ApplicationBar (context actions) | WP8.1 AppBar | No literal capture |
| List rows (routines/history/exercises) | WP8.1 LongListSelector rows | No literal capture |
| Set logging grid | WP8.1 typography/list grammar | No literal capture |
| Prescription / custom-exercise dialogs | WP8.1 MessageDialog / ListPicker | No literal capture |
| Rest timer banner | WP8.1 status surfaces | No literal capture |
| Recommendation "why" | WP8.1 detail page grammar | No literal capture |
| Live Tile | WP8.1 Start tiles (suite) | No literal capture |

Concrete workaround for every row: implement against the suite's shared controls
(`MetroAppBar`, `MetroContextMenuPopup`, `MetroListPicker`, `MetroMessageDialog`, `MetroPivot`)
which already encode the WP8.1 grammar, and measure against the suite's existing reference
screenshots. Add real captures only from WP8.1 (never Windows 10 Mobile) if a suitable one appears.
