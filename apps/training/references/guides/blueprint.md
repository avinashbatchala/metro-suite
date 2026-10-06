# Training — blueprint

**Authoritative spec for this app.** Target: **Windows Phone 8.1** Metro presentation on a portrait
phone (768×1280 / xhdpi). This is a MetroSuite design, not a Microsoft product.

Core rule: **log training → understand performance → prescribe next exposure.** Windows Phone 8.1
presentation, evidence-informed resistance-training engine.

---

## 0. The progression engine

The engine is pure Kotlin (`domain/progression/`) and produces exactly one of:

```text
ADD_REPS  ADD_LOAD  HOLD  REDUCE_LOAD  REVIEW
```

Every recommendation carries structured `reasons` and a plain-language `explanation` ("why?").

### Double progression (`DOUBLE_PROGRESSION`)

- Accumulate reps within `repMin..repMax` at a fixed load.
- When every prescribed work set reaches `repMax` with acceptable effort and valid technique →
  `ADD_LOAD` by one equipment increment.
- Above `repMin` but not `repMax` → `ADD_REPS` (goal = previous total + 1, still ≤ `repMax` each).
- Otherwise → `HOLD` / `REDUCE_LOAD` / `REVIEW` per the rules below.

### RIR

- RIR is optional and **noisy**. Use practical zones (`targetRirMin..targetRirMax`), never decimals.
- `ADD_LOAD` requires every set at top range and effort **not materially harder** than target
  (a `0 RIR` when target is `1–3` holds instead). Being easier than target never withholds
  progression.

### No-RIR (conservative)

- First top-range exposure → `HOLD` (confirmation). Second consecutive → `ADD_LOAD`.
- No RIR ever changes the increment; it only changes *when* load is added.

### Quality

- `technique limited` → never grant `ADD_LOAD` from that exposure (`HOLD`).
- `failed rep` → the failed attempt is not counted; usually `HOLD`.
- `pain` → `REVIEW`; Auto Progress paused. Never diagnose.

### Underperformance

- `HOLD` after one poor exposure. `REDUCE_LOAD` (one increment) only after **two consecutive**
  comparable poor exposures where the below-min sets were genuinely difficult.
- Below range but easy (high RIR) → `HOLD`/`REVIEW`, never an automatic reduction.

### Direction / semantics

- `MORE_LOAD_IS_HARDER` → `nextLoad = load + increment`.
- `LESS_LOAD_IS_HARDER` (assisted) → `nextLoad = assistance − increment` (UI: "reduce assistance").
- `REPS_ONLY` → reps/`REVIEW` ceiling; no invented load.
- `PER_DUMBBELL` increments per implement; `PER_SIDE` shows "kg / side".

### Comparability / epochs / gaps / confidence

- Comparable = same exercise + rep range + work-set count + load semantics + direction.
- A programming change starts a new **progression epoch** (old history stays visible).
- Gap > `longGapDays` (default 28) → `LOW` confidence, hold load.
- Confidence: `HIGH` (RIR, complete, comparable, clean), `MEDIUM` (no RIR confirmed), `LOW`
  (missing data / gap / program change / mixed loads / quality issue).

### Mixed loads

- Work sets at different loads with no explicit top/backoff structure → `HOLD`, `LOW` confidence.
  Never average.

### Warm-ups / extra sets

- Only prescribed `WORK` sets feed progression. Warm-ups, drop, backoff and extra sets are logged
  and may count toward weekly volume but never change the prescription.

### Non-coercive

- The user owns the workout. Recommendations prefill; they never lock load/reps/sets.
- Recommendations record `recommended` vs `accepted` (with algorithm version).

---

## 1. Data model

- `ExerciseDefinition` — identity + progression metadata (mechanic, equipment, resistance model,
  load semantics, progression direction, increment, muscles, unilateral, builtIn). No rep range
  here.
- `RoutineExercisePrescription` — sets, `repMin`/`repMax`, target RIR, rest, auto-progress,
  policy, optional increment override, note. A fixed rep range lives **here**, never on the
  exercise.
- `Workout` / `WorkoutExercise` / `WorkoutSet` — session with snapshots for historical integrity.
- Built-in and custom exercises are the same schema.

---

## 2. Root — `training`

WP8.1 Pivot:

```text
training

today     routines     history     progress
```

ApplicationBar (context): `start` / `+` / `…` overflow (`settings`). No FAB, no bottom navigation.

### Today

Active workout → `resume`; else next routine → `start workout`; else empty state.

### Routines

List of routines (`name`, exercise/work-set count). Tap → routine detail. Overflow: new routine,
delete.

### History

Chronological completed workouts grouped by month (`month`, routine, `min · sets`). Tap → workout
detail with that workout's recommendations.

### Progress

`estimated weekly sets` per muscle (primary 1.0, secondary 0.5), compared with last week; then
`exercise progress` list (tap → exercise detail). Never calls volume "optimal".

---

## 3. Subpages

### Onboarding / plan builder (first run)

Shown before the main shell on first launch (and from Settings → `rebuild plan`). WP8.1-styled
steps: `units & goal` → `effort logging` → `training days` → `muscle focus`. The final step builds
concrete routines from the built-in library via the deterministic plan generator (compound-first,
covers every target muscle, goal-appropriate rep/RIR defaults), saves them, and opens Today.
`skip` exits to an empty app.

### Routine detail / editor

- Name field; ordered exercises with `sets × repMin–repMax [· RIR] · auto/manual`.
- Tap a row → prescription editor (sets, rep min/max, RIR min/max, rest).
- Long press → Metro context menu (`move up`, `move down`, `remove`).
- AppBar: `save`, `add`; overflow `delete routine`.

### Exercise picker

Search + category (`all` / `custom`). Built-in and custom together. AppBar `+` → custom exercise.

### Custom exercise

Progressive reveal. Name → movement → equipment → resistance → (load-based) weight-entry,
progression, minimum increment → primary/secondary muscles. Shows `Auto Progress: available` /
`Auto Progress: needs weight increment`. Same schema as built-ins.

### Active workout

Per exercise: a header (bundled illustration, name, `next · repMin–repMax · RIR`), then an inline
editable grid `SET | LOAD | REPS | RIR | ✓` — no per-set dialog. LOAD/REPS are inline numeric
fields (system keyboard) with `−`/`+` steppers; the first set's load and reps copy live to the
remaining sets until a set is edited by hand; RIR is a `– 0 1 2 3 4 5+` segmented row; the set
number opens the quality menu. `+ set`, `+ warm-up`, rest bar, finish, discard. AppBar: `add`
exercise, `finish`; overflow `discard workout`.

### Workout complete

`workout complete`, duration + work sets, then `next time` rows with `↑ load` / `↑ reps` / `hold` /
`↓ load` / `review`. Tap a row → recommendation detail.

### Recommendation detail (WHY)

Exercise, decision, next load, then `why?`: last performance and the structured reasons as prose,
ending with the next available load. Never "the AI determined…".

### Exercise detail

Bundled illustration (two-frame flip) + name, metadata (mechanic, equipment, weight entry,
progression, increment, muscles), then a **progress chart** with a metric switch
(estimated 1RM / top-set load / total reps / volume load) and a range switch
(week / month / 3m / 6m / year / all), followed by comparable history.

Charts are custom Canvas drawings (accent line/bars, thin rules) — never a third-party/Material
chart library.

### Settings

`units` (kg/lb), `log RIR`, `smart progression hints`. Theme/accent follow MetroOS.

### Live Tile

Title `Training`; back face shows the active workout (routine + current exercise) or the next
routine. Updates on semantic events only.

---

## 4. Out of scope (v1)

AMRAP-aware policies, per-side left/right logging, charts, export/import, workout notifications,
social features, wearable/cloud AI. See `README.md` § Platform exceptions.

## 5. Images & gaps

See [`known-gaps.md`](known-gaps.md) — this app is a MetroSuite original, so WP8.1 captures cover
layout grammar (Pivot, ApplicationBar, list, dialogs) rather than literal Training screens.
