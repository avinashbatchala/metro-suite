# Third-party notices — Metro Training

## Exercise illustrations

- **Source:** free-exercise-db — <https://github.com/yuhonas/free-exercise-db>
- **License:** The Unlicense (public domain) — <http://unlicense.org/>
- **Purpose:** Bundled two-frame exercise illustrations shown in Exercise detail and the active
  workout.
- **Usage:** Images are downloaded at build time from the `exercises/<Id>/0.jpg` and
  `.../1.jpg` paths, downscaled to 512px and re-encoded as WebP under
  `app/src/main/res/drawable-nodpi/training_ex_<exerciseId>_[0|1].webp`.

Exercise-id → dataset-id mapping (all 31 built-ins resolve):

| Training exercise id | free-exercise-db id |
|----------------------|---------------------|
| barbell_bench_press | Barbell_Bench_Press_-_Medium_Grip |
| incline_dumbbell_press | Incline_Dumbbell_Press |
| machine_chest_press | Machine_Bench_Press |
| cable_fly | Cable_Crossover |
| pull_up | Pullups |
| lat_pulldown | Wide-Grip_Lat_Pulldown |
| barbell_row | Bent_Over_Barbell_Row |
| seated_cable_row | Seated_Cable_Rows |
| back_squat | Barbell_Squat |
| leg_press | Leg_Press |
| bulgarian_split_squat | Split_Squat_with_Dumbbells |
| leg_extension | Leg_Extensions |
| romanian_deadlift | Romanian_Deadlift |
| hip_thrust | Barbell_Hip_Thrust |
| seated_leg_curl | Seated_Leg_Curl |
| lying_leg_curl | Lying_Leg_Curls |
| overhead_press | Standing_Military_Press |
| dumbbell_shoulder_press | Dumbbell_Shoulder_Press |
| lateral_raise | Side_Lateral_Raise |
| reverse_pec_deck | Reverse_Machine_Flyes |
| dumbbell_curl | Dumbbell_Bicep_Curl |
| preacher_curl | Preacher_Curl |
| cable_curl | Standing_Biceps_Cable_Curl |
| cable_pushdown | Triceps_Pushdown |
| overhead_cable_extension | Cable_Rope_Overhead_Triceps_Extension |
| close_grip_bench_press | Close-Grip_Barbell_Bench_Press |
| standing_calf_raise | Standing_Calf_Raises |
| seated_calf_raise | Seated_Calf_Raise |
| cable_crunch | Cable_Crunch |
| hanging_leg_raise | Hanging_Leg_Raise |
| assisted_pull_up | Band_Assisted_Pull-Up |

Custom exercises have no bundled illustration; the UI falls back to a Canvas glyph. Any dataset
image that is later found to carry a different license will be attributed here before release.
