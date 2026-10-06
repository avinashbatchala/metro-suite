# Training

**Package:** `com.metro.training`
**Tier:** 2

## Status

WP 8.1 Metro resistance-training tracker with an evidence-informed, explainable automatic
progression engine. Built to the spec in this app's `references/guides/blueprint.md` and
`references/research.md`.

## App role

Log resistance training, understand performance, and prescribe the next exposure:

```text
log training → understand performance → prescribe next exposure
```

Core principle: **Windows Phone 8.1 presentation, evidence-informed resistance-training engine.**

The app is a healthy, general-population resistance-training tracker. It is not a nutrition,
rehabilitation, or medical app.

## The progression rule (product)

> Performance determines progression. Exercise metadata determines how progression is expressed.
> Muscle group informs analytics and defaults — never arbitrary load jumps.

Every exposure yields one of `ADD_REPS`, `ADD_LOAD`, `HOLD`, `REDUCE_LOAD`, `REVIEW`, always with a
structured, deterministic "why".

## Architecture

```
app/src/main/java/com/metro/training/
  domain/
    exercises/   ExerciseDefinition, BuiltInExercises, ExerciseDefaults, taxonomy
    routines/    Routine, RoutineExercisePrescription, ProgressionPolicy
    workout/     WorkoutSet, Workout, CompletedExerciseExposure, snapshots
    progression/ ProgressionEngine (pure Kotlin), WeightMath, explanations
    volume/      MuscleVolumeCalculator
  data/
    database/    Room entities, DAO, database
    repositories/TrainingRepository
  ui/            Compose (WP8.1 Metro), TrainingViewModel, screens
  tiles/         TrainingTileProvider (Metro Live Tile)
```

Progression logic is pure Kotlin with **no Android dependencies** and is extensively unit tested.

### Key engine properties

- **RIR-aware double progression**: accumulate reps within the range, then add one equipment
  increment.
- **RIR is noisy**: practical zones (`targetRirMin`–`targetRirMax`), not decimals.
- **No RIR** → more conservative (confirm top range once more before adding load).
- **Assisted** (`LESS_LOAD_IS_HARDER`) → "reduce assistance", never "add load".
- **Reps-only** → `REVIEW` at the ceiling; no invented load.
- **Quality flags**: `technique limited` blocks `ADD_LOAD`; `pain` → `REVIEW`; `failed rep`
  holds.
- **Underperformance** reduces load only after **two consecutive** comparable exposures, by one
  increment.
- **Comparable exposures** share rep range / work-set count / load semantics / direction; a
  programming change starts a new **progression epoch**.
- **Long gaps** lower confidence and hold load.
- Deterministic, offline, no LLM, no cloud.

## Visuals & logging (v1.1)

- **Inline set logging** — each row is `SET | PREVIOUS | KG | REPS | RIR | ✓`; cells are edited in
  place via a **MacroFactor-style bottom numeric keypad** (no system keyboard), so the table stays
  visible. The focused cell shows an accent underline.
- **Copy-first-set / prefill** — KG and REPS prefill from last time; set 1 live-copies to later
  sets until edited; "+ set" copies the previous set.
- **Set number menu** — set type (normal/warm-up/failure/drop/backoff/myorep), per-side L/R,
  partial reps, set note, remove.
- **Auto-advance** — `next` moves KG → REPS → RIR → next set; completing a set jumps to the next
  incomplete set.
- **Charts** — custom Metro Canvas line/bar charts. Exercise detail shows estimated 1RM (Epley,
  1–12 reps), top-set load, total reps and volume load over week/month/3m/6m/year/all; Progress
  shows estimated weekly sets per muscle (vs last week) and a weekly-volume trend.
- **Illustrations** — bundled free-exercise-db images (two-frame flip) + instructions. See
  `THIRD_PARTY_NOTICES.md`.
- **Animations** — set-complete accent flash, PR celebration, timestamp-based rest bar (plus an
  ongoing notification), staggered list entry.

## Revamp status (Hevy/MacroFactor-inspired)

- **Phase 1 — logging** ✅ inline cells + bottom numeric keypad, set types (normal/warm-up/failure/
  drop/backoff/myorep), previous-values column, per-side/partials/myoreps, set + exercise + workout
  notes, exercise menu (note/replace/reorder/superset/history/remove), start-empty-workout, header
  stats, ongoing rest notification.
- **Phase 2 — library** ✅ bundled instructions/cues + muscle/equipment filters.
- **Phase 3 — routines** 🔶 pending (folders, inline editor polish).
- **Phase 4 — history/PRs** ✅ personal records on exercise detail, richer workout summary; 🔶
  calendar view pending.
- **Phase 5 — progress** ✅ weekly sets + volume trend + per-exercise charts; 🔶 distribution chart
  pending.
- **Phase 6 — equipment/program** 🔶 plate + warm-up calculators done; gym-profile settings UI and
  equipment-aware generation pending.
- **Phase 7 — data** ✅ JSON/CSV export + JSON/Hevy-CSV import (SAF).
- **Phase 8 — fidelity** 🔶 goldens/device pass pending.

## Screen inventory

Authoritative spec: [`references/guides/blueprint.md`](references/guides/blueprint.md)

1. **Today** — resume active workout / start next routine
2. **Routines** — list, routine detail, routine editor
3. **History** — completed workouts + workout detail (with recommendations)
4. **Progress** — estimated weekly sets + exercise progress
5. **Exercise picker** — built-in + custom + search
6. **Custom exercise** — progressive form
7. **Active workout** — set grid, RIR, quality, rest timer
8. **Workout complete** — "next time" recommendations + WHY detail
9. **Exercise detail** — metadata + history
10. **Settings** — units, RIR, hints

## Data

- Local-first, Room-backed, offline. No account, no Google Play Services/Firebase requirement.
- Schema migrations are real; history is never dropped.
- `WorkoutExerciseEntity` snapshots prescription/rep range/RIR/increment for historical integrity.
- Custom and built-in exercises share one `ExerciseDefinition` and one progression engine.

## Commands

```bash
cd apps/training

./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:installDebug

# From repo root
../../scripts/verify-app.sh training
```

## Platform exceptions

| Intended behavior | Compromise |
|-------------------|------------|
| Rest timer survives reboot exactly | After a reboot the app clears a stale rest deadline rather than resuming mid-rest |
| Per-side left/right set logging | v1 logs one value per side when both sides match (unilateral limitation) |
| Visual voicemail-style analytics graphs | v1 prioritizes high-quality history over charts |
| Workout ongoing notification | v1 keeps the active workout screen + rest banner; notification is optional/deferred |

## Reference and golden expectations

- `references/guides/blueprint.md` — authoritative page spec
- `references/research.md` — evidence basis and product-heuristic distinctions
- `references/web-resources.md` — WP8.1 sources
- `references/known-gaps.md` — missing captures

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Agent postmortem

_None._
