# Training — research & rationale

This document records the evidence-informed basis for the progression engine and, crucially,
distinguishes **research-supported principles** from **product heuristics**. The app does not
diagnose, prescribe rehabilitation, or claim physiological certainty.

## Research-supported principles

- **Progressive overload drives adaptation.** Systematically increasing training demand (load,
  reps, or sets) is the core driver of strength and hypertrophy. (American College of Sports
  Medicine position stands on resistance training; Schoenfeld et al. reviews.)
- **Load–strength relationship.** Maximal strength is more specific to higher loads; heavy compound
  work generally favours lower rep ranges and more conservative effort. This does **not** mean heavy
  loading is required for hypertrophy.
- **Hypertrophy across a broad load spectrum.** When sets are sufficiently challenging, hypertrophy
  occurs across wide loading ranges (roughly 5–30 reps at/near similar effort), not only 8–12.
  (Schoenfeld et al., "Strength and Hypertrophy Adaptations Between Low- vs. High-Load Resistance
  Training"; Lasevicius et al.) Therefore rep ranges are **programming choices**, and the app
  supports arbitrary sensible ranges (3–5 … 15–30).
- **Proximity to failure.** Hypertrophy can be achieved without training every set to failure; sets
  taken close to failure (≈0–3 reps in reserve) are generally sufficient, with failure reserving
  extra fatigue. (Zourdos et al. on RIR/RPE; Grgic et al. on failure vs non-failure.) Hence RIR is
  an optional effort signal, not a requirement.
- **RIR/RPE is imperfect.** Estimated RIR/RPE is noisy, especially near failure and for less
  experienced lifters. (Zourdos et al.; Steele et al. on RPE accuracy.) Hence the engine uses
  **zones**, not decimals.
- **Volume dose–response.** More weekly sets generally produce more hypertrophy up to a point, with
  diminishing returns and individual variation; the literature does **not** justify one universal
  "optimal" set range per muscle. (Schoenfeld et al., dose–response meta-analyses.) Hence weekly
  sets are presented as **estimates**, never "optimal".
- **Autoregulation.** Adjusting load/reps to performance and effort is common practice (RPE/RIR
  autoregulation). The engine autoregulates deterministically from logged performance.

## Plan builder — exercise selection

The routine generator (`domain/plan/`) picks exercises from the built-in library using these
evidence-informed rules. They are curated **defaults**, fully editable afterward.

- **Compound-first per muscle.** Multi-joint movements load major muscles effectively and carry
  over to strength; the generator guarantees each target muscle an exercise, leading with the
  highest-priority compound where one exists (ACSM progression models; Schoenfeld & Grgic).
- **Sensible isolation coverage.** Muscles with little compound involvement (side/rear delts,
  biceps, triceps, calves, core) lead with isolation work.
- **Goal-appropriate loading.** `STRENGTH` → lower reps (3–5) and slightly more conservative RIR on
  compounds; `HYPERTROPHY` → compounds plus isolations in moderate/higher rep ranges (6–15, and
  10–20 for isolations); `GENERAL` → balanced. Rep ranges remain programming choices (see above),
  and effort defaults are not physiological laws.
- **Stable, loadable movements** are preferred so progressive overload can be applied cleanly.
- **Ordering:** compounds before isolation within a session (ACSM; Simão et al. on exercise order).
- **Weekly frequency via splits.** The preset is chosen from the requested days/week
  (Full body 1, Upper/Lower 2 or 4, Push/Pull/Legs 3 or 6, body-part split 5). This is a
  convenience mapping, not a claim of optimality.
- **Volume** targets roughly 6 movements/day; the user can add/remove. Weekly sets are only ever
  presented as *estimates*.

Research anchors: Schoenfeld BJ, Grgic J, et al. on training frequency, exercise selection and
variation; ACSM Position Stand on progression models; Simão R, et al. on the effect of exercise
order. These are starting points, not exhaustive.

## Product heuristics (NOT established physiological laws)
These are deliberate, conservative engineering choices so the app is deterministic and explainable:

- **Two consecutive underperformance sessions before a load reduction.** A single poor day is
  treated as noise until confirmed.
- **One top-range exposure without RIR requires a second confirmation before adding load.** The
  engine does not pretend to know effort when RIR is absent.
- **One increment per step.** Load changes are always exactly one configured equipment increment —
  never a percentage computed by the app.
- **Primary = 1.0 set, secondary = 0.5 set** for weekly volume estimation. This is a bookkeeping
  convention, not measured biological stimulus.
- **Long-gap (>28 days) confidence downgrade** and repeat-load default. A heuristic for staleness.
- **Warm-ups and extra sets excluded from progression.** Determinism and fatigue-management
  convenience.
- **`technique limited` blocks load increase; `pain` → REVIEW.** Safety-oriented product policy.

## Explicitly rejected

- One "magic" hypertrophy rep range (e.g. 8–12).
- Requirement of training to failure.
- Muscle-group-specific load increments.
- Sex/age/bodyweight load multipliers.
- Automatic weekly-volume manipulation.
- Estimated recovery/readiness scores.
- Diagnosing or rehabilitating pain.

## Progression algorithm version

Recommendations store `progressionAlgorithmVersion` (currently **1**). Historical recommendations
remain explainable; the engine never silently rewrites them under a new algorithm.

## References (starting points)

- ACSM. *Progression Models in Resistance Training for Healthy Adults.* Med Sci Sports Exerc.
- Schoenfeld BJ, Grgic J, Ogborn D, Krieger JW. *Strength and Hypertrophy Adaptations Between Low-
  vs. High-Load Resistance Training: A Systematic Review and Meta-analysis.* J Strength Cond Res.
- Schoenfeld BJ, Ogborn D, Krieger JW. *Dose-response relationship between weekly resistance
  training volume and increases in muscle mass.* J Sports Sci.
- Lasevicius T, et al. *Effects of different intensities of resistance training with equated volume
  load on muscle strength and hypertrophy.* Eur J Sport Sci.
- Grgic J, et al. *Effects of resistance training performed to repetition failure or non-failure on
  muscular strength and hypertrophy: A systematic review and meta-analysis.* J Sport Health Sci.
- Zourdos MC, et al. *Novel Resistance Training–Specific Rating of Perceived Exertion Scale
  Measuring Repetitions in Reserve.* J Strength Cond Res.
- Steele J, et al. *Sources of variability in the effectiveness of resistance training.*
