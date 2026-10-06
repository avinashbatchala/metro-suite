package com.metro.training.domain.progression

import com.metro.training.domain.workout.SetQuality

enum class ProgressionDecision {
    ADD_REPS,
    ADD_LOAD,
    HOLD,
    REDUCE_LOAD,
    REVIEW,
}

enum class ProgressionConfidence { HIGH, MEDIUM, LOW }

/** Structured, UI-renderable reasons a recommendation was produced. */
enum class ProgressionReason {
    ALL_SETS_AT_TOP_OF_RANGE,
    RIR_WITHIN_TARGET,
    RIR_HARDER_THAN_TARGET,
    RIR_EASIER_THAN_TARGET,
    RIR_MIXED,
    ONE_SET_BELOW_MIN,
    SOME_SETS_BELOW_MIN,
    ALL_SETS_BELOW_MIN,
    BELOW_RANGE_BUT_EASY,
    SECOND_CONSECUTIVE_UNDERPERFORMANCE,
    UNDERPERFORMANCE,
    TECHNIQUE_LIMITED,
    FAILED_REP,
    PAIN_REPORTED,
    LONG_TRAINING_GAP,
    MIXED_LOADS,
    INCOMPLETE_WORK_SETS,
    NO_HISTORY,
    CONFIRMATION_REQUIRED,
    REP_RANGE_NOT_REACHED,
    REPS_ONLY_CEILING,
    NO_RIR_CONSERVATIVE,
    PROGRAM_CHANGED,
    NEW_EPOCH,
    EQUIPMENT_INCREMENT,
    LOAD_MAY_BE_TOO_HEAVY,
}

/** Suggested rep goal for the next exposure. */
data class RepTarget(
    val totalRepsGoal: Int,
    val repMin: Int,
    val repMax: Int,
)

/** Structured explanation lines (UI renders prose; never "the AI determined…"). */
data class ProgressionExplanation(
    val headline: String,
    val details: List<String>,
)

data class ProgressionDebug(
    val topRange: Boolean,
    val rirValid: Boolean,
    val qualityValid: Boolean,
    val historyComparable: Boolean,
    val incomplete: Boolean,
    val mixedLoads: Boolean,
    val longGap: Boolean,
    val currentLoad: Double?,
    val increment: Double?,
    val nextLoad: Double?,
) {
    fun asLines(): List<String> = listOf(
        "topRange=$topRange",
        "rirValid=$rirValid",
        "qualityValid=$qualityValid",
        "historyComparable=$historyComparable",
        "incomplete=$incomplete",
        "mixedLoads=$mixedLoads",
        "longGap=$longGap",
        "currentLoad=$currentLoad",
        "increment=$increment",
        "nextLoad=$nextLoad",
    )
}

data class ProgressionRecommendation(
    val decision: ProgressionDecision,
    val currentLoad: Double?,
    val nextLoad: Double?,
    val repTarget: RepTarget?,
    val confidence: ProgressionConfidence,
    val reasons: List<ProgressionReason>,
    val explanation: ProgressionExplanation,
    val algorithmVersion: Int,
    val debug: ProgressionDebug? = null,
)

/** Internal summary of the current exposure's work-set performance. */
internal data class ExposureAnalysis(
    val anchoredLoad: Double?,
    val workSetCount: Int,
    val completedCount: Int,
    val topRangeCount: Int,
    val atOrAboveMinCount: Int,
    val belowMinCount: Int,
    val allAtTop: Boolean,
    val allAtOrAboveMin: Boolean,
    val allBelowMin: Boolean,
    val mixedLoads: Boolean,
    val hasPain: Boolean,
    val hasTechniqueLimited: Boolean,
    val hasFailedRep: Boolean,
    val rirConfigured: Boolean,
    val rirAvailable: Boolean,
    val rirWithinTarget: Boolean,
    val rirMateriallyHarder: Boolean,
    val rirMateriallyEasier: Boolean,
    val totalReps: Int,
    val qualities: Set<SetQuality>,
    val difficultOverall: Boolean,
)
