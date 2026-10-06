package com.metro.training.domain.progression

import com.metro.training.domain.exercises.EquipmentType
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ExerciseMechanic
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.LoadUnit
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.exercises.ResistanceModel
import com.metro.training.domain.routines.ProgressionPolicy
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DoubleProgressionEngineTest {

    private val engine = DoubleProgressionEngine()
    private val now = 1_700_000_000_000L

    // ---- ADD_LOAD --------------------------------------------------------

    @Test
    fun addLoad_whenAllSetsTopRangeAndRirWithinTarget() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure(
            "e1", bench, prescription, now - 1_000,
            ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 1),
        )
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, rec.decision)
        assertEquals(82.5, rec.nextLoad!!, 1e-9)
        assertTrue(rec.reasons.contains(ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE))
        assertTrue(rec.reasons.contains(ProgressionReason.RIR_WITHIN_TARGET))
        assertEquals(ProgressionConfidence.HIGH, rec.confidence)
    }

    @Test
    fun addLoad_whenRirEasierThanTarget() {
        val bench = bench()
        val prescription = prescription(rirMin = 1, rirMax = 3)
        val exposure = exposure(
            "e1", bench, prescription, now - 1_000,
            ws(1, 80.0, 10, 4), ws(2, 80.0, 10, 4), ws(3, 80.0, 10, 3),
        )
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.RIR_EASIER_THAN_TARGET))
    }

    @Test
    fun hold_whenOneSetIsMateriallyHarderThanTarget() {
        val bench = bench()
        val prescription = prescription(rirMin = 2, rirMax = 3)
        val exposure = exposure(
            "e1", bench, prescription, now - 1_000,
            ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 0),
        )
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.RIR_HARDER_THAN_TARGET))
    }

    @Test
    fun hold_whenTechniqueLimitedEvenAtTopRange() {
        val bench = bench()
        val exposure = exposure(
            "e1", bench, prescription(), now - 1_000,
            ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2, SetQuality.TECHNIQUE_LIMITED), ws(3, 80.0, 10, 2),
        )
        val rec = engine.evaluate(bench, prescription(), exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.TECHNIQUE_LIMITED))
    }

    @Test
    fun review_whenPainReported() {
        val bench = bench()
        val exposure = exposure(
            "e1", bench, prescription(), now - 1_000,
            ws(1, 80.0, 8, 2, SetQuality.PAIN), ws(2, 80.0, 8, 2), ws(3, 80.0, 7, 2),
        )
        val rec = engine.evaluate(bench, prescription(), exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.REVIEW, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.PAIN_REPORTED))
    }

    @Test
    fun hold_whenFailedRepAtTopRange() {
        val bench = bench()
        val exposure = exposure(
            "e1", bench, prescription(), now - 1_000,
            ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 2, SetQuality.FAILED_REP),
        )
        val rec = engine.evaluate(bench, prescription(), exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.FAILED_REP))
    }

    // ---- no RIR ----------------------------------------------------------

    @Test
    fun noRir_firstTopRangeExposureHoldsForConfirmation() {
        val legPress = legPress()
        val prescription = prescription(exerciseId = "legpress", rirMin = null, rirMax = null, repMin = 8, repMax = 12, incrementOverride = null)
        val exposure = exposure("e1", legPress, prescription, now - 1_000, ws(1, 100.0, 12), ws(2, 100.0, 12), ws(3, 100.0, 12))
        val rec = engine.evaluate(legPress, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.CONFIRMATION_REQUIRED))
    }

    @Test
    fun noRir_secondConsecutiveTopRangeAddsLoad() {
        val legPress = legPress()
        val prescription = prescription(exerciseId = "legpress", rirMin = null, rirMax = null, repMin = 8, repMax = 12)
        val first = exposure("e1", legPress, prescription, now - TimeUnit.DAYS.toMillis(2), ws(1, 100.0, 12), ws(2, 100.0, 12), ws(3, 100.0, 12))
        val second = exposure("e2", legPress, prescription, now - 1_000, ws(1, 100.0, 12), ws(2, 100.0, 12), ws(3, 100.0, 12))
        val rec = engine.evaluate(legPress, prescription, second, listOf(first), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, rec.decision)
        assertEquals(105.0, rec.nextLoad!!, 1e-9)
    }

    @Test
    fun noRir_topRangeThenUnderperformanceResetsConfirmation() {
        val legPress = legPress()
        val prescription = prescription(exerciseId = "legpress", rirMin = null, rirMax = null, repMin = 8, repMax = 12)
        val top = exposure("e1", legPress, prescription, now - TimeUnit.DAYS.toMillis(4), ws(1, 100.0, 12), ws(2, 100.0, 12), ws(3, 100.0, 12))
        val poor = exposure("e2", legPress, prescription, now - 1_000, ws(1, 100.0, 6), ws(2, 100.0, 6), ws(3, 100.0, 5))
        val rec = engine.evaluate(legPress, prescription, poor, listOf(top), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
    }

    // ---- ADD_REPS --------------------------------------------------------

    @Test
    fun addReps_whenAboveMinButNotAtTop() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure("e1", bench, prescription, now - 1_000, ws(1, 80.0, 8, 2), ws(2, 80.0, 8, 2), ws(3, 80.0, 7, 2))
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_REPS, rec.decision)
        assertEquals(80.0, rec.nextLoad!!, 1e-9)
        assertEquals(24, rec.repTarget!!.totalRepsGoal)
        assertEquals(6, rec.repTarget.repMin)
        assertEquals(10, rec.repTarget.repMax)
    }

    // ---- REDUCE_LOAD -----------------------------------------------------

    @Test
    fun hold_onSinglePoorExposure() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure("e1", bench, prescription, now - 1_000, ws(1, 100.0, 5, 1), ws(2, 100.0, 5, 1), ws(3, 100.0, 4, 0))
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
    }

    @Test
    fun reduceLoad_onSecondConsecutivePoorExposure() {
        val bench = bench()
        val prescription = prescription()
        val first = exposure("e1", bench, prescription, now - TimeUnit.DAYS.toMillis(3), ws(1, 100.0, 5, 1), ws(2, 100.0, 5, 1), ws(3, 100.0, 4, 0))
        val second = exposure("e2", bench, prescription, now - 1_000, ws(1, 100.0, 5, 1), ws(2, 100.0, 4, 0), ws(3, 100.0, 4, 0))
        val rec = engine.evaluate(bench, prescription, second, listOf(first), nowMillis = now)
        assertEquals(ProgressionDecision.REDUCE_LOAD, rec.decision)
        assertEquals(97.5, rec.nextLoad!!, 1e-9)
        assertTrue(rec.reasons.contains(ProgressionReason.SECOND_CONSECUTIVE_UNDERPERFORMANCE))
    }

    @Test
    fun noReduction_whenBelowRangeButEasy() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure("e1", bench, prescription, now - 1_000, ws(1, 100.0, 4, 5), ws(2, 100.0, 4, 5), ws(3, 100.0, 4, 5))
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.BELOW_RANGE_BUT_EASY))
    }

    // ---- assisted --------------------------------------------------------

    @Test
    fun assisted_reducesAssistanceAtTopRangeAndNeverAdds() {
        val assisted = assistedPullUp()
        val prescription = prescription(exerciseId = "assist", repMin = 6, repMax = 10, rirMin = 1, rirMax = 3)
        val exposure = exposure("e1", assisted, prescription, now - 1_000, ws(1, 40.0, 10, 2), ws(2, 40.0, 10, 2), ws(3, 40.0, 10, 2))
        val rec = engine.evaluate(assisted, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, rec.decision)
        assertEquals(35.0, rec.nextLoad!!, 1e-9)
        assertEquals("reduce assistance", rec.explanation.headline)
    }

    // ---- rounding --------------------------------------------------------

    @Test
    fun weightMath_snapsCleanly() {
        assertEquals(82.5, WeightMath.increase(80.0, 2.5, ProgressionDirection.MORE_LOAD_IS_HARDER)!!, 1e-9)
        assertEquals(8.5, WeightMath.increase(7.5, 1.0, ProgressionDirection.MORE_LOAD_IS_HARDER)!!, 1e-9)
        assertEquals(45.0, WeightMath.increase(50.0, 5.0, ProgressionDirection.LESS_LOAD_IS_HARDER)!!, 1e-9)
        assertEquals(55.0, WeightMath.increase(50.0, 5.0, ProgressionDirection.MORE_LOAD_IS_HARDER)!!, 1e-9)
    }

    // ---- mixed loads / incomplete / gap ---------------------------------

    @Test
    fun mixedLoads_holdWithLowConfidence() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure("e1", bench, prescription, now - 1_000, ws(1, 80.0, 10, 2), ws(2, 75.0, 10, 2), ws(3, 70.0, 10, 2))
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertEquals(ProgressionConfidence.LOW, rec.confidence)
        assertTrue(rec.reasons.contains(ProgressionReason.MIXED_LOADS))
    }

    @Test
    fun incompleteWorkSets_holdWithLowConfidence() {
        val bench = bench()
        val prescription = prescription()
        val exposure = exposure("e1", bench, prescription, now - 1_000, ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2))
        val rec = engine.evaluate(bench, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertEquals(ProgressionConfidence.LOW, rec.confidence)
        assertTrue(rec.reasons.contains(ProgressionReason.INCOMPLETE_WORK_SETS))
    }

    @Test
    fun longGap_holdsWithLowConfidence() {
        val bench = bench()
        val prescription = prescription()
        val old = exposure("e1", bench, prescription, now - TimeUnit.DAYS.toMillis(100), ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 2))
        val current = exposure("e2", bench, prescription, now - 1_000, ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 2))
        val rec = engine.evaluate(bench, prescription, current, listOf(old), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertEquals(ProgressionConfidence.LOW, rec.confidence)
        assertTrue(rec.reasons.contains(ProgressionReason.LONG_TRAINING_GAP))
    }

    // ---- epochs ----------------------------------------------------------

    @Test
    fun programChange_startsNewEpochAndBlocksImmediateAddLoad() {
        val bench = bench()
        val oldPrescription = prescription(repMin = 6, repMax = 10)
        val newPrescription = prescription(repMin = 10, repMax = 15)
        val oldTop = exposure("e1", bench, oldPrescription, now - TimeUnit.DAYS.toMillis(4), ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 2))
        val newTop = exposure("e2", bench, newPrescription, now - 1_000, ws(1, 80.0, 15, 2), ws(2, 80.0, 15, 2), ws(3, 80.0, 15, 2))
        val rec = engine.evaluate(bench, newPrescription, newTop, listOf(oldTop), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.NEW_EPOCH))
    }

    // ---- custom exercise -------------------------------------------------

    @Test
    fun customExercise_participatesIdentically() {
        val custom = ExerciseDefinition(
            id = "custom_cable_unicorn_raise",
            name = "Cable Unicorn Raise",
            mechanic = ExerciseMechanic.ISOLATION,
            equipment = EquipmentType.CABLE,
            resistanceModel = ResistanceModel.EXTERNAL_LOAD,
            loadSemantics = LoadSemantics.PER_SIDE,
            progressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
            defaultIncrement = 1.25,
            incrementUnit = LoadUnit.KG,
            primaryMuscles = setOf(MuscleGroup.SIDE_DELTS),
            secondaryMuscles = emptySet(),
            builtIn = false,
        )
        val prescription = prescription(exerciseId = custom.id, repMin = 10, repMax = 15, rirMin = 1, rirMax = 3)
        val exposure = exposure("e1", custom, prescription, now - 1_000, ws(1, 10.0, 15, 2), ws(2, 10.0, 15, 2), ws(3, 10.0, 15, 2))
        val rec = engine.evaluate(custom, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, rec.decision)
        assertEquals(11.25, rec.nextLoad!!, 1e-9)
    }

    // ---- end-to-end sequences -------------------------------------------

    @Test
    fun endToEnd_compoundDoubleProgression() {
        val bench = bench()
        val prescription = prescription()

        val s1 = exposure("s1", bench, prescription, now - TimeUnit.DAYS.toMillis(7), ws(1, 80.0, 8, 2), ws(2, 80.0, 8, 2), ws(3, 80.0, 7, 2))
        val r1 = engine.evaluate(bench, prescription, s1, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_REPS, r1.decision)
        assertEquals(24, r1.repTarget!!.totalRepsGoal)

        val s2 = exposure("s2", bench, prescription, now - TimeUnit.DAYS.toMillis(3), ws(1, 80.0, 10, 2), ws(2, 80.0, 10, 2), ws(3, 80.0, 10, 1))
        val r2 = engine.evaluate(bench, prescription, s2, listOf(s1), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, r2.decision)
        assertEquals(82.5, r2.nextLoad!!, 1e-9)

        val s3 = exposure("s3", bench, prescription, now - 1_000, ws(1, 82.5, 8), ws(2, 82.5, 7), ws(3, 82.5, 6))
        val r3 = engine.evaluate(bench, prescription, s3, listOf(s2, s1), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_REPS, r3.decision)
        assertEquals(82.5, r3.nextLoad!!, 1e-9)
    }

    @Test
    fun endToEnd_isolationCoarseJumps() {
        val lateral = lateralRaise()
        val prescription = prescription(exerciseId = "lateral", repMin = 10, repMax = 20, rirMin = 0, rirMax = 2)
        val s1 = exposure("s1", lateral, prescription, now - TimeUnit.DAYS.toMillis(9), ws(1, 10.0, 15, 2), ws(2, 10.0, 14, 2), ws(3, 10.0, 12, 1))
        val r1 = engine.evaluate(lateral, prescription, s1, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_REPS, r1.decision)

        val s2 = exposure("s2", lateral, prescription, now - TimeUnit.DAYS.toMillis(5), ws(1, 10.0, 18, 2), ws(2, 10.0, 17, 1), ws(3, 10.0, 16, 1))
        val r2 = engine.evaluate(lateral, prescription, s2, listOf(s1), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_REPS, r2.decision)

        val s3 = exposure("s3", lateral, prescription, now - 1_000, ws(1, 10.0, 20, 2), ws(2, 10.0, 20, 2), ws(3, 10.0, 20, 1))
        val r3 = engine.evaluate(lateral, prescription, s3, listOf(s2, s1), nowMillis = now)
        assertEquals(ProgressionDecision.ADD_LOAD, r3.decision)
        assertEquals(12.0, r3.nextLoad!!, 1e-9)
    }

    @Test
    fun endToEnd_underperformanceReducesByOneIncrement() {
        val squat = squat()
        val prescription = prescription(exerciseId = "squat", repMin = 5, repMax = 8, rirMin = null, rirMax = null)
        val a = exposure("a", squat, prescription, now - TimeUnit.DAYS.toMillis(4), ws(1, 120.0, 5), ws(2, 120.0, 4), ws(3, 120.0, 4))
        val rA = engine.evaluate(squat, prescription, a, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.HOLD, rA.decision)

        val b = exposure("b", squat, prescription, now - 1_000, ws(1, 120.0, 4), ws(2, 120.0, 4), ws(3, 120.0, 3))
        val rB = engine.evaluate(squat, prescription, b, listOf(a), nowMillis = now)
        assertEquals(ProgressionDecision.REDUCE_LOAD, rB.decision)
        assertEquals(117.5, rB.nextLoad!!, 1e-9)
    }

    @Test
    fun repsOnly_reachesCeilingAndReviews() {
        val pushUp = ExerciseDefinition(
            id = "pushup",
            name = "Push-Up",
            mechanic = ExerciseMechanic.COMPOUND,
            equipment = EquipmentType.BODYWEIGHT,
            resistanceModel = ResistanceModel.REPS_ONLY,
            loadSemantics = LoadSemantics.NONE,
            progressionDirection = ProgressionDirection.REPS_ONLY,
            defaultIncrement = null,
            incrementUnit = null,
            primaryMuscles = setOf(MuscleGroup.CHEST),
            secondaryMuscles = setOf(MuscleGroup.TRICEPS),
            builtIn = true,
        )
        val prescription = prescription(exerciseId = "pushup", repMin = 10, repMax = 20, rirMin = null, rirMax = null)
        val exposure = exposure("e1", pushUp, prescription, now - 1_000, ws(1, null, 20), ws(2, null, 20), ws(3, null, 20))
        val rec = engine.evaluate(pushUp, prescription, exposure, emptyList(), nowMillis = now)
        assertEquals(ProgressionDecision.REVIEW, rec.decision)
        assertTrue(rec.reasons.contains(ProgressionReason.REPS_ONLY_CEILING))
        assertNull(rec.nextLoad)
    }

    // ---- helpers ---------------------------------------------------------

    private fun bench() = ExerciseDefinition(
        id = "bench",
        name = "Barbell Bench Press",
        mechanic = ExerciseMechanic.COMPOUND,
        equipment = EquipmentType.BARBELL,
        resistanceModel = ResistanceModel.EXTERNAL_LOAD,
        loadSemantics = LoadSemantics.TOTAL_WEIGHT,
        progressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
        defaultIncrement = 2.5,
        incrementUnit = LoadUnit.KG,
        primaryMuscles = setOf(MuscleGroup.CHEST),
        secondaryMuscles = setOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
        builtIn = true,
    )

    private fun legPress() = bench().copy(
        id = "legpress", name = "Leg Press", equipment = EquipmentType.MACHINE,
        defaultIncrement = 5.0, primaryMuscles = setOf(MuscleGroup.QUADS),
        secondaryMuscles = setOf(MuscleGroup.GLUTES),
    )

    private fun squat() = bench().copy(
        id = "squat", name = "Back Squat", defaultIncrement = 2.5,
        primaryMuscles = setOf(MuscleGroup.QUADS),
        secondaryMuscles = setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
    )

    private fun lateralRaise() = ExerciseDefinition(
        id = "lateral",
        name = "Lateral Raise",
        mechanic = ExerciseMechanic.ISOLATION,
        equipment = EquipmentType.DUMBBELL,
        resistanceModel = ResistanceModel.EXTERNAL_LOAD,
        loadSemantics = LoadSemantics.PER_DUMBBELL,
        progressionDirection = ProgressionDirection.MORE_LOAD_IS_HARDER,
        defaultIncrement = 2.0,
        incrementUnit = LoadUnit.KG,
        primaryMuscles = setOf(MuscleGroup.SIDE_DELTS),
        secondaryMuscles = emptySet(),
        builtIn = true,
    )

    private fun assistedPullUp() = ExerciseDefinition(
        id = "assist",
        name = "Assisted Pull-Up",
        mechanic = ExerciseMechanic.COMPOUND,
        equipment = EquipmentType.MACHINE,
        resistanceModel = ResistanceModel.ASSISTED_BODYWEIGHT,
        loadSemantics = LoadSemantics.ASSISTANCE_WEIGHT,
        progressionDirection = ProgressionDirection.LESS_LOAD_IS_HARDER,
        defaultIncrement = 5.0,
        incrementUnit = LoadUnit.KG,
        primaryMuscles = setOf(MuscleGroup.LATS),
        secondaryMuscles = setOf(MuscleGroup.BICEPS),
        builtIn = true,
    )

    private fun prescription(
        exerciseId: String = "bench",
        sets: Int = 3,
        repMin: Int = 6,
        repMax: Int = 10,
        rirMin: Int? = 1,
        rirMax: Int? = 3,
        incrementOverride: Double? = null,
        policy: ProgressionPolicy = ProgressionPolicy(),
    ) = RoutineExercisePrescription(
        id = "$exerciseId-p",
        exerciseId = exerciseId,
        workSetCount = sets,
        repMin = repMin,
        repMax = repMax,
        targetRirMin = rirMin,
        targetRirMax = rirMax,
        restSeconds = 120,
        autoProgressEnabled = true,
        progressionPolicy = policy,
        incrementOverrideKg = incrementOverride,
    )

    private fun exposure(
        id: String,
        exercise: ExerciseDefinition,
        prescription: RoutineExercisePrescription,
        performedAt: Long,
        vararg sets: WorkoutSet,
    ): CompletedExerciseExposure {
        val snapshot = ExposureSnapshot(
            repMin = prescription.repMin,
            repMax = prescription.repMax,
            workSetCount = prescription.workSetCount,
            targetRirMin = prescription.targetRirMin,
            targetRirMax = prescription.targetRirMax,
            loadSemantics = exercise.loadSemantics,
            progressionDirection = exercise.progressionDirection,
            incrementKg = prescription.incrementOverrideKg ?: exercise.defaultIncrement,
        )
        return CompletedExerciseExposure(
            id = id,
            exerciseId = exercise.id,
            prescriptionId = prescription.id,
            performedAt = performedAt,
            snapshot = snapshot,
            sets = sets.toList(),
        )
    }

    private fun ws(
        index: Int,
        load: Double?,
        reps: Int,
        rir: Int? = null,
        quality: SetQuality = SetQuality.NORMAL,
        setType: SetType = SetType.WORK,
        prescribed: Boolean = true,
        completed: Boolean = true,
        session: String = "s",
    ) = WorkoutSet(
        id = "$session-$index",
        exerciseSessionId = session,
        setIndex = index,
        setType = setType,
        load = load,
        repsCompleted = reps,
        rir = rir,
        quality = quality,
        completed = completed,
        prescribed = prescribed,
    )
}
