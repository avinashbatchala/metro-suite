package com.metro.training.data

import android.content.Context
import com.metro.training.domain.exercises.LoadUnit

/** Local Training preferences. Theme/accent are owned by MetroOS; not duplicated here. */
class TrainingPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("metro_training_prefs", Context.MODE_PRIVATE)

    var weightUnit: LoadUnit
        get() = runCatching { LoadUnit.valueOf(prefs.getString(KEY_UNIT, LoadUnit.KG.name)!!) }
            .getOrDefault(LoadUnit.KG)
        set(value) = prefs.edit().putString(KEY_UNIT, value.name).apply()

    var rirEnabled: Boolean
        get() = prefs.getBoolean(KEY_RIR, true)
        set(value) = prefs.edit().putBoolean(KEY_RIR, value).apply()

    var smartHints: Boolean
        get() = prefs.getBoolean(KEY_HINTS, true)
        set(value) = prefs.edit().putBoolean(KEY_HINTS, value).apply()

    var defaultRestSeconds: Int
        get() = prefs.getInt(KEY_REST, 0)
        set(value) = prefs.edit().putInt(KEY_REST, value).apply()

    // ---- gym profile (equipment) ----------------------------------------

    var barWeightKg: Double
        get() = prefs.getFloat(KEY_BAR, 20f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_BAR, value.toFloat()).apply()

    /** Available plate denominations per side (kg). */
    var plateWeightsKg: List<Double>
        get() = (prefs.getString(KEY_PLATES, null) ?: DEFAULT_PLATES)
            .split(',')
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 }
            .ifEmpty { DEFAULT_PLATES.split(',').map { it.toDouble() } }
        set(value) = prefs.edit()
            .putString(KEY_PLATES, value.joinToString(",") { it.toString() })
            .apply()

    var equipmentIncrementKg: Double
        get() = prefs.getFloat(KEY_INC, 2.5f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_INC, value.toFloat()).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var trainingDays: Int
        get() = prefs.getInt(KEY_DAYS, 3)
        set(value) = prefs.edit().putInt(KEY_DAYS, value).apply()

    /**
     * Active rest timer. Stored here (not in the workouts table) so ticking/pausing rest never
     * invalidates the history or active-workout Room observers.
     */
    fun setRest(workoutId: String, deadlineMillis: Long) {
        prefs.edit()
            .putString(KEY_REST_WORKOUT, workoutId)
            .putLong(KEY_REST_DEADLINE, deadlineMillis)
            .apply()
    }

    fun clearRest() {
        prefs.edit().remove(KEY_REST_WORKOUT).remove(KEY_REST_DEADLINE).apply()
    }

    /** Deadline for [workoutId] if a rest is still running, else null. */
    fun restDeadline(workoutId: String): Long? {
        if (prefs.getString(KEY_REST_WORKOUT, null) != workoutId) return null
        val deadline = prefs.getLong(KEY_REST_DEADLINE, 0L)
        return deadline.takeIf { it > System.currentTimeMillis() }
    }

    companion object {
        private const val KEY_UNIT = "weight_unit"
        private const val KEY_RIR = "rir_enabled"
        private const val KEY_HINTS = "smart_hints"
        private const val KEY_REST = "default_rest_seconds"
        private const val KEY_BAR = "bar_weight_kg"
        private const val KEY_PLATES = "plate_weights_kg"
        private const val KEY_INC = "equipment_increment_kg"
        private const val DEFAULT_PLATES = "25,20,15,10,5,2.5,1.25"
        private const val KEY_ONBOARDED = "onboarding_completed"
        private const val KEY_DAYS = "training_days"
        private const val KEY_REST_WORKOUT = "rest_workout_id"
        private const val KEY_REST_DEADLINE = "rest_deadline"
    }
}
