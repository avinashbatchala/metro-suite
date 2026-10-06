package com.metro.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.metro.training.R
import com.metro.training.domain.analytics.ExerciseAnalytics
import com.metro.training.domain.analytics.ProgressMetric
import com.metro.training.domain.analytics.ProgressRange
import com.metro.training.domain.exercises.EquipmentType
import com.metro.training.domain.exercises.ExerciseDefinition
import com.metro.training.domain.exercises.ExerciseInstructions
import com.metro.training.domain.exercises.ExerciseMechanic
import com.metro.training.domain.exercises.LoadSemantics
import com.metro.training.domain.exercises.LoadUnit
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.training.domain.exercises.ProgressionDirection
import com.metro.training.domain.exercises.ResistanceModel
import com.metro.training.domain.routines.RoutineExercise
import com.metro.training.domain.routines.RoutineExercisePrescription
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import com.metro.training.ui.components.MetroLineChart
import com.metro.training.ui.components.MetroSegmentedRow
import kotlinx.coroutines.delay

// ---- routine detail ------------------------------------------------------

@Composable
fun RoutineDetailScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val routine = viewModel.selectedRoutine ?: return
    var editing by remember { mutableStateOf<RoutineExercise?>(null) }
    var menuTarget by remember { mutableStateOf<Rect?>(null) }
    var menuRecordId by remember { mutableStateOf<String?>(null) }
    val menuVisible = remember { MutableTransitionState(false) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    MetroContextMenuClearOnDismiss(menuVisible) { menuRecordId = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding()
            .onGloballyPositioned { rootBounds = it.boundsInWindow() },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            MetroAppTitle(title = if (viewModel.isNewRoutine) "new routine" else "routine")
            MetroTextBox(
                value = routine.name,
                onValueChange = viewModel::renameRoutine,
                placeholder = "routine name",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            if (routine.exercises.isEmpty()) {
                MetroEmptyState(
                    message = "No exercises yet. Tap + to add one.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(routine.exercises, key = { it.prescription.id }) { record ->
                        RoutineExerciseRow(
                            record = record,
                            name = viewModel.exerciseFor(record.prescription.exerciseId)?.name ?: record.prescription.exerciseId,
                            onClick = { editing = record },
                            onLongPress = { bounds ->
                                menuTarget = bounds
                                menuRecordId = record.prescription.id
                                menuVisible.targetState = true
                            },
                        )
                        Divider()
                    }
                }
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(MetroSystemIconType.Check, "save", onClick = viewModel::saveRoutine),
                MetroAppBarIcon(MetroSystemIconType.Add, "add", onClick = { viewModel.openExercisePicker(forWorkout = false) }),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem("delete routine", onClick = { viewModel.deleteRoutine(routine) }),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        menuRecordId?.let { id ->
            MetroContextMenuPopup(
                visibleState = menuVisible,
                anchorBounds = menuTarget ?: Rect.Zero,
                rootBounds = rootBounds,
                items = listOf(
                    MetroContextMenuItem("move up", onClick = {
                        viewModel.moveRoutineExercise(id, -1); menuVisible.targetState = false
                    }),
                    MetroContextMenuItem("move down", onClick = {
                        viewModel.moveRoutineExercise(id, 1); menuVisible.targetState = false
                    }),
                    MetroContextMenuItem("remove", onClick = {
                        viewModel.removeRoutineExercise(id); menuVisible.targetState = false
                    }),
                ),
                onDismissRequest = { menuVisible.targetState = false },
            )
        }
    }

    editing?.let { record ->
        PrescriptionEditorDialog(
            record = record,
            exercise = viewModel.exerciseFor(record.prescription.exerciseId),
            onSave = { viewModel.updateRoutineExercise(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun RoutineExerciseRow(
    record: RoutineExercise,
    name: String,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val p = record.prescription
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .metroClickable { onClick() }
            .padding(vertical = 14.dp),
    ) {
        MetroText(text = name, style = MetroTextStyle.ListItemTitle)
        val rir = if (p.rirConfigured) " · ${p.targetRirMin}–${p.targetRirMax} RIR" else " · no RIR"
        MetroText(
            text = "${p.workSetCount} × ${p.repMin}–${p.repMax}$rir" +
                (if (p.autoProgressEnabled) " · auto" else " · manual"),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
private fun PrescriptionEditorDialog(
    record: RoutineExercise,
    exercise: ExerciseDefinition?,
    onSave: (RoutineExercisePrescription) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = record.prescription
    var sets by remember { mutableStateOf(p.workSetCount.toString()) }
    var repMin by remember { mutableStateOf(p.repMin.toString()) }
    var repMax by remember { mutableStateOf(p.repMax.toString()) }
    var rirMin by remember { mutableStateOf(p.targetRirMin?.toString() ?: "") }
    var rirMax by remember { mutableStateOf(p.targetRirMax?.toString() ?: "") }
    var rest by remember { mutableStateOf(((p.restSeconds ?: 120) / 60).toString()) }

    MetroMessageDialog(
        title = exercise?.name ?: "exercise",
        onDismissRequest = onDismiss,
        confirmLabel = "save",
        onConfirm = {
            onSave(
                p.copy(
                    workSetCount = sets.toIntOrNull() ?: p.workSetCount,
                    repMin = repMin.toIntOrNull() ?: p.repMin,
                    repMax = repMax.toIntOrNull() ?: p.repMax,
                    targetRirMin = rirMin.toIntOrNull(),
                    targetRirMax = rirMax.toIntOrNull(),
                    restSeconds = (rest.toIntOrNull() ?: 2) * 60,
                ),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FieldRow("sets", sets) { sets = it }
            FieldRow("rep min", repMin) { repMin = it }
            FieldRow("rep max", repMax) { repMax = it }
            FieldRow("RIR min", rirMin) { rirMin = it }
            FieldRow("RIR max", rirMax) { rirMax = it }
            FieldRow("rest (min)", rest) { rest = it }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(text = label, style = MetroTextStyle.ListItemSubtitle, modifier = Modifier.width(110.dp))
        MetroTextBox(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- exercise picker -----------------------------------------------------

@Composable
fun ExercisePickerScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val categories = listOf("all", "custom")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            MetroAppTitle(title = "add exercise")
            MetroTextBox(
                value = viewModel.pickerQuery,
                onValueChange = viewModel::updatePickerQuery,
                placeholder = "search",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
            MetroListPicker(
                options = categories,
                selectedOptionIndex = if (viewModel.pickerCategory == PickerCategory.All) 0 else 1,
                onSelectOption = {
                    viewModel.updatePickerCategory(if (it == 0) PickerCategory.All else PickerCategory.Custom)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            val muscles = listOf("all muscles") + MuscleGroup.values().map { it.name.lowercase().replace('_', ' ') }
            MetroListPicker(
                options = muscles,
                selectedOptionIndex = viewModel.pickerMuscle?.let { MuscleGroup.values().indexOf(it) + 1 } ?: 0,
                onSelectOption = { viewModel.choosePickerMuscle(if (it == 0) null else MuscleGroup.values()[it - 1]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            val equipment = listOf("all equipment") + EquipmentType.values().map { it.name.lowercase().replace('_', ' ') }
            MetroListPicker(
                options = equipment,
                selectedOptionIndex = viewModel.pickerEquipment?.let { EquipmentType.values().indexOf(it) + 1 } ?: 0,
                onSelectOption = { viewModel.choosePickerEquipment(if (it == 0) null else EquipmentType.values()[it - 1]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(viewModel.filteredExercises, key = { it.id }) { exercise ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metroClickable { viewModel.pickExercise(exercise) }
                            .padding(vertical = 12.dp),
                    ) {
                        MetroText(text = exercise.name, style = MetroTextStyle.ListItemTitle)
                        MetroText(
                            text = exerciseSubtitle(exercise),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                    Divider()
                }
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(MetroSystemIconType.Add, "custom", onClick = viewModel::startCustomExercise),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---- custom exercise -----------------------------------------------------

@Composable
fun CustomExerciseScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val draft = viewModel.customDraft ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MetroAppTitle(title = "custom exercise")
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            MetroTextBox(
                value = draft.name,
                onValueChange = { v -> viewModel.updateCustomDraft { it.copy(name = v) } },
                placeholder = "name",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            EnumPicker("movement", ExerciseMechanic.values(), draft.mechanic) { v ->
                viewModel.updateCustomDraft { it.copy(mechanic = v) }
            }
            EnumPicker("equipment", EquipmentType.values(), draft.equipment) { v ->
                viewModel.updateCustomDraft { it.copy(equipment = v) }
            }
            EnumPicker("resistance", ResistanceModel.values(), draft.resistanceModel) { v ->
                viewModel.updateCustomDraft { it.copy(resistanceModel = v) }
            }
            if (draft.loadBased) {
                EnumPicker("weight entry", LoadSemantics.values(), draft.loadSemantics) { v ->
                    viewModel.updateCustomDraft { it.copy(loadSemantics = v) }
                }
                if (draft.resistanceModel != ResistanceModel.ASSISTED_BODYWEIGHT) {
                    EnumPicker(
                        "progression",
                        arrayOf(ProgressionDirection.MORE_LOAD_IS_HARDER),
                        draft.progressionDirection,
                    ) { v -> viewModel.updateCustomDraft { it.copy(progressionDirection = v) } }
                }
                MetroTextBox(
                    value = draft.incrementKgText,
                    onValueChange = { v -> viewModel.updateCustomDraft { it.copy(incrementKgText = v) } },
                    placeholder = "minimum increment (kg)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
            SectionLabel("primary muscles")
            MuscleMultiSelect(draft.primaryMuscles) { next ->
                viewModel.updateCustomDraft { it.copy(primaryMuscles = next) }
            }
            SectionLabel("secondary muscles")
            MuscleMultiSelect(draft.secondaryMuscles) { next ->
                viewModel.updateCustomDraft { it.copy(secondaryMuscles = next) }
            }
            MetroText(
                text = draft.validationSummary(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(MetroSystemIconType.Check, "save", onClick = viewModel::saveCustomExercise),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun <T> EnumPicker(
    label: String,
    values: Array<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val names = values.map { it.toString().lowercase().replace('_', ' ') }
    MetroListPicker(
        options = names,
        selectedOptionIndex = values.indexOf(selected).coerceAtLeast(0),
        onSelectOption = { onSelect(values[it]) },
        label = label,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

// ---- exercise detail -----------------------------------------------------

@Composable
fun ExerciseDetailScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val progress = viewModel.selectedExerciseProgress ?: return
    var metric by remember { mutableStateOf(ProgressMetric.ESTIMATED_1RM) }
    var range by remember { mutableStateOf(ProgressRange.THREE_MONTHS) }
    val points = remember(progress.exposures, metric, range) {
        ExerciseAnalytics.series(progress.exposures, metric, range)
    }
    val valueText: (Double) -> String = { viewModel.formatWeight(it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ExerciseImageHeader(progress.exercise.name, progress.exercise.id)
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                MetadataBlock(progress.exercise)

                ExerciseInstructions.forExercise(progress.exercise.id)?.let { info ->
                    SectionLabel("how to")
                    MetroText(
                        text = listOf(info.level, info.category, info.force)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                    info.instructions.forEachIndexed { index, step ->
                        MetroText(
                            text = "${index + 1}.  $step",
                            style = MetroTextStyle.ListItemSubtitle,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }

                SectionLabel("personal records")
                val bestLoad = progress.exposures.mapNotNull { ExerciseAnalytics.valueOf(it, ProgressMetric.TOP_SET_LOAD) }.maxOrNull()
                val bestOneRm = progress.exposures.mapNotNull { ExerciseAnalytics.valueOf(it, ProgressMetric.ESTIMATED_1RM) }.maxOrNull()
                val bestReps = progress.exposures.flatMap { it.sets }.filter { it.completed }.maxOfOrNull { it.repsCompleted }
                val bestVolume = progress.exposures.mapNotNull { ExerciseAnalytics.valueOf(it, ProgressMetric.VOLUME_LOAD) }.maxOrNull()
                listOfNotNull(
                    bestLoad?.let { "heaviest set · ${viewModel.formatWeight(it)}" },
                    bestOneRm?.let { "estimated 1RM · ${viewModel.formatWeight(it)}" },
                    bestReps?.let { "most reps · $it" },
                    bestVolume?.let { "best session volume · ${viewModel.formatWeight(it)}" },
                ).forEach { line ->
                    MetroText(text = line, style = MetroTextStyle.ListItemTitle, modifier = Modifier.padding(vertical = 4.dp))
                }

                SectionLabel("progress")
                MetroSegmentedRow(
                    options = ProgressMetric.values().toList(),
                    selected = metric,
                    label = { it.label },
                    onSelect = { metric = it },
                )
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    MetroSegmentedRow(
                        options = ProgressRange.values().toList(),
                        selected = range,
                        label = { it.label },
                        onSelect = { range = it },
                    )
                }
                MetroLineChart(
                    points = points,
                    valueText = valueText,
                    modifier = Modifier.padding(top = 8.dp),
                )

                SectionLabel("history")
                val recent = progress.exposures.take(12)
                if (recent.isEmpty()) {
                    MetroText(
                        text = "No comparable history yet.",
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                } else {
                    recent.forEach { exposure ->
                        MetroText(
                            text = exposure.workSets.joinToString(" · ") { set ->
                                val load = set.load?.let { "${viewModel.formatWeight(it)} × " } ?: ""
                                "$load${set.repsCompleted}"
                            },
                            style = MetroTextStyle.ListItemTitle,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        Divider()
                    }
                }
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Delete,
                    label = "archive",
                    onClick = { viewModel.archiveExercise(progress.exercise) },
                    enabled = !progress.exercise.builtIn,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Square exercise illustration with a two-frame Metro flip; falls back to a text header. */
@Composable
private fun ExerciseImageHeader(name: String, exerciseId: String) {
    val frame0 = exerciseImageRes(exerciseId, 0)
    val frame1 = exerciseImageRes(exerciseId, 1)
    MetroAppTitle(title = name)
    if (frame0 == null) return
    var showSecond by remember { mutableStateOf(false) }
    LaunchedEffect(exerciseId) {
        while (true) {
            delay(1400)
            showSecond = !showSecond
        }
    }
    val current = if (showSecond && frame1 != null) frame1 else frame0
    Image(
        painter = painterResource(current),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(320.dp),
        contentScale = ContentScale.Crop,
    )
}


@Composable
private fun MetadataBlock(exercise: ExerciseDefinition) {
    val lines = listOf(
        exercise.mechanic.name.lowercase(),
        exercise.equipment.name.lowercase().replace('_', ' '),
        "weight: ${exercise.loadSemantics.name.lowercase().replace('_', ' ')}",
        "progression: ${exercise.progressionDirection.name.lowercase().replace('_', ' ')}",
        exercise.defaultIncrement?.let { "increment: ${it} kg" } ?: "reps only",
        "primary: ${exercise.primaryMuscles.joinToString { it.name.lowercase() }}",
        exercise.secondaryMuscles.takeIf { it.isNotEmpty() }
            ?.let { "secondary: ${it.joinToString { m -> m.name.lowercase() }}" },
    ).filterNotNull()
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        lines.forEach { line ->
            MetroText(
                text = line,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

// ---- settings ------------------------------------------------------------

@Composable
fun SettingsScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val units = listOf("kg", "lb")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            MetroAppTitle(title = "settings")
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SectionLabel("units")
            MetroListPicker(
                options = units,
                selectedOptionIndex = if (viewModel.weightUnit == LoadUnit.KG) 0 else 1,
                onSelectOption = { viewModel.chooseWeightUnit(if (it == 0) LoadUnit.KG else LoadUnit.LB) },
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel("effort")
            ToggleRow("log RIR", viewModel.rirEnabled) { viewModel.toggleRir(it) }
            SectionLabel("workout")
            ToggleRow("smart progression hints", viewModel.smartHints) { viewModel.toggleSmartHints(it) }
            MetroText(
                text = "Theme and accent follow the MetroOS system settings.",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 16.dp),
            )
            }
        }
        MetroAppBar(
            icons = listOf(MetroAppBarIcon(MetroSystemIconType.Back, "back", onClick = onBack)),
            menuItems = listOf(MetroAppBarMenuItem("rebuild plan", onClick = viewModel::startOnboarding)),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(text = label, style = MetroTextStyle.ListItemTitle, modifier = Modifier.weight(1f))
        MetroCheckBox(checked = checked, onCheckedChange = onChange)
    }
}
