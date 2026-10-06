package com.metro.training.domain.progression

import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.workout.CompletedExerciseExposure

/**
 * Builds deterministic, UI-renderable explanation prose from structured reasons. Never says an AI
 * decided anything — this is a coaching rule with visible inputs.
 */
object ProgressionExplanations {

    fun build(
        exercise: ExerciseDefinition,
        prescription: RoutineExercisePrescription,
        exposure: CompletedExerciseExposure,
        decision: ProgressionDecision,
        reasons: List<ProgressionReason>,
        nextLoad: Double?,
        repTarget: RepTarget?,
    ): ProgressionExplanation {
        val headline = headline(exercise, decision)
        val details = mutableListOf<String>()

        val performed = exposure.workSets.filter { it.completed }
        if (performed.isNotEmpty()) {
            val lines = performed.joinToString(" · ") { set ->
                val load = set.load?.let { "${format(it)} kg × " } ?: ""
                val rir = set.rir?.let { " (${it} RIR)" } ?: ""
                "$load${set.repsCompleted} reps$rir"
            }
            details += "Last time: $lines"
        }

        reasons.forEach { reason ->
            prose(reason, prescription, decision, nextLoad)?.let { details += it }
        }

        if (repTarget != null) {
            details += "Goal next time: beat ${repTarget.totalRepsGoal - 1} total reps " +
                "within ${repTarget.repMin}–${repTarget.repMax} per set, same load."
        }

        return ProgressionExplanation(headline = headline, details = details.distinct())
    }

    fun headline(exercise: ExerciseDefinition, decision: ProgressionDecision): String = when (decision) {
        ProgressionDecision.ADD_LOAD -> if (exercise.progressionDirection == ProgressionDirection.LESS_LOAD_IS_HARDER) {
            "reduce assistance"
        } else {
            "increase load"
        }
        ProgressionDecision.ADD_REPS -> "add reps"
        ProgressionDecision.REDUCE_LOAD -> if (exercise.progressionDirection == ProgressionDirection.LESS_LOAD_IS_HARDER) {
            "add assistance"
        } else {
            "reduce load"
        }
        ProgressionDecision.HOLD -> "hold"
        ProgressionDecision.REVIEW -> "review"
    }

    private fun prose(
        reason: ProgressionReason,
        prescription: RoutineExercisePrescription,
        decision: ProgressionDecision,
        nextLoad: Double?,
    ): String? = when (reason) {
        ProgressionReason.ALL_SETS_AT_TOP_OF_RANGE ->
            "Every work set reached the top of your ${prescription.repMin}–${prescription.repMax} rep range."
        ProgressionReason.RIR_WITHIN_TARGET ->
            "Effort stayed within your target RIR ${prescription.targetRirMin}–${prescription.targetRirMax}."
        ProgressionReason.RIR_EASIER_THAN_TARGET ->
            "Effort was easier than your target — progression is still earned."
        ProgressionReason.RIR_HARDER_THAN_TARGET ->
            "At least one set was harder than your target effort, so load is held."
        ProgressionReason.RIR_MIXED ->
            "RIR data was inconsistent."
        ProgressionReason.ONE_SET_BELOW_MIN ->
            "One work set fell below the minimum rep — repeating the load to collect more evidence."
        ProgressionReason.SOME_SETS_BELOW_MIN ->
            "Some work sets fell below the minimum rep."
        ProgressionReason.ALL_SETS_BELOW_MIN ->
            "All work sets fell below the minimum rep."
        ProgressionReason.BELOW_RANGE_BUT_EASY ->
            "The miss looked easy, so this is not treated as too-heavy."
        ProgressionReason.UNDERPERFORMANCE ->
            "This session was below target. One poor session does not change load."
        ProgressionReason.SECOND_CONSECUTIVE_UNDERPERFORMANCE ->
            "This is the second comparable session below target, so load is reduced by one increment."
        ProgressionReason.TECHNIQUE_LIMITED ->
            "A set was flagged technique limited, so load is not increased from it."
        ProgressionReason.FAILED_REP ->
            "A rep was missed; the failed attempt was not counted."
        ProgressionReason.PAIN_REPORTED ->
            "Auto Progress is paused because this exercise was marked painful. Review before progressing."
        ProgressionReason.LONG_TRAINING_GAP ->
            "It has been a while since your last comparable session; confidence is low."
        ProgressionReason.MIXED_LOADS ->
            "Work sets used different loads, so no single progression anchor exists."
        ProgressionReason.INCOMPLETE_WORK_SETS ->
            "Not all prescribed work sets were completed."
        ProgressionReason.NO_HISTORY ->
            "There is no comparable history yet."
        ProgressionReason.CONFIRMATION_REQUIRED ->
            "No RIR was logged. Reaching the top of the range once more confirms progression."
        ProgressionReason.REP_RANGE_NOT_REACHED ->
            "You are above the minimum but not yet at the top of the range — add reps at the same load."
        ProgressionReason.REPS_ONLY_CEILING ->
            "You reached the top of the rep range. Consider a harder variation, added weight, or a new range."
        ProgressionReason.NO_RIR_CONSERVATIVE ->
            "Without RIR the app progresses more conservatively."
        ProgressionReason.PROGRAM_CHANGED ->
            "The prescription changed, so a new progression block started."
        ProgressionReason.NEW_EPOCH ->
            "This is the first exposure with the current programming."
        ProgressionReason.EQUIPMENT_INCREMENT ->
            nextLoad?.let { "Next available load: ${format(it)} kg." } ?: "No valid increment configured."
        ProgressionReason.LOAD_MAY_BE_TOO_HEAVY ->
            "The load may be too heavy for the target range."
    }

    fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    @Suppress("unused")
    private fun decisionWord(decision: ProgressionDecision): String = decision.name.lowercase()
}
