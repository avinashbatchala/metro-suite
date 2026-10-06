package com.metro.training.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metro.training.data.TrainingPreferences
import com.metro.training.data.repositories.TrainingRepository
import com.metro.training.domain.analytics.ExerciseAnalytics
import com.metro.training.domain.analytics.OneRepMax
import com.metro.training.domain.analytics.ProgressMetric
import com.metro.training.domain.workout.CompletedExerciseExposure
import com.metro.training.domain.exercises.ExerciseDefaults
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.plan.DayFocus
import com.metro.training.domain.plan.PlanRequest
import com.metro.training.domain.plan.RoutineTemplateGenerator
import com.metro.training.domain.plan.SplitPreset
import com.metro.training.domain.plan.TrainingGoal
import com.metro.training.domain.plan.dayFocuses
import com.metro.training.domain.progression.ProgressionDecision
import com.metro.training.domain.progression.ProgressionRecommendation
import com.metro.training.domain.routines.ProgressionPolicy
import com.metro.training.domain.routines.Routine
import com.metro.training.domain.routines.RoutineExercise
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.training.domain.volume.MuscleVolumeCalculator
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.training.domain.workout.WorkoutSummary
import com.metro.training.notify.WorkoutNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class TrainingRoute {
    Main,
    RoutineDetail,
    ExercisePicker,
    CustomExercise,
    ActiveWorkout,
    WorkoutSummary,
    ExerciseDetail,
    Settings,
}

enum class TrainingPivot { Today, Routines, History, Progress }

enum class PickerCategory { All, Custom }

data class RecommendationView(
    val exerciseName: String,
    val exerciseId: String,
    val workoutExerciseId: String,
    val decision: ProgressionDecision,
    val currentLoad: Double?,
    val nextLoad: Double?,
    val repTargetText: String?,
    val explanation: List<String>,
    val headline: String,
)

data class ExerciseProgressData(
    val exercise: ExerciseDefinition,
    val exposures: List<com.metro.training.domain.workout.CompletedExerciseExposure>,
)

data class PrEvent(val exerciseName: String, val kind: String)

data class OnboardingUiState(
    val step: Int = 0,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val days: Int = 3,
    val preset: SplitPreset = SplitPreset.suggest(3),
    val rirEnabled: Boolean = true,
    val unit: com.metro.training.domain.exercises.LoadUnit = com.metro.training.domain.exercises.LoadUnit.KG,
    val focus: List<DayFocus> = SplitPreset.suggest(3).dayFocuses(3),
)

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = TrainingRepository(application.applicationContext)
    val prefs = TrainingPreferences(application.applicationContext)

    /** Best estimated 1RM per exercise id, loaded once per workout for live PR detection. */
    private val prCache = HashMap<String, Double>()

    /** Sets the user edited by hand — the "copy set 1" feature never overwrites these. */
    private val manuallyEditedSets = HashSet<String>()

    var route by mutableStateOf(TrainingRoute.Main)
        private set
    var pivot by mutableStateOf(TrainingPivot.Today)
        private set

    var exercises by mutableStateOf<List<ExerciseDefinition>>(emptyList())
        private set
    var routines by mutableStateOf<List<Routine>>(emptyList())
        private set
    var activeWorkout by mutableStateOf<Workout?>(null)
        private set
    var historySummaries by mutableStateOf<List<WorkoutSummary>>(emptyList())
        private set

    var selectedRoutine by mutableStateOf<Routine?>(null)
        private set
    var isNewRoutine by mutableStateOf(false)
        private set

    var pickerQuery by mutableStateOf("")
        private set
    var pickerCategory by mutableStateOf(PickerCategory.All)
        private set
    var pickerForWorkout by mutableStateOf(false)
        private set
    var pickerMuscle by mutableStateOf<MuscleGroup?>(null)
        private set
    var pickerEquipment by mutableStateOf<com.metro.training.domain.exercises.EquipmentType?>(null)
        private set

    var customDraft by mutableStateOf<CustomExerciseDraft?>(null)
        private set

    var summaryWorkout by mutableStateOf<Workout?>(null)
        private set
    var summaryRecommendations by mutableStateOf<List<RecommendationView>>(emptyList())
        private set
    var selectedRecommendation by mutableStateOf<RecommendationView?>(null)
        private set

    var weeklyMuscleSets by mutableStateOf<List<Pair<MuscleGroup, Double>>>(emptyList())
        private set
    var lastWeekMuscleSets by mutableStateOf<Map<MuscleGroup, Double>>(emptyMap())
        private set
    var weeklyVolumePoints by mutableStateOf<List<com.metro.training.domain.analytics.ExercisePoint>>(emptyList())
        private set
    var selectedExerciseProgress by mutableStateOf<ExerciseProgressData?>(null)
        private set
    var prEvent by mutableStateOf<PrEvent?>(null)
        private set

    /** Previous comparable work sets per live workout exercise: (load, reps, rir). */
    var previousByExercise by mutableStateOf<Map<String, List<Triple<Double?, Int, Int?>>>>(emptyMap())
        private set
    var workoutNoteDraft by mutableStateOf<String?>(null)
        private set

    var status by mutableStateOf<String?>(null)
        private set

    var showOnboarding by mutableStateOf(!prefs.onboardingCompleted)
        private set
    var onboarding by mutableStateOf(OnboardingUiState())
        private set

    var weightUnit by mutableStateOf(prefs.weightUnit)
        private set
    var rirEnabled by mutableStateOf(prefs.rirEnabled)
        private set
    var smartHints by mutableStateOf(prefs.smartHints)
        private set
    var defaultRestSeconds by mutableStateOf(prefs.defaultRestSeconds)
        private set

    val filteredExercises: List<ExerciseDefinition>
        get() {
            val query = pickerQuery.trim().lowercase()
            return exercises
                .filter { pickerCategory == PickerCategory.All || !it.builtIn }
                .filter { pickerMuscle == null || pickerMuscle!! in it.primaryMuscles }
                .filter { pickerEquipment == null || it.equipment == pickerEquipment }
                .filter { query.isEmpty() || it.name.lowercase().contains(query) }
        }

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            repo.observeExercises().collectLatest { exercises = it }
        }
        viewModelScope.launch { repo.observeRoutines().collectLatest { routines = it } }
        // Recovery only: the in-memory active workout is authoritative while editing, so we do not
        // observe the workouts table (that would reload on every restart-timer write and clobber edits).
        viewModelScope.launch {
            val recovered = withContext(Dispatchers.IO) { repo.activeWorkout() }
                ?.let { it.copy(restDeadlineMillis = prefs.restDeadline(it.id)) }
            activeWorkout = recovered
            recovered?.let { primeForWorkout(it) }
            recovered?.let {
                val name = it.routineName.ifBlank { "Workout" }
                it.restDeadlineMillis?.let { deadline -> WorkoutNotifier.showRest(getApplication(), name, deadline) }
                    ?: WorkoutNotifier.showWorkout(getApplication(), name, it.startedAt)
            }
        }
        viewModelScope.launch { refreshHistory() }
        viewModelScope.launch { refreshProgress() }
    }

    suspend fun refreshHistory() {
        historySummaries = withContext(Dispatchers.IO) { repo.completedWorkoutSummaries() }
    }

    // ---- navigation ------------------------------------------------------

    fun setPivot(index: Int) {
        pivot = when (index) {
            1 -> TrainingPivot.Routines
            2 -> TrainingPivot.History
            3 -> TrainingPivot.Progress
            else -> TrainingPivot.Today
        }
        if (pivot == TrainingPivot.Progress) viewModelScope.launch { refreshProgress() }
    }

    fun closeSubpage() {
        route = TrainingRoute.Main
        selectedRoutine = null
        isNewRoutine = false
        selectedExerciseProgress = null
        selectedRecommendation = null
    }

    fun dismissStatus() { status = null }

    fun showStatus(message: String) { status = message }

    // ---- settings --------------------------------------------------------

    fun chooseWeightUnit(unit: com.metro.training.domain.exercises.LoadUnit) {
        weightUnit = unit
        prefs.weightUnit = unit
    }

    fun toggleRir(enabled: Boolean) {
        rirEnabled = enabled
        prefs.rirEnabled = enabled
    }

    fun toggleSmartHints(enabled: Boolean) {
        smartHints = enabled
        prefs.smartHints = enabled
    }

    fun setDefaultRest(seconds: Int) {
        defaultRestSeconds = seconds
        prefs.defaultRestSeconds = seconds
    }

    fun openSettings() { route = TrainingRoute.Settings }

    // ---- backup / restore ------------------------------------------------

    suspend fun exportJsonBackup(): String = withContext(Dispatchers.IO) {
        com.metro.training.data.backup.TrainingBackup.exportJson(repo.allCompletedWorkouts())
    }

    suspend fun exportCsvBackup(): String = withContext(Dispatchers.IO) {
        com.metro.training.data.backup.TrainingBackup.exportCsv(repo.allCompletedWorkouts())
    }

    /** Import a JSON backup or a Hevy-style CSV; returns the number of workouts imported. */
    suspend fun importBackup(text: String): Int = withContext(Dispatchers.IO) {
        val trimmed = text.trimStart()
        val workouts = if (trimmed.startsWith("[")) {
            com.metro.training.data.backup.TrainingBackup.parseJson(text)
        } else {
            com.metro.training.data.backup.HevyCsvImport.parse(text, exercises)
        }
        if (workouts.isNotEmpty()) repo.importWorkouts(workouts)
        refreshHistory()
        workouts.size
    }

    // ---- onboarding / plan builder --------------------------------------

    fun startOnboarding() {
        val days = prefs.trainingDays.coerceIn(1, 6)
        val preset = SplitPreset.suggest(days)
        onboarding = OnboardingUiState(
            step = 0,
            days = days,
            preset = preset,
            rirEnabled = prefs.rirEnabled,
            unit = prefs.weightUnit,
            focus = preset.dayFocuses(days),
        )
        showOnboarding = true
    }

    fun onboardingNext() {
        onboarding = onboarding.copy(step = (onboarding.step + 1).coerceAtMost(ONBOARDING_STEPS - 1))
    }

    fun onboardingBack() {
        onboarding = onboarding.copy(step = (onboarding.step - 1).coerceAtLeast(0))
    }

    fun onboardingSetGoal(goal: TrainingGoal) {
        onboarding = onboarding.copy(goal = goal)
    }

    fun onboardingSetRir(enabled: Boolean) {
        onboarding = onboarding.copy(rirEnabled = enabled)
    }

    fun onboardingSetUnit(unit: com.metro.training.domain.exercises.LoadUnit) {
        onboarding = onboarding.copy(unit = unit)
    }

    fun onboardingSetDays(days: Int) {
        val clamped = days.coerceIn(1, 6)
        val preset = SplitPreset.suggest(clamped)
        onboarding = onboarding.copy(days = clamped, preset = preset, focus = preset.dayFocuses(clamped))
    }

    fun onboardingSetPreset(preset: SplitPreset) {
        onboarding = onboarding.copy(preset = preset, focus = preset.dayFocuses(onboarding.days))
    }

    fun onboardingToggleMuscle(dayIndex: Int, muscle: MuscleGroup) {
        val focus = onboarding.focus.toMutableList()
        val day = focus.getOrNull(dayIndex) ?: return
        val next = if (muscle in day.muscles) day.muscles - muscle else day.muscles + muscle
        focus[dayIndex] = day.copy(muscles = next)
        onboarding = onboarding.copy(focus = focus)
    }

    fun onboardingReplaceMuscles(dayIndex: Int, muscles: Set<MuscleGroup>) {
        val focus = onboarding.focus.toMutableList()
        val day = focus.getOrNull(dayIndex) ?: return
        focus[dayIndex] = day.copy(muscles = muscles)
        onboarding = onboarding.copy(focus = focus)
    }

    fun buildPlanFromOnboarding() {
        val state = onboarding
        viewModelScope.launch {
            val builtIns = withContext(Dispatchers.IO) {
                val all = repo.exercises()
                if (all.isEmpty()) {
                    repo.ensureSeeded()
                    repo.exercises()
                } else {
                    all
                }
            }.filter { it.builtIn }

            val request = PlanRequest(
                days = state.days,
                goal = state.goal,
                focusPerDay = state.focus.filter { it.muscles.isNotEmpty() },
                rirEnabled = state.rirEnabled,
            )
            val routines = RoutineTemplateGenerator.generate(request, builtIns)
            routines.forEach { repo.saveRoutine(it) }

            prefs.trainingDays = state.days
            prefs.rirEnabled = state.rirEnabled
            prefs.weightUnit = state.unit
            prefs.onboardingCompleted = true
            rirEnabled = state.rirEnabled
            weightUnit = state.unit
            showOnboarding = false
            route = TrainingRoute.Main
            pivot = TrainingPivot.Today
        }
    }

    fun skipOnboarding() {
        prefs.onboardingCompleted = true
        showOnboarding = false
    }

    // ---- exercises -------------------------------------------------------

    fun startCustomExercise() {
        customDraft = CustomExerciseDraft()
        route = TrainingRoute.CustomExercise
    }

    fun updateCustomDraft(transform: (CustomExerciseDraft) -> CustomExerciseDraft) {
        customDraft = customDraft?.let(transform)
    }

    fun saveCustomExercise() {
        val draft = customDraft ?: return
        val definition = draft.toDefinition() ?: run {
            status = draft.validationMessage()
            return
        }
        viewModelScope.launch {
            repo.saveExercise(definition)
            customDraft = null
            route = TrainingRoute.ExercisePicker
        }
    }

    fun archiveExercise(exercise: ExerciseDefinition) {
        viewModelScope.launch {
            repo.archiveExercise(exercise.id)
            if (selectedExerciseProgress?.exercise?.id == exercise.id) closeSubpage()
        }
    }

    fun openExercisePicker(forWorkout: Boolean = false) {
        pickerQuery = ""
        pickerCategory = PickerCategory.All
        pickerMuscle = null
        pickerEquipment = null
        pickerForWorkout = forWorkout
        route = TrainingRoute.ExercisePicker
    }

    fun choosePickerMuscle(muscle: MuscleGroup?) { pickerMuscle = muscle }
    fun choosePickerEquipment(equipment: com.metro.training.domain.exercises.EquipmentType?) { pickerEquipment = equipment }

    fun pickExercise(exercise: ExerciseDefinition) {
        if (pickerForWorkout) {
            addExerciseToWorkout(exercise)
        } else {
            addExerciseToSelectedRoutine(exercise)
        }
    }

    fun updatePickerQuery(query: String) { pickerQuery = query }

    fun updatePickerCategory(category: PickerCategory) { pickerCategory = category }

    // ---- routines --------------------------------------------------------

    fun newRoutine() {
        selectedRoutine = Routine(
            id = TrainingRepository.newId(),
            name = "",
            exercises = emptyList(),
            createdAt = System.currentTimeMillis(),
        )
        isNewRoutine = true
        route = TrainingRoute.RoutineDetail
    }

    fun openRoutine(id: String) {
        viewModelScope.launch {
            selectedRoutine = withContext(Dispatchers.IO) { repo.routine(id) }
            isNewRoutine = false
            route = TrainingRoute.RoutineDetail
        }
    }

    fun renameRoutine(name: String) {
        selectedRoutine = selectedRoutine?.copy(name = name)
    }

    fun addExerciseToSelectedRoutine(exercise: ExerciseDefinition) {
        val routine = selectedRoutine ?: return
        val prescription = prescriptionFor(exercise)
        val updated = routine.copy(
            exercises = routine.exercises + RoutineExercise(prescription, routine.exercises.size),
        )
        selectedRoutine = updated
        route = TrainingRoute.RoutineDetail
    }

    fun updateRoutineExercise(prescription: RoutineExercisePrescription) {
        val routine = selectedRoutine ?: return
        selectedRoutine = routine.copy(
            exercises = routine.exercises.map {
                if (it.prescription.id == prescription.id) it.copy(prescription = prescription) else it
            },
        )
    }

    fun removeRoutineExercise(recordId: String) {
        val routine = selectedRoutine ?: return
        selectedRoutine = routine.copy(
            exercises = routine.exercises
                .filterNot { it.prescription.id == recordId }
                .mapIndexed { index, exercise -> exercise.copy(order = index) },
        )
    }

    fun moveRoutineExercise(recordId: String, delta: Int) {        val routine = selectedRoutine ?: return
        val list = routine.exercises.toMutableList()
        val index = list.indexOfFirst { it.prescription.id == recordId }
        if (index < 0) return
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (target == index) return
        val item = list.removeAt(index)
        list.add(target, item)
        selectedRoutine = routine.copy(exercises = list.mapIndexed { i, e -> e.copy(order = i) })
    }

    fun setRoutineSupersetTag(recordId: String, tag: String?) {
        val routine = selectedRoutine ?: return
        selectedRoutine = routine.copy(
            exercises = routine.exercises.map {
                if (it.prescription.id == recordId) it.copy(supersetTag = tag) else it
            },
        )
    }

    fun duplicateRoutine(routine: Routine) {
        viewModelScope.launch {
            repo.duplicateRoutine(routine.id)
        }
    }

    fun saveRoutine() {
        val routine = selectedRoutine ?: return
        if (routine.name.isBlank()) { status = "name your routine"; return }
        viewModelScope.launch {
            repo.saveRoutine(routine)
            isNewRoutine = false
            route = TrainingRoute.Main
            selectedRoutine = null
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            repo.archiveRoutine(routine.id)
            if (selectedRoutine?.id == routine.id) closeSubpage()
        }
    }

    private fun prescriptionFor(exercise: ExerciseDefinition): RoutineExercisePrescription {
        val defaults = ExerciseDefaults.forExercise(exercise)
        return RoutineExercisePrescription(
            id = TrainingRepository.newId(),
            exerciseId = exercise.id,
            workSetCount = defaults.workSetCount,
            repMin = defaults.repMin,
            repMax = defaults.repMax,
            targetRirMin = if (rirEnabled) defaults.targetRirMin else null,
            targetRirMax = if (rirEnabled) defaults.targetRirMax else null,
            restSeconds = if (defaultRestSeconds > 0) defaultRestSeconds else defaults.restSeconds,
            autoProgressEnabled = exercise.autoProgressReady,
            progressionPolicy = ProgressionPolicy(),
        )
    }

    // ---- workout ---------------------------------------------------------

    fun startWorkout(routine: Routine) {
        if (activeWorkout != null) { route = TrainingRoute.ActiveWorkout; return }
        viewModelScope.launch {
            val id = repo.startWorkout(routine)
            prefs.clearRest()
            val workout = withContext(Dispatchers.IO) { repo.workout(id) }
            activeWorkout = workout
            workout?.let { primeForWorkout(it) }
            workout?.let { WorkoutNotifier.showWorkout(getApplication(), it.routineName.ifBlank { "Workout" }, it.startedAt) }
            route = TrainingRoute.ActiveWorkout
        }
    }

    fun resumeWorkout() {
        if (activeWorkout != null) route = TrainingRoute.ActiveWorkout
    }

    /**
     * Inline set edit. When the edited set is the first prescribed work set of its exercise, its
     * load/reps are copied live to the remaining prescribed work sets (unless the user already
     * edited those by hand).
     */
    fun updateSet(setId: String, transform: (WorkoutSet) -> WorkoutSet) {
        val workout = activeWorkout ?: return
        val oldById = workout.exercises.flatMap { it.sets }.associateBy { it.id }

        val updatedExercises = workout.exercises.map { exercise ->
            val firstWorkId = exercise.sets
                .filter { it.prescribed && it.setType == SetType.WORK }
                .minByOrNull { it.setIndex }?.id
            val isFirst = firstWorkId == setId

            var sourceLoad: Double? = null
            var sourceReps: Int? = null
            val edited = exercise.sets.map { set ->
                if (set.id == setId) {
                    transform(set).also { sourceLoad = it.load; sourceReps = it.repsCompleted }
                } else {
                    set
                }
            }
            val copied = if (isFirst) {
                edited.map { set ->
                    if (set.id != setId && set.prescribed && set.setType == SetType.WORK &&
                        set.id !in manuallyEditedSets
                    ) {
                        set.copy(
                            load = sourceLoad,
                            repsCompleted = sourceReps ?: set.repsCompleted,
                        )
                    } else {
                        set
                    }
                }
            } else {
                edited
            }
            exercise.copy(sets = copied)
        }

        manuallyEditedSets.add(setId)
        activeWorkout = workout.copy(exercises = updatedExercises)
        updatedExercises.flatMap { it.sets }.forEach { set ->
            if (oldById[set.id] != set) {
                viewModelScope.launch { repo.saveSet(set) }
            }
        }
    }

    fun completeSet(setId: String) {
        val workout = activeWorkout ?: return
        val exercise = workout.exercises.firstOrNull { ex -> ex.sets.any { it.id == setId } }
        val set = exercise?.sets?.firstOrNull { it.id == setId }
        updateSet(setId) { s ->
            if (s.repsCompleted <= 0) s else s.copy(completed = true)
        }
        exercise?.let { detectPr(it, setId) }
        // Warm-up, drop and myo-rep sets do not trigger a full rest.
        val skipRest = set == null || set.setType.suppressesRest || set.setType == SetType.WARMUP
        if (!skipRest) {
            val rest = exercise?.restSeconds ?: defaultRestSeconds.takeIf { it > 0 } ?: 0
            if (rest > 0) startRest(rest)
        }
    }

    fun consumePrEvent() { prEvent = null }

    private fun detectPr(exercise: WorkoutExercise, setId: String) {
        val set = exercise.sets.firstOrNull { it.id == setId } ?: return
        if (!set.countsTowardProgression) return
        val load = set.load ?: return
        val estimate = OneRepMax.epley(load, set.repsCompleted) ?: return
        val previous = prCache[exercise.exerciseId]
        if (previous == null || estimate > previous + 0.01) {
            prCache[exercise.exerciseId] = estimate
            prEvent = PrEvent(
                exerciseName = exercise.exerciseName,
                kind = "estimated 1RM ${formatWeight(estimate)}",
            )
        }
    }

    private suspend fun primeForWorkout(workout: Workout) {
        manuallyEditedSets.clear()
        prCache.clear()
        val previous = HashMap<String, List<Triple<Double?, Int, Int?>>>()
        workout.exercises.forEach { exercise ->
            val history = withContext(Dispatchers.IO) { repo.recentExposures(exercise.exerciseId, 200) }
            ExerciseAnalytics.personalBest(history, ProgressMetric.ESTIMATED_1RM)
                ?.let { prCache[exercise.exerciseId] = it }
            val workSets = withContext(Dispatchers.IO) {
                repo.previousWorkSets(
                    exerciseId = exercise.exerciseId,
                    repMin = exercise.snapshot.repMin,
                    repMax = exercise.snapshot.repMax,
                    workSetCount = exercise.snapshot.workSetCount,
                )
            }
            if (workSets.isNotEmpty()) previous[exercise.id] = workSets
        }
        previousByExercise = previous
        prefillFromPrevious(workout, previous)
    }

    /**
     * Prefill the prescribed work sets of a freshly started workout with last time's load/reps/rir
     * (editable). Sets the user already touched are left alone.
     */
    private suspend fun prefillFromPrevious(
        workout: Workout,
        previous: Map<String, List<Triple<Double?, Int, Int?>>>,
    ) {
        withContext(Dispatchers.IO) {
            workout.exercises.forEach { exercise ->
                val previousSets = previous[exercise.id] ?: return@forEach
                var index = 0
                exercise.sets
                    .filter { it.setType.feedsProgression && it.prescribed }
                    .forEach { set ->
                        val values = previousSets.getOrNull(index++)
                        if (values != null && !set.completed && !manuallyEditedSets.contains(set.id)) {
                            repo.saveSet(
                                set.copy(
                                    load = values.first ?: set.load,
                                    repsCompleted = values.second,
                                    rir = values.third ?: set.rir,
                                ),
                            )
                        }
                    }
            }
        }
        activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
    }

    // ---- Phase 1 logging actions ----------------------------------------

    fun startEmptyWorkout() {
        if (activeWorkout != null) { route = TrainingRoute.ActiveWorkout; return }
        viewModelScope.launch {
            val id = repo.startEmptyWorkout()
            prefs.clearRest()
            val workout = withContext(Dispatchers.IO) { repo.workout(id) }
            activeWorkout = workout
            workout?.let { primeForWorkout(it) }
            workout?.let { WorkoutNotifier.showWorkout(getApplication(), it.routineName.ifBlank { "Workout" }, it.startedAt) }
            route = TrainingRoute.ActiveWorkout
        }
    }

    fun changeSetType(setId: String, type: SetType) {
        updateSet(setId) { it.copy(setType = type) }
    }

    fun moveExercise(workoutExerciseId: String, delta: Int) {
        viewModelScope.launch {
            repo.moveWorkoutExercise(workoutExerciseId, delta)
            activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
        }
    }

    fun removeExercise(workoutExerciseId: String) {
        viewModelScope.launch {
            repo.removeWorkoutExercise(workoutExerciseId)
            activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
        }
    }

    fun replaceExercise(workoutExerciseId: String, newExerciseId: String) {
        viewModelScope.launch {
            repo.replaceWorkoutExercise(workoutExerciseId, newExerciseId)
            activeWorkout?.id?.let {
                val workout = withContext(Dispatchers.IO) { repo.workout(it) }
                activeWorkout = workout
                workout?.let { primeForWorkout(it) }
            }
        }
    }

    fun setSupersetTag(workoutExerciseId: String, tag: String?) {        viewModelScope.launch {
            repo.setSupersetTag(workoutExerciseId, tag)
            activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
        }
    }

    // ---- equipment calculators ------------------------------------------

    fun plateBreakdown(targetKg: Double): List<Double>? =
        com.metro.training.domain.equipment.PlateMath.platesPerSide(
            targetKg,
            prefs.barWeightKg,
            prefs.plateWeightsKg,
        )

    fun warmupSets(workingKg: Double): List<com.metro.training.domain.equipment.WarmupSet> =
        com.metro.training.domain.equipment.WarmupCalculator.suggest(
            workingKg,
            prefs.barWeightKg,
            prefs.equipmentIncrementKg,
        )

    fun addWarmupSets(workoutExerciseId: String) {
        val workout = activeWorkout ?: return
        val exercise = workout.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        val working = exercise.sets.firstOrNull { it.setType.feedsProgression }?.load ?: return
        val suggested = warmupSets(working)
        viewModelScope.launch {
            suggested.forEach { repo.addWarmupSet(workoutExerciseId, it.loadKg, it.reps) }
            activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
        }
    }

    fun setExerciseNote(workoutExerciseId: String, note: String) {
        viewModelScope.launch {
            repo.updateWorkoutExerciseNote(workoutExerciseId, note)
            activeWorkout?.id?.let { activeWorkout = withContext(Dispatchers.IO) { repo.workout(it) } }
        }
    }

    fun openWorkoutNote() { workoutNoteDraft = activeWorkout?.note.orEmpty() }
    fun updateWorkoutNoteDraft(text: String) { workoutNoteDraft = text }
    fun dismissWorkoutNote() { workoutNoteDraft = null }
    fun saveWorkoutNote() {
        val workout = activeWorkout ?: return
        val note = workoutNoteDraft.orEmpty()
        workoutNoteDraft = null
        activeWorkout = workout.copy(note = note)
        viewModelScope.launch { repo.updateWorkoutNote(workout.id, note) }
    }

    /** Smart swap candidates: same primary muscle, similar equipment, not already in the workout. */
    fun swapCandidates(exerciseId: String): List<ExerciseDefinition> {
        val source = exercises.firstOrNull { it.id == exerciseId } ?: return emptyList()
        val inWorkout = activeWorkout?.exercises?.map { it.exerciseId }?.toSet().orEmpty()
        return exercises
            .filter { it.id != exerciseId && it.id !in inWorkout && !it.archived }
            .filter { it.primaryMuscles.any { muscle -> muscle in source.primaryMuscles } }
            .sortedWith(
                compareByDescending<ExerciseDefinition> { it.equipment == source.equipment }
                    .thenByDescending { it.mechanic == source.mechanic }
                    .thenBy { it.name },
            )
            .take(8)
    }

    /** Live header stats. */
    fun workoutVolumeKg(): Double =
        activeWorkout?.exercises?.flatMap { it.sets }
            ?.filter { it.completed && it.load != null }
            ?.sumOf { (it.load ?: 0.0) * it.repsCompleted } ?: 0.0

    fun completedSetCount(): Int =
        activeWorkout?.exercises?.flatMap { it.sets }?.count { it.completed } ?: 0

    fun addSet(workoutExerciseId: String, setType: SetType) {
        viewModelScope.launch {
            repo.addExtraSet(workoutExerciseId, setType)
            activeWorkout?.id?.let { id ->
                activeWorkout = withContext(Dispatchers.IO) { repo.workout(id) }
            }
        }
    }

    fun removeSet(setId: String) {
        viewModelScope.launch {
            repo.deleteSet(setId)
            activeWorkout?.id?.let { id ->
                activeWorkout = withContext(Dispatchers.IO) { repo.workout(id) }
            }
        }
    }

    fun addExerciseToWorkout(exercise: ExerciseDefinition) {
        val workout = activeWorkout ?: return
        viewModelScope.launch {
            repo.addExerciseToWorkout(workout.id, exercise.id, prescriptionFor(exercise))
            activeWorkout = withContext(Dispatchers.IO) { repo.workout(workout.id) }
            route = TrainingRoute.ActiveWorkout
        }
    }

    fun startRest(seconds: Int) {
        val workout = activeWorkout ?: return
        val deadline = System.currentTimeMillis() + seconds * 1000L
        // Persisted in prefs, not the workouts table — no Room invalidation, no reload, no clobber.
        prefs.setRest(workout.id, deadline)
        activeWorkout = workout.copy(restDeadlineMillis = deadline)
        WorkoutNotifier.showRest(getApplication(), workout.routineName.ifBlank { "Workout" }, deadline)
    }

    fun clearRest() {
        val workout = activeWorkout ?: return
        prefs.clearRest()
        activeWorkout = workout.copy(restDeadlineMillis = null)
        WorkoutNotifier.showWorkout(getApplication(), workout.routineName.ifBlank { "Workout" }, workout.startedAt)
    }

    fun finishWorkout() {
        val workout = activeWorkout ?: return
        viewModelScope.launch {
            repo.finishWorkout(workout.id)
            prefs.clearRest()
            WorkoutNotifier.stop(getApplication())
            val completed = withContext(Dispatchers.IO) { repo.workout(workout.id) }
            val recs = withContext(Dispatchers.IO) { repo.recommendationsForWorkout(workout.id) }
            summaryWorkout = completed
            summaryRecommendations = buildRecommendationViews(completed, recs)
            activeWorkout = null
            route = TrainingRoute.WorkoutSummary
            refreshHistory()
        }
    }

    fun discardWorkout() {
        val workout = activeWorkout ?: return
        viewModelScope.launch {
            repo.discardWorkout(workout.id)
            prefs.clearRest()
            WorkoutNotifier.stop(getApplication())
            activeWorkout = null
            route = TrainingRoute.Main
        }
    }

    fun openRecommendation(view: RecommendationView) { selectedRecommendation = view }

    fun closeRecommendation() { selectedRecommendation = null }

    fun acceptRecommendation(view: RecommendationView, load: Double?, repGoal: Int?) {
        val workoutId = summaryWorkout?.id ?: return
        viewModelScope.launch {
            repo.acceptRecommendation(workoutId, view.workoutExerciseId, load, repGoal)
        }
    }

    private suspend fun buildRecommendationViews(
        workout: Workout?,
        recommendations: List<ProgressionRecommendation>,
    ): List<RecommendationView> {
        val workoutValue = workout ?: return emptyList()
        val autoExercises = workoutValue.exercises.filter { it.autoProgressEnabled }
        return autoExercises.mapIndexedNotNull { index, exercise ->
            val rec = recommendations.getOrNull(index) ?: return@mapIndexedNotNull null
            RecommendationView(
                exerciseName = exercise.exerciseName,
                exerciseId = exercise.exerciseId,
                workoutExerciseId = exercise.id,
                decision = rec.decision,
                currentLoad = rec.currentLoad,
                nextLoad = rec.nextLoad,
                repTargetText = rec.repTarget?.let { "beat ${it.totalRepsGoal - 1} total reps" },
                explanation = rec.explanation.details,
                headline = rec.explanation.headline,
            )
        }
    }

    // ---- history / progress ---------------------------------------------

    fun openHistoryWorkout(workoutId: String) {
        viewModelScope.launch {
            val workout = withContext(Dispatchers.IO) { repo.workout(workoutId) } ?: return@launch
            summaryWorkout = workout
            val recs = withContext(Dispatchers.IO) { repo.recommendationsForWorkout(workoutId) }
            summaryRecommendations = buildRecommendationViews(workout, recs)
            route = TrainingRoute.WorkoutSummary
        }
    }

    fun openExerciseProgress(exercise: ExerciseDefinition) {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { repo.recentExposures(exercise.id, 400) }
            selectedExerciseProgress = ExerciseProgressData(exercise = exercise, exposures = history)
            route = TrainingRoute.ExerciseDetail
        }
    }

    fun exerciseFor(id: String): ExerciseDefinition? = exercises.firstOrNull { it.id == id }

    private suspend fun refreshProgress() {
        val now = System.currentTimeMillis()
        val weekStart = startOfWeek(now)
        val lastWeekStart = weekStart - 7L * 24 * 60 * 60 * 1000
        val definitions = withContext(Dispatchers.IO) { repo.exercisesById() }
        val thisWeek = withContext(Dispatchers.IO) { repo.completedExposuresSince(weekStart) }
        val lastWeek = withContext(Dispatchers.IO) {
            repo.completedExposuresSince(lastWeekStart).filter { it.performedAt < weekStart }
        }
        weeklyMuscleSets = MuscleVolumeCalculator.estimate(thisWeek, definitions)
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }
        lastWeekMuscleSets = MuscleVolumeCalculator.estimate(lastWeek, definitions)

        val twelveWeeks = withContext(Dispatchers.IO) {
            repo.completedExposuresSince(weekStart - 11L * 7 * 24 * 60 * 60 * 1000)
        }
        weeklyVolumePoints = buildWeeklyVolume(twelveWeeks, weekStart)
    }

    private fun buildWeeklyVolume(
        exposures: List<CompletedExerciseExposure>,
        weekStart: Long,
    ): List<com.metro.training.domain.analytics.ExercisePoint> {
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val buckets = LinkedHashMap<Long, Double>()
        for (index in 0..11) buckets[weekStart - index * weekMs] = 0.0
        exposures.forEach { exposure ->
            val bucket = startOfWeek(exposure.performedAt)
            if (!buckets.containsKey(bucket)) return@forEach
            val volume = exposure.sets
                .filter { it.setType == SetType.WORK && it.completed }
                .sumOf { (it.load ?: 0.0) * it.repsCompleted }
            buckets[bucket] = (buckets[bucket] ?: 0.0) + volume
        }
        return buckets.entries.sortedBy { it.key }
            .map { com.metro.training.domain.analytics.ExercisePoint(it.key, it.value) }
    }

    private fun startOfWeek(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatWeight(kg: Double): String {
        val value = when (weightUnit) {
            com.metro.training.domain.exercises.LoadUnit.KG -> kg
            com.metro.training.domain.exercises.LoadUnit.LB -> com.metro.training.domain.progression.WeightMath.kgToLb(kg)
        }
        val rounded = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        val suffix = if (weightUnit == com.metro.training.domain.exercises.LoadUnit.KG) "kg" else "lb"
        return "$rounded $suffix"
    }

    fun weightNumber(kg: Double?): String {
        if (kg == null) return ""
        val value = when (weightUnit) {
            com.metro.training.domain.exercises.LoadUnit.KG -> kg
            com.metro.training.domain.exercises.LoadUnit.LB -> com.metro.training.domain.progression.WeightMath.kgToLb(kg)
        }
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    fun parseWeight(text: String): Double? {
        val value = text.trim().toDoubleOrNull() ?: return null
        return when (weightUnit) {
            com.metro.training.domain.exercises.LoadUnit.KG -> value
            com.metro.training.domain.exercises.LoadUnit.LB ->
                com.metro.training.domain.progression.WeightMath.lbToKg(value)
        }
    }

    val weightSuffix: String
        get() = if (weightUnit == com.metro.training.domain.exercises.LoadUnit.KG) "kg" else "lb"

    val barWeightKg: Double get() = prefs.barWeightKg
    val plateWeights: List<Double> get() = prefs.plateWeightsKg
    fun setBarWeight(kg: Double) { if (kg > 0) prefs.barWeightKg = kg }
    fun setPlateWeights(text: String) {
        val list = text.split(',').mapNotNull { it.trim().toDoubleOrNull() }.filter { it > 0.0 }.sortedDescending()
        if (list.isNotEmpty()) prefs.plateWeightsKg = list
    }

    companion object {
        const val ONBOARDING_STEPS = 4
    }
}
