# Agent instructions — Training (`com.metro.training`)

**Tier 2** | WP 8.1 Metro resistance-training tracker with an automatic progression engine.
Reference: `references/guides/blueprint.md` and `references/research.md`.

Hard rules for this app:

- **Performance determines progression.** Exercise metadata determines how it is expressed. Muscle
  group informs analytics/defaults only — never load jumps.
- Auto Progress is **deterministic, offline, explainable**. No LLM, no cloud, no randomness.
- Built-in and **custom exercises share one `ExerciseDefinition`** and one progression engine.
  Never special-case `isCustom`.
- Never hard-code rep-range-by-goal fractions, sex/age/bodyweight load multipliers, or
  muscle-specific increments.
- Progression logic lives in `domain/progression/` as **pure Kotlin** with no Android imports;
  write unit tests first.
- Do not let weekly volume analytics change progression.
- Pain → `REVIEW`; never diagnose or prescribe rehabilitation.
- No Material UI: no cards, FAB, bottom sheets/navigation, chips, snackbars, Material pickers.
  Use the suite toolkit (`MetroAppBar`, `MetroContextMenuPopup`, `MetroListPicker`, …).

Verify: `../../scripts/verify-app.sh training`
