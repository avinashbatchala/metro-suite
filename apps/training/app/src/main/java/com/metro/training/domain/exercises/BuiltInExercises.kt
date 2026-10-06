package com.metro.training.domain.exercises

/**
 * Deterministic starter library. Built-ins use the exact same [ExerciseDefinition] schema as
 * custom exercises — no special-casing anywhere in the progression engine.
 *
 * Increments are canonical kilograms; they are equipment-driven defaults the user can override.
 */
object BuiltInExercises {

    val all: List<ExerciseDefinition> = listOf(
        // ---- CHEST ----
        def("barbell_bench_press", "Barbell Bench Press", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
        dumbbell("incline_dumbbell_press", "Incline Dumbbell Press", ExerciseMechanic.COMPOUND, 2.0, MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS),
        def("machine_chest_press", "Machine Chest Press", ExerciseMechanic.COMPOUND, EquipmentType.MACHINE, 2.5, MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
        perSide("cable_fly", "Cable Fly", ExerciseMechanic.ISOLATION, 2.5, MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),

        // ---- BACK ----
        addedWeight("pull_up", "Pull-Up", ExerciseMechanic.COMPOUND, 2.5, MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
        def("lat_pulldown", "Lat Pulldown", ExerciseMechanic.COMPOUND, EquipmentType.CABLE, 2.5, MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
        def("barbell_row", "Barbell Row", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.BICEPS, MuscleGroup.REAR_DELTS),
        def("seated_cable_row", "Seated Cable Row", ExerciseMechanic.COMPOUND, EquipmentType.CABLE, 2.5, MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.BICEPS),

        // ---- QUADS ----
        def("back_squat", "Back Squat", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.LOWER_BACK),
        def("leg_press", "Leg Press", ExerciseMechanic.COMPOUND, EquipmentType.MACHINE, 5.0, MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
        dumbbell("bulgarian_split_squat", "Bulgarian Split Squat", ExerciseMechanic.COMPOUND, 2.0, MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, unilateral = true),
        def("leg_extension", "Leg Extension", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 5.0, MuscleGroup.QUADS),

        // ---- HAMSTRINGS / GLUTES ----
        def("romanian_deadlift", "Romanian Deadlift", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
        def("hip_thrust", "Hip Thrust", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
        def("seated_leg_curl", "Seated Leg Curl", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 5.0, MuscleGroup.HAMSTRINGS),
        def("lying_leg_curl", "Lying Leg Curl", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 5.0, MuscleGroup.HAMSTRINGS),

        // ---- SHOULDERS ----
        def("overhead_press", "Overhead Press", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS, MuscleGroup.SIDE_DELTS),
        dumbbell("dumbbell_shoulder_press", "Dumbbell Shoulder Press", ExerciseMechanic.COMPOUND, 2.0, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
        dumbbell("lateral_raise", "Lateral Raise", ExerciseMechanic.ISOLATION, 2.0, MuscleGroup.SIDE_DELTS),
        def("reverse_pec_deck", "Reverse Pec Deck", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 2.5, MuscleGroup.REAR_DELTS, MuscleGroup.UPPER_BACK),

        // ---- BICEPS ----
        dumbbell("dumbbell_curl", "Dumbbell Curl", ExerciseMechanic.ISOLATION, 2.0, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        def("preacher_curl", "Preacher Curl", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 2.5, MuscleGroup.BICEPS),
        def("cable_curl", "Cable Curl", ExerciseMechanic.ISOLATION, EquipmentType.CABLE, 2.5, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),

        // ---- TRICEPS ----
        def("cable_pushdown", "Cable Pushdown", ExerciseMechanic.ISOLATION, EquipmentType.CABLE, 2.5, MuscleGroup.TRICEPS),
        def("overhead_cable_extension", "Overhead Cable Extension", ExerciseMechanic.ISOLATION, EquipmentType.CABLE, 2.5, MuscleGroup.TRICEPS),
        def("close_grip_bench_press", "Close-Grip Bench Press", ExerciseMechanic.COMPOUND, EquipmentType.BARBELL, 2.5, MuscleGroup.TRICEPS, MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),

        // ---- CALVES ----
        def("standing_calf_raise", "Standing Calf Raise", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 5.0, MuscleGroup.CALVES),
        def("seated_calf_raise", "Seated Calf Raise", ExerciseMechanic.ISOLATION, EquipmentType.MACHINE, 5.0, MuscleGroup.CALVES),

        // ---- CORE ----
        def("cable_crunch", "Cable Crunch", ExerciseMechanic.ISOLATION, EquipmentType.CABLE, 5.0, MuscleGroup.ABS),
        repsOnly("hanging_leg_raise", "Hanging Leg Raise", ExerciseMechanic.ISOLATION, EquipmentType.BODYWEIGHT, MuscleGroup.ABS, MuscleGroup.OBLIQUES),

        // ---- ASSISTED (reverse progression) ----
        assisted("assisted_pull_up", "Assisted Pull-Up", 5.0, MuscleGroup.LATS, MuscleGroup.BICEPS, MuscleGroup.UPPER_BACK),
    )

    val byId: Map<String, ExerciseDefinition> = all.associateBy { it.id }

    private fun def(
        id: String,
        name: String,
        mechanic: ExerciseMechanic,
        equipment: EquipmentType,
        increment: Double,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
    ) = ExerciseDefinition(
        id = id,
        name = name,
        mechanic = mechanic,
        equipment = equipment,
        resistanceModel = ResistanceModel.EXTERNAL_LOAD,
        loadSemantics = LoadSemantics.TOTAL_WEIGHT,
        progressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
        defaultIncrement = increment,
        incrementUnit = LoadUnit.KG,
        primaryMuscles = setOf(primary),
        secondaryMuscles = secondary.toSet(),
        builtIn = true,
    )

    private fun dumbbell(
        id: String,
        name: String,
        mechanic: ExerciseMechanic,
        increment: Double,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
        unilateral: Boolean = false,
    ) = def(id, name, mechanic, EquipmentType.DUMBBELL, increment, primary, *secondary).copy(
        loadSemantics = LoadSemantics.PER_DUMBBELL,
        unilateral = unilateral,
    )

    private fun perSide(
        id: String,
        name: String,
        mechanic: ExerciseMechanic,
        increment: Double,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
    ) = def(id, name, mechanic, EquipmentType.CABLE, increment, primary, *secondary).copy(
        loadSemantics = LoadSemantics.PER_SIDE,
    )

    private fun addedWeight(
        id: String,
        name: String,
        mechanic: ExerciseMechanic,
        increment: Double,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
    ) = def(id, name, mechanic, EquipmentType.BODYWEIGHT, increment, primary, *secondary).copy(
        resistanceModel = ResistanceModel.BODYWEIGHT_PLUS_LOAD,
        loadSemantics = LoadSemantics.ADDED_WEIGHT,
    )

    private fun assisted(
        id: String,
        name: String,
        increment: Double,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
    ) = ExerciseDefinition(
        id = id,
        name = name,
        mechanic = ExerciseMechanic.COMPOUND,
        equipment = EquipmentType.MACHINE,
        resistanceModel = ResistanceModel.ASSISTED_BODYWEIGHT,
        loadSemantics = LoadSemantics.ASSISTANCE_WEIGHT,
        progressionDirection = ProgressionDirection.LESS_LOAD_IS_HARDER,
        defaultIncrement = increment,
        incrementUnit = LoadUnit.KG,
        primaryMuscles = setOf(primary),
        secondaryMuscles = secondary.toSet(),
        builtIn = true,
    )

    private fun repsOnly(
        id: String,
        name: String,
        mechanic: ExerciseMechanic,
        equipment: EquipmentType,
        primary: MuscleGroup,
        vararg secondary: MuscleGroup,
    ) = ExerciseDefinition(
        id = id,
        name = name,
        mechanic = mechanic,
        equipment = equipment,
        resistanceModel = ResistanceModel.REPS_ONLY,
        loadSemantics = LoadSemantics.NONE,
        progressionDirection = ProgressionDirection.REPS_ONLY,
        defaultIncrement = null,
        incrementUnit = null,
        primaryMuscles = setOf(primary),
        secondaryMuscles = secondary.toSet(),
        builtIn = true,
    )
}
