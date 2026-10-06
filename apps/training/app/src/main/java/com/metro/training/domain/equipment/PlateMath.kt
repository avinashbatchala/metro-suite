package com.metro.training.domain.equipment

import kotlin.math.abs

/**
 * Weight-plate solver for a barbell. Available plates/bar are user-configurable (gym profile).
 * Returns the plates **per side** (descending), or null when the target is not exactly achievable
 * with the available plates.
 */
object PlateMath {
    fun platesPerSide(
        targetKg: Double,
        barKg: Double,
        availablePlatesKg: List<Double>,
    ): List<Double>? {
        val perSide = (targetKg - barKg) / 2.0
        if (perSide < -1e-6) return null
        var remaining = perSide
        val used = mutableListOf<Double>()
        availablePlatesKg.sortedDescending().forEach { plate ->
            if (plate <= 0.0) return@forEach
            while (remaining + 1e-6 >= plate) {
                used += plate
                remaining -= plate
            }
        }
        return if (abs(remaining) <= 5e-4) used else null
    }

    fun describe(plates: List<Double>): String =
        if (plates.isEmpty()) "bar only" else plates.joinToString(" + ") { format(it) }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
