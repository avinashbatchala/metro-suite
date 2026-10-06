package com.metro.training.domain.progression

import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.exercises.ResistanceModel
import com.metro.training.domain.routines.ProgressionPolicy
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.workout.ExposureSnapshot
import com.metro.training.domain.workout.WorkoutSet
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Pure, deterministic, offline progression engine. No Android dependencies.
 *
 * The engine only ever looks at prescription parameters, the current exposure's work sets and a
 * small window of comparable history. It never uses muscle groups, sex, age, body weight or
 * body-fat to change load.
 */
interface ProgressionEngine {
    fun evaluate(
        exercise: ExerciseDefinition,
        prescription: RoutineExercisePrescription,
        currentExposure: CompletedExerciseExposure,
        history: List<CompletedExerciseExposure>,
        policy: ProgressionPolicy = prescription.progressionPolicy,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProgressionRecommendation
}

object ProgressionAlgorithm {
    const val VERSION = 1
    private const val LOAD_EPSILON = 0.0001
    fun loadsEqual(a: Double, b: Double): Boolean = abs(a - b) < LOAD_EPSILON
}

class DoubleProgressionEngine : ProgressionEngine {

    override fun evaluate(
        exercise: ExerciseDefinition,
        prescription: RoutineExercisePrescription,
        currentExposure: CompletedExerciseExposure,
        history: List<CompletedExerciseExposure>,
        policy: ProgressionPolicy,
        nowMillis: Long,
    ): ProgressionRecommendation {
        val comparableHistory = comparableHistory(currentExposure, history)
        val previous = comparableHistory.firstOrNull()
        val hadHistoryForExercise = history.any { it.exerciseId == currentExposure.exerciseId }
        val programChanged = hadHistoryForExercise && comparableHistory.isEmpty()

        val analysis = analyze(currentExposure, prescription)
        val increment = prescription.incrementOverrideKg ?: exercise.defaultIncrement
        val repsOnly = exercise.resistanceModel == ResistanceModel.REPS_ONLY ||
            exercise.progressionDirection == ProgressionDirection.REPS_ONLY

        val longGap = previous != null &&
            nowMillis - previous.performedAt > TimeUnit.DAYS.toMillis(policy.longGapDays.toLong())

        val underperformsNow = underperforms(currentExposure, prescription)
        val underperformedBefore = previous?.let { underperforms(it, prescription) } == true

        val reasons = mutableListOf<ProgressionReason>()
        var decision: ProgressionDecision
        var confidence = ProgressionConfidence.HIGH
        var nextLoad: Double? = analysis.anchoredLoad
        var repTarget: RepTarget? = null

        when {
            analysis.hasPain -> {
                decision = ProgressionDecision.REVIEW
                confidence = ProgressionConfidence.LOW
                reasons += ProgressionReason.PAIN_REPORTED
                nextLoad = analysis.anchoredLoad
            }
            analysis.completedCount < prescription.workSetCount -> {
                decision = ProgressionDecision.HOLD
                confidence = ProgressionConfidence.LOW
                reasons += ProgressionReason.INCOMPLETE_WORK_SETS
                nextLoad = analysis.anchoredLoad
            }
            analysis.mixedLoads -> {
                decision = ProgressionDecision.HOLD
                confidence = ProgressionConfidence.LOW
                reasons += ProgressionReason.MIXED_LOADS
                nextLoad = analysis.anchoredLoad
            }
            longGap -> {
                decision = ProgressionDecision.HOLD
                confidence = ProgressionConfidence.LOW
                reasons += ProgressionReason.LONG_TRAINING_GAP
                nextLoad = analysis.anchoredLoad
            }
            underperformsNow && underperformedBefore && !repsOnly -> {
                decision = ProgressionDecision.REDUCE_LOAD
                confidence = if (analysis.rirConfigured) {
                    ProgressionConfidence.MEDIUM
                } else {
                    ProgressionConfidence.LOW
                }
                reasons += ProgressionReason.SECOND_CONSECUTIVE_UNDERPERFORMANCE
                reasons += if (analysis.allBelowMin) {
                    ProgressionReason.ALL_SETS_BELOW_MIN
                } else {
                    ProgressionReason.SOME_SETS_BELOW_MIN
                }
                nextLoad = analysis.anchoredLoad?.let { current ->
                    increment?.let { step ->
                        WeightMath.decrease(current, step, exercise.progressionDirection)
                    }
                } ?: analysis.anchoredLoad
            }
            underperformsNow -> {
                decision = ProgressionDecision.HOLD
                confidence = ProgressionConfidence.LOW
                reasons += ProgressionReason.UNDERPERFORMANCE
                reasons += if (analysis.allBelowMin) {
                    ProgressionReason.ALL_SETS_BELOW_MIN
                } else {
                    ProgressionReason.SOME_SETS_BELOW_MIN
                }
                nextLoad = analysis.anchoredLoad
            }
            analysis.allAtTop -> {
                when {
                    repsOnly -> {
                        decision = ProgressionDecision.REVIEW
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.REPS_ONLY_CEILING
                        nextLoad = null
                    }
                    analysis.hasTechniqueLimited -> {
                        decision = ProgressionDecision.HOLD
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.TECHNIQUE_LIMITED
                    }
                    analysis.hasFailedRep -> {
                        decision = ProgressionDecision.HOLD
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.FAILED_REP
                    }
                    analysis.rirAvailable && analysis.rirMateriallyHarder -> {
                        decision = ProgressionDecision.HOLD
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.RIR_HARDER_THAN_TARGET
                    }
                    analysis.rirAvailable -> {
                        decision = ProgressionDecision.ADD_LOAD
                        confidence = ProgressionConfidence.HIGH
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += if (analysis.rirMateriallyEasier) {
                            ProgressionReason.RIR_EASIER_THAN_TARGET
                        } else {
                            ProgressionReason.RIR_WITHIN_TARGET
                        }
                        reasons += ProgressionReason.EQUIPMENT_INCREMENT
                        nextLoad = increaseLoad(analysis.anchoredLoad, increment, exercise.progressionDirection)
                    }
                    policy.requireTwoTopRangeSessionsWithoutRir &&
                        previous != null && previousAllAtTop(previous) -> {
                        decision = ProgressionDecision.ADD_LOAD
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.NO_RIR_CONSERVATIVE
                        reasons += ProgressionReason.EQUIPMENT_INCREMENT
                        nextLoad = increaseLoad(analysis.anchoredLoad, increment, exercise.progressionDirection)
                    }
                    else -> {
                        decision = ProgressionDecision.HOLD
                        confidence = ProgressionConfidence.MEDIUM
                        reasons += ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE
                        reasons += ProgressionReason.CONFIRMATION_REQUIRED
                        reasons += ProgressionReason.NO_RIR_CONSERVATIVE
                    }
                }
            }
            analysis.allAtOrAboveMin -> {
                decision = ProgressionDecision.ADD_REPS
                confidence = when {
                    analysis.hasTechniqueLimited || analysis.hasFailedRep -> ProgressionConfidence.MEDIUM
                    analysis.rirAvailable -> ProgressionConfidence.HIGH
                    else -> ProgressionConfidence.MEDIUM
                }
                reasons += ProgressionReason.REP_RANGE_NOT_REACHED
                if (analysis.hasTechniqueLimited) reasons += ProgressionReason.TECHNIQUE_LIMITED
                if (analysis.hasFailedRep) reasons += ProgressionReason.FAILED_REP
                if (analysis.rirAvailable && analysis.rirMateriallyEasier) {
                    reasons += ProgressionReason.RIR_EASIER_THAN_TARGET
                }
                repTarget = RepTarget(
                    totalRepsGoal = analysis.totalReps + 1,
                    repMin = prescription.repMin,
                    repMax = prescription.repMax,
                )
                nextLoad = analysis.anchoredLoad
            }
            else -> {
                decision = ProgressionDecision.HOLD
                confidence = ProgressionConfidence.MEDIUM
                reasons += if (analysis.allBelowMin) {
                    ProgressionReason.ALL_SETS_BELOW_MIN
                } else {
                    ProgressionReason.ONE_SET_BELOW_MIN
                }
                if (!analysis.difficultOverall) reasons += ProgressionReason.BELOW_RANGE_BUT_EASY
                nextLoad = analysis.anchoredLoad
            }
        }

        if (programChanged) {
            reasons += ProgressionReason.NEW_EPOCH
            if (decision == ProgressionDecision.ADD_LOAD) {
                decision = ProgressionDecision.HOLD
                reasons += ProgressionReason.PROGRAM_CHANGED
                confidence = ProgressionConfidence.LOW
                nextLoad = analysis.anchoredLoad
            }
        }

        val debug = ProgressionDebug(
            topRange = analysis.allAtTop,
            rirValid = analysis.rirAvailable && analysis.rirWithinTarget,
            qualityValid = !analysis.hasPain && !analysis.hasTechniqueLimited && !analysis.hasFailedRep,
            historyComparable = comparableHistory.isNotEmpty(),
            incomplete = analysis.completedCount < prescription.workSetCount,
            mixedLoads = analysis.mixedLoads,
            longGap = longGap,
            currentLoad = analysis.anchoredLoad,
            increment = increment,
            nextLoad = nextLoad,
        )

        return ProgressionRecommendation(
            decision = decision,
            currentLoad = analysis.anchoredLoad,
            nextLoad = nextLoad,
            repTarget = repTarget,
            confidence = confidence,
            reasons = reasons.distinct(),
            explanation = ProgressionExplanations.build(
                exercise = exercise,
                prescription = prescription,
                exposure = currentExposure,
                decision = decision,
                reasons = reasons.distinct(),
                nextLoad = nextLoad,
                repTarget = repTarget,
            ),
            algorithmVersion = ProgressionAlgorithm.VERSION,
            debug = debug,
        )
    }

    // ---- analysis --------------------------------------------------------

    private fun analyze(
        exposure: CompletedExerciseExposure,
        prescription: RoutineExercisePrescription,
    ): ExposureAnalysis {
        val prescribed = exposure.workSets
        val completed = prescribed.filter { it.completed }
        val loads = completed.mapNotNull { it.load }
        val anchored = mostFrequentLoad(loads)
        val mixed = distinctLoads(loads).size > 1

        val topCount = completed.count { it.repsCompleted >= prescription.repMax }
        val atLeastMin = completed.count { it.repsCompleted >= prescription.repMin }
        val belowMinSets = completed.filter { it.repsCompleted < prescription.repMin }

        val rirAvailable = prescription.rirConfigured && completed.isNotEmpty() &&
            completed.all { it.rir != null }
        val rirValues = completed.mapNotNull { it.rir }
        val rirWithin = rirAvailable &&
            rirValues.all { it >= prescription.targetRirMin!! && it <= prescription.targetRirMax!! }
        val rirHarder = rirAvailable && rirValues.any { it < prescription.targetRirMin!! }
        val rirEasier = rirAvailable &&
            rirValues.all { it >= prescription.targetRirMax!! } &&
            rirValues.any { it > prescription.targetRirMax!! }

        return ExposureAnalysis(
            anchoredLoad = anchored,
            workSetCount = prescription.workSetCount,
            completedCount = completed.size,
            topRangeCount = topCount,
            atOrAboveMinCount = atLeastMin,
            belowMinCount = belowMinSets.size,
            allAtTop = completed.size == prescription.workSetCount &&
                topCount == prescription.workSetCount,
            allAtOrAboveMin = completed.size == prescription.workSetCount &&
                atLeastMin == prescription.workSetCount,
            allBelowMin = completed.isNotEmpty() && belowMinSets.size == completed.size,
            mixedLoads = mixed,
            hasPain = prescribed.any { it.quality.isPain },
            hasTechniqueLimited = completed.any { it.quality.isTechniqueLimited },
            hasFailedRep = completed.any { it.quality.isFailedRep },
            rirConfigured = prescription.rirConfigured,
            rirAvailable = rirAvailable,
            rirWithinTarget = rirWithin,
            rirMateriallyHarder = rirHarder,
            rirMateriallyEasier = rirEasier,
            totalReps = completed.sumOf { it.repsCompleted },
            qualities = completed.map { it.quality }.toSet(),
            difficultOverall = belowMinSets.all { isDifficult(it) },
        )
    }

    private fun underperforms(
        exposure: CompletedExerciseExposure,
        prescription: RoutineExercisePrescription,
    ): Boolean {
        val completed = exposure.workSets.filter { it.completed }
        if (completed.isEmpty()) return false
        val below = completed.filter { it.repsCompleted < prescription.repMin }
        if (below.size * 2 < completed.size) return false
        return below.all { isDifficult(it) }
    }

    private fun isDifficult(set: WorkoutSet): Boolean = set.rir == null || set.rir <= 2

    private fun previousAllAtTop(previous: CompletedExerciseExposure): Boolean {
        val sets = previous.workSets.filter { it.completed }
        return sets.isNotEmpty() && sets.all { it.repsCompleted >= previous.snapshot.repMax }
    }

    private fun increaseLoad(
        current: Double?,
        increment: Double?,
        direction: ProgressionDirection,
    ): Double? {
        if (current == null || increment == null) return null
        return WeightMath.increase(current, increment, direction)
    }

    private fun mostFrequentLoad(loads: List<Double>): Double? {
        if (loads.isEmpty()) return null
        return loads.groupingBy { it }.eachCount().entries
            .maxWithOrNull(
                compareBy<Map.Entry<Double, Int>> { it.value }
                    .thenByDescending { -loads.indexOf(it.key) },
            )
            ?.key
    }

    private fun distinctLoads(loads: List<Double>): List<Double> =
        loads.fold(mutableListOf()) { acc, value ->
            if (acc.none { ProgressionAlgorithm.loadsEqual(it, value) }) acc.add(value)
            acc
        }

    private fun comparableHistory(
        current: CompletedExerciseExposure,
        history: List<CompletedExerciseExposure>,
    ): List<CompletedExerciseExposure> = history
        .filter { it.exerciseId == current.exerciseId && it.id != current.id }
        .filter { comparable(current.snapshot, it.snapshot) }
        .sortedByDescending { it.performedAt }

    private fun comparable(a: ExposureSnapshot, b: ExposureSnapshot): Boolean =
        a.repMin == b.repMin &&
            a.repMax == b.repMax &&
            a.workSetCount == b.workSetCount &&
            a.loadSemantics == b.loadSemantics &&
            a.progressionDirection == b.progressionDirection
}
