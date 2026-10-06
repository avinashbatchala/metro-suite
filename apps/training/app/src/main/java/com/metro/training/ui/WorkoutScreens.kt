@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.metro.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.training.domain.progression.WeightMath
import com.metro.training.domain.workout.SetQuality
import com.metro.training.domain.workout.SetType
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutExercise
import com.metro.training.domain.workout.WorkoutSet
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val workout = viewModel.activeWorkout ?: return
    var expandedSetId by remember(workout.id) { mutableStateOf<String?>(null) }
    var menuExerciseId by remember { mutableStateOf<String?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val menuVisible = remember { MutableTransitionState(false) }
    MetroContextMenuClearOnDismiss(menuVisible) { menuExerciseId = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .metroNavBarPadding()
            .onGloballyPositioned { rootBounds = it.boundsInWindow() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MetroAppTitle(title = workout.routineName.ifBlank { "workout" })
            WorkoutHeader(viewModel = viewModel, workout = workout)
            RestBanner(viewModel = viewModel, workout = workout)
            workout.exercises.forEach { exercise ->
                WorkoutExerciseBlock(
                    viewModel = viewModel,
                    exercise = exercise,
                    expandedSetId = expandedSetId,
                    onToggleSet = { setId -> expandedSetId = if (expandedSetId == setId) null else setId },
                    onOpenMenu = { rect ->
                        menuExerciseId = exercise.id
                        menuAnchor = rect
                        menuVisible.targetState = true
                    },
                )
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = "add",
                    onClick = { viewModel.openExercisePicker(forWorkout = true) },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = "finish",
                    onClick = viewModel::finishWorkout,
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem("workout note", onClick = viewModel::openWorkoutNote),
                MetroAppBarMenuItem("discard workout", onClick = viewModel::discardWorkout),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val menuId = menuExerciseId
        if (menuId != null) {
            val exercise = workout.exercises.firstOrNull { it.id == menuId }
            ExerciseMenu(
                visibleState = menuVisible,
                anchor = menuAnchor,
                rootBounds = rootBounds,
                canMoveUp = workout.exercises.firstOrNull()?.id != menuId,
                canMoveDown = workout.exercises.lastOrNull()?.id != menuId,
                inSuperset = exercise?.supersetTag != null,
                onDismiss = { menuVisible.targetState = false },
                viewModel = viewModel,
                exerciseId = menuId,
            )
        }
    }

    viewModel.workoutNoteDraft?.let { draft ->
        WorkoutNoteDialog(
            value = draft,
            onChange = viewModel::updateWorkoutNoteDraft,
            onSave = viewModel::saveWorkoutNote,
            onDismiss = viewModel::dismissWorkoutNote,
        )
    }

    viewModel.prEvent?.let { event ->
        PrCelebration(event = event, onDismiss = viewModel::consumePrEvent)
    }
}

@Composable
private fun WorkoutHeader(viewModel: TrainingViewModel, workout: Workout) {
    var elapsed by remember(workout.id) { mutableLongStateOf(0L) }
    LaunchedEffect(workout.id) {
        while (true) {
            elapsed = ((System.currentTimeMillis() - workout.startedAt) / 1000L).coerceAtLeast(0L)
            delay(1000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = "%d:%02d".format(elapsed / 60, elapsed % 60),
            style = MetroTextStyle.ListItemTitle,
        )
        Spacer(modifier = Modifier.width(16.dp))
        MetroText(
            text = "${viewModel.formatWeight(viewModel.workoutVolumeKg())} volume · ${viewModel.completedSetCount()} sets",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
    }
    Divider()
}

@Composable
private fun RestBanner(viewModel: TrainingViewModel, workout: Workout) {
    val deadline = workout.restDeadlineMillis ?: return
    var remaining by remember(deadline) { mutableLongStateOf(0L) }
    val startedAt = remember(deadline) { System.currentTimeMillis() }
    val total = (deadline - startedAt).coerceAtLeast(1L)
    LaunchedEffect(deadline) {
        while (true) {
            remaining = ((deadline - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            if (remaining <= 0L) break
            delay(1000)
        }
    }
    val fraction = (remaining * 1000L).toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroText(text = "REST", style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.accent)
            Spacer(modifier = Modifier.width(12.dp))
            MetroText(text = "%d:%02d".format(remaining / 60, remaining % 60), style = MetroTextStyle.ListItemTitle)
            Spacer(modifier = Modifier.weight(1f))
            MetroText(
                text = "+30",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(horizontal = 8.dp).metroClickable { viewModel.startRest(30) },
            )
            MetroText(
                text = "skip",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier.metroClickable { viewModel.clearRest() },
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(MetroTheme.colors.accent),
            )
        }
    }
}

@Composable
private fun ExerciseMenu(
    visibleState: MutableTransitionState<Boolean>,
    anchor: Rect,
    rootBounds: Rect,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    inSuperset: Boolean,
    onDismiss: () -> Unit,
    viewModel: TrainingViewModel,
    exerciseId: String,
) {
    var swapFor by remember { mutableStateOf<String?>(null) }
    var noteFor by remember { mutableStateOf<String?>(null) }
    var plateFor by remember { mutableStateOf<String?>(null) }

    MetroContextMenuPopup(
        visibleState = visibleState,
        anchorBounds = anchor,
        rootBounds = rootBounds,
        items = buildList {
            add(MetroContextMenuItem("exercise note", onClick = { noteFor = exerciseId; visibleState.targetState = false }))
            add(MetroContextMenuItem("add warm-up sets", onClick = { viewModel.addWarmupSets(exerciseId); visibleState.targetState = false }))
            add(MetroContextMenuItem("plate calculator", onClick = { plateFor = exerciseId; visibleState.targetState = false }))
            add(MetroContextMenuItem("replace exercise", onClick = { swapFor = exerciseId; visibleState.targetState = false }))
            add(MetroContextMenuItem("move up", enabled = canMoveUp, onClick = { viewModel.moveExercise(exerciseId, -1); visibleState.targetState = false }))
            add(MetroContextMenuItem("move down", enabled = canMoveDown, onClick = { viewModel.moveExercise(exerciseId, 1); visibleState.targetState = false }))
            add(
                MetroContextMenuItem(
                    if (inSuperset) "remove from superset" else "add to superset",
                    onClick = {
                        viewModel.setSupersetTag(exerciseId, if (inSuperset) null else "A")
                        visibleState.targetState = false
                    },
                ),
            )
            add(MetroContextMenuItem("history", onClick = {
                viewModel.activeWorkout?.exercises?.firstOrNull { it.id == exerciseId }
                    ?.let { we -> viewModel.exerciseFor(we.exerciseId)?.let(viewModel::openExerciseProgress) }
                visibleState.targetState = false
            }))
            add(MetroContextMenuItem("remove", onClick = { viewModel.removeExercise(exerciseId); visibleState.targetState = false }))
        },
        onDismissRequest = { visibleState.targetState = false },
    )

    swapFor?.let { weId ->
        SwapDialog(
            candidates = viewModel.swapCandidates(
                viewModel.activeWorkout?.exercises?.firstOrNull { it.id == weId }?.exerciseId.orEmpty(),
            ),
            onPick = { newId -> viewModel.replaceExercise(weId, newId); swapFor = null },
            onDismiss = { swapFor = null },
        )
    }
    noteFor?.let {
        viewModel.activeWorkout?.exercises?.firstOrNull { ex -> ex.id == it }?.let { we ->
            ExerciseNoteDialog(
                initial = we.note,
                onSave = { text -> viewModel.setExerciseNote(it, text); noteFor = null },
                onDismiss = { noteFor = null },
            )
        }
    }
    plateFor?.let { weId ->
        val working = viewModel.activeWorkout?.exercises?.firstOrNull { it.id == weId }
            ?.sets?.firstOrNull { it.setType.feedsProgression }?.load
        PlateCalculatorDialog(
            viewModel = viewModel,
            initial = working ?: viewModel.barWeightKg,
            onDismiss = { plateFor = null },
        )
    }
}

@Composable
private fun PlateCalculatorDialog(
    viewModel: TrainingViewModel,
    initial: Double,
    onDismiss: () -> Unit,
) {
    var target by remember { mutableStateOf(viewModel.weightNumber(initial)) }
    val targetKg = viewModel.parseWeight(target)
    val plates = targetKg?.let { viewModel.plateBreakdown(it) }
    MetroMessageDialog(title = "plate calculator", onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MetroTextBox(
                value = target,
                onValueChange = { target = it },
                placeholder = "target (${viewModel.weightSuffix})",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetroText(
                text = "bar ${viewModel.formatWeight(viewModel.barWeightKg)}",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
            MetroText(
                text = when {
                    targetKg == null -> "enter a target"
                    plates == null -> "not achievable with your plates"
                    plates.isEmpty() -> "bar only"
                    else -> "per side: " + com.metro.training.domain.equipment.PlateMath.describe(plates)
                },
                style = MetroTextStyle.ListItemTitle,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun WorkoutExerciseBlock(
    viewModel: TrainingViewModel,
    exercise: WorkoutExercise,
    expandedSetId: String?,
    onToggleSet: (String) -> Unit,
    onOpenMenu: (Rect) -> Unit,
) {
    val snapshot = exercise.snapshot
    val imageRes = exerciseImageRes(exercise.exerciseId, 0)
    var headerBounds by remember { mutableStateOf(Rect.Zero) }
    val previous = viewModel.previousByExercise[exercise.id].orEmpty()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onGloballyPositioned { headerBounds = it.boundsInWindow() },
        ) {
            if (imageRes != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    exercise.supersetTag?.let {
                        MetroText(
                            text = "superset $it",
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.accent,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    MetroText(text = exercise.exerciseName, style = MetroTextStyle.ListItemTitle)
                }
                val rir = if (snapshot.targetRirMin != null && snapshot.targetRirMax != null) {
                    " · ${snapshot.targetRirMin}–${snapshot.targetRirMax} RIR"
                } else {
                    ""
                }
                MetroText(
                    text = "target ${snapshot.repMin}–${snapshot.repMax}$rir",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
            MetroText(
                text = "⋯",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier
                    .metroClickable { onOpenMenu(headerBounds) }
                    .padding(horizontal = 8.dp),
            )
        }
        if (exercise.note.isNotBlank()) {
            MetroText(
                text = exercise.note,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            HeaderCell("SET", 0.6f)
            HeaderCell("PREVIOUS", 1.1f)
            HeaderCell(viewModel.weightSuffix.uppercase(), 1f)
            HeaderCell("REPS", 0.8f)
            HeaderCell("RIR", 0.6f)
            HeaderCell("", 0.5f)
        }

        var prescribedSeen = 0
        exercise.sets.forEach { set ->
            val previousValues = if (set.setType.feedsProgression && set.prescribed) {
                previous.getOrNull(prescribedSeen++)
            } else {
                null
            }
            SetRow(
                viewModel = viewModel,
                set = set,
                previous = previousValues,
                expanded = expandedSetId == set.id,
                onClick = { onToggleSet(set.id) },
            )
            if (expandedSetId == set.id) {
                SetEditorPanel(viewModel = viewModel, exercise = exercise, set = set)
            }
        }

        Row(modifier = Modifier.padding(top = 6.dp)) {
            MetroText(
                text = "+ set",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .metroClickable { viewModel.addSet(exercise.id, SetType.WORK) }
                    .padding(end = 16.dp)
                    .padding(vertical = 4.dp),
            )
            MetroText(
                text = "+ warm-up",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .metroClickable { viewModel.addSet(exercise.id, SetType.WARMUP) }
                    .padding(vertical = 4.dp),
            )
        }
        Divider()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    MetroText(
        text = text,
        style = MetroTextStyle.ListItemSubtitle,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.weight(weight),
    )
}

private fun setLabel(set: WorkoutSet): String = when (set.setType) {
    SetType.WARMUP -> "W"
    SetType.FAILURE -> "F"
    SetType.DROP -> "D"
    SetType.BACKOFF -> "B"
    SetType.MYOREP -> "M"
    SetType.WORK -> set.setIndex.toString()
}

@Composable
private fun setLabelColor(set: WorkoutSet): Color =
    if (set.setType == SetType.WORK) MetroTheme.colors.primaryText else MetroTheme.colors.accent

@Composable
private fun SetRow(
    viewModel: TrainingViewModel,
    set: WorkoutSet,
    previous: Triple<Double?, Int, Int?>?,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(set.completed) {
        if (set.completed) { flash.snapTo(1f); flash.animateTo(0f, tween(450)) }
    }
    val background = lerp(MetroTheme.colors.background, MetroTheme.colors.accent, flash.value * 0.22f)
    val previousText = previous?.let { (load, reps, rir) ->
        val loadText = load?.let { viewModel.weightNumber(it) } ?: "–"
        "$loadText×$reps" + (rir?.let { " @$it" } ?: "")
    } ?: "–"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (expanded) MetroTheme.colors.secondarySurface else background)
            .metroClickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = setLabel(set),
            style = MetroTextStyle.ListItemTitle,
            color = setLabelColor(set),
            modifier = Modifier.weight(0.6f),
        )
        MetroText(
            text = previousText,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.weight(1.1f),
        )
        MetroText(
            text = viewModel.weightNumber(set.load).ifBlank { "–" },
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.weight(1f),
        )
        MetroText(
            text = if (set.isPerSide) {
                "${set.repsLeft ?: 0}/${set.repsRight ?: 0}"
            } else if (set.repsCompleted > 0) {
                set.repsCompleted.toString()
            } else {
                "–"
            },
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.weight(0.8f),
        )
        MetroText(
            text = set.rir?.toString() ?: "–",
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.weight(0.6f),
        )
        Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
            com.metro.ui.MetroCheckBox(
                checked = set.completed,
                onCheckedChange = { checked ->
                    if (checked) viewModel.completeSet(set.id) else viewModel.updateSet(set.id) { it.copy(completed = false) }
                },
            )
        }
    }
}

@Composable
private fun SetEditorPanel(
    viewModel: TrainingViewModel,
    exercise: WorkoutExercise,
    set: WorkoutSet,
) {
    val increment = exercise.snapshot.incrementKg
    var perSide by remember(set.id) { mutableStateOf(set.isPerSide) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MetroTheme.colors.secondarySurface)
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberField(
                label = "load (${viewModel.weightSuffix})",
                value = viewModel.weightNumber(set.load),
                decimal = true,
                modifier = Modifier.weight(1f),
                onCommit = { text -> viewModel.updateSet(set.id) { it.copy(load = viewModel.parseWeight(text) ?: it.load) } },
                onStep = { dir ->
                    val current = set.load
                    if (current != null && increment != null) {
                        val next = if (dir > 0) current + increment else (current - increment).coerceAtLeast(0.0)
                        viewModel.updateSet(set.id) { it.copy(load = WeightMath.clean(next)) }
                    }
                },
            )
            NumberField(
                label = "reps",
                value = if (set.repsCompleted > 0) set.repsCompleted.toString() else "",
                decimal = false,
                modifier = Modifier.weight(1f),
                onCommit = { text -> viewModel.updateSet(set.id) { it.copy(repsCompleted = text.toIntOrNull() ?: it.repsCompleted) } },
                onStep = { dir -> viewModel.updateSet(set.id) { it.copy(repsCompleted = (it.repsCompleted + dir).coerceAtLeast(0)) } },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        MetroText(text = "RIR", style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
        RirSegmented(selected = set.rir) { rir -> viewModel.updateSet(set.id) { it.copy(rir = rir) } }

        Spacer(modifier = Modifier.height(8.dp))
        MetroText(text = "set type", style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
        TypeSegmented(selected = set.setType) { type -> viewModel.changeSetType(set.id, type) }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroText(
                text = "per side",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.weight(1f),
            )
            com.metro.ui.MetroCheckBox(checked = perSide, onCheckedChange = { checked ->
                perSide = checked
                viewModel.updateSet(set.id) {
                    if (checked) it.copy(repsLeft = it.repsLeft ?: it.repsCompleted, repsRight = it.repsRight ?: it.repsCompleted)
                    else it.copy(repsLeft = null, repsRight = null)
                }
            })
        }
        if (perSide) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberField(
                    label = "left reps",
                    value = (set.repsLeft ?: 0).toString(),
                    decimal = false,
                    modifier = Modifier.weight(1f),
                    onCommit = { text -> viewModel.updateSet(set.id) { it.copy(repsLeft = text.toIntOrNull() ?: it.repsLeft) } },
                    onStep = { dir -> viewModel.updateSet(set.id) { it.copy(repsLeft = ((it.repsLeft ?: 0) + dir).coerceAtLeast(0)) } },
                )
                NumberField(
                    label = "right reps",
                    value = (set.repsRight ?: 0).toString(),
                    decimal = false,
                    modifier = Modifier.weight(1f),
                    onCommit = { text -> viewModel.updateSet(set.id) { it.copy(repsRight = text.toIntOrNull() ?: it.repsRight) } },
                    onStep = { dir -> viewModel.updateSet(set.id) { it.copy(repsRight = ((it.repsRight ?: 0) + dir).coerceAtLeast(0)) } },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        NumberField(
            label = "partial reps",
            value = (set.partialReps ?: 0).toString(),
            decimal = false,
            modifier = Modifier.fillMaxWidth(),
            onCommit = { text -> viewModel.updateSet(set.id) { it.copy(partialReps = text.toIntOrNull()) } },
            onStep = { dir -> viewModel.updateSet(set.id) { it.copy(partialReps = ((it.partialReps ?: 0) + dir).coerceAtLeast(0)) } },
        )

        Spacer(modifier = Modifier.height(8.dp))
        var note by remember(set.id, set.note) { mutableStateOf(set.note) }
        MetroTextBox(
            value = note,
            onValueChange = { note = it; viewModel.updateSet(set.id) { s -> s.copy(note = it) } },
            placeholder = "set note",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = "delete set",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.accent,
            modifier = Modifier.metroClickable { viewModel.removeSet(set.id) }.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    decimal: Boolean,
    onCommit: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value, focused) { if (!focused) text = value }
    Column(modifier = modifier) {
        MetroText(text = label, style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetroText(
                text = "−",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.metroClickable { onStep(-1) }.padding(horizontal = 6.dp),
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it; onCommit(it) },
                singleLine = true,
                textStyle = TextStyle(
                    color = MetroTheme.colors.primaryText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                ),
                cursorBrush = SolidColor(MetroTheme.colors.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
            )
            MetroText(
                text = "+",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.metroClickable { onStep(1) }.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
private fun RirSegmented(selected: Int?, onSelect: (Int?) -> Unit) {
    val options: List<Int?> = listOf(null, 0, 1, 2, 3, 4, 5)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { option ->
            val active = option == selected
            Box(modifier = Modifier.metroClickable { onSelect(option) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                MetroText(
                    text = option?.let { if (it == 5) "5+" else it.toString() } ?: "–",
                    style = MetroTextStyle.ListItemTitle,
                    color = if (active) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun TypeSegmented(selected: SetType, onSelect: (SetType) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(SetType.WORK, SetType.WARMUP, SetType.FAILURE, SetType.DROP, SetType.BACKOFF, SetType.MYOREP).forEach { type ->
            val active = type == selected
            MetroText(
                text = type.name.lowercase().replace('_', ' '),
                style = MetroTextStyle.ListItemSubtitle,
                color = if (active) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                modifier = Modifier.metroClickable { onSelect(type) }.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SwapDialog(
    candidates: List<com.metro.training.domain.exercises.ExerciseDefinition>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    MetroMessageDialog(title = "replace exercise", onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            candidates.forEach { candidate ->
                MetroText(
                    text = candidate.name,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.metroClickable { onPick(candidate.id) }.padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseNoteDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    MetroMessageDialog(
        title = "exercise note",
        onDismissRequest = onDismiss,
        confirmLabel = "save",
        onConfirm = { onSave(text) },
    ) {
        MetroTextBox(value = text, onValueChange = { text = it }, placeholder = "note", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WorkoutNoteDialog(value: String, onChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    MetroMessageDialog(
        title = "workout note",
        onDismissRequest = onDismiss,
        confirmLabel = "save",
        onConfirm = onSave,
    ) {
        MetroTextBox(value = value, onValueChange = onChange, placeholder = "note", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PrCelebration(event: PrEvent, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(event) {
        alpha.animateTo(1f, tween(180))
        scale.animateTo(1f, tween(280))
        delay(1500)
        alpha.animateTo(0f, tween(240))
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .background(MetroTheme.colors.accent)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MetroText(text = "PERSONAL BEST", style = MetroTextStyle.SectionHeader, color = Color.White)
            MetroText(text = event.exerciseName, style = MetroTextStyle.PivotTab, color = Color.White)
            MetroText(text = event.kind, style = MetroTextStyle.ListItemSubtitle, color = Color.White)
        }
    }
}

// ---- summary -------------------------------------------------------------

@Composable
fun WorkoutSummaryScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val workout = viewModel.summaryWorkout ?: return
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
            MetroAppTitle(title = "workout complete")
            MetroText(
                text = "${formatDuration(workout.durationMillis)} · ${workout.completedExposureCount} work sets · " +
                    "${viewModel.formatWeight(workout.exercises.flatMap { it.sets }
                        .filter { it.completed && it.load != null }
                        .sumOf { (it.load ?: 0.0) * it.repsCompleted })} volume",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            SectionLabel("exercises")
            workout.exercises.forEach { exercise ->
                MetroText(
                    text = exercise.exerciseName,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
                MetroText(
                    text = exercise.sets.filter { it.completed }.joinToString(" · ") { set ->
                        val load = set.load?.let { "${viewModel.weightNumber(it)}×" } ?: ""
                        "$load${set.repsCompleted}"
                    }.ifBlank { "no completed sets" },
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
                Divider()
            }
            SectionLabel("next time")
            viewModel.summaryRecommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metroClickable { viewModel.openRecommendation(recommendation) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MetroText(text = recommendation.exerciseName, style = MetroTextStyle.ListItemTitle)
                        MetroText(
                            text = recommendationText(viewModel, recommendation),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                    MetroText(
                        text = decisionGlyph(recommendation.decision),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.accent,
                    )
                }
                Divider()
            }
        }
        MetroAppBar(
            icons = listOf(MetroAppBarIcon(MetroSystemIconType.Back, "done", onClick = onBack)),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun decisionGlyph(decision: com.metro.training.domain.progression.ProgressionDecision): String =
    when (decision) {
        com.metro.training.domain.progression.ProgressionDecision.ADD_LOAD -> "↑ load"
        com.metro.training.domain.progression.ProgressionDecision.ADD_REPS -> "↑ reps"
        com.metro.training.domain.progression.ProgressionDecision.REDUCE_LOAD -> "↓ load"
        com.metro.training.domain.progression.ProgressionDecision.REVIEW -> "review"
        else -> "hold"
    }

internal fun recommendationText(viewModel: TrainingViewModel, view: RecommendationView): String {
    val load = view.nextLoad?.let { viewModel.formatWeight(it) }
    val target = view.repTargetText
    val current = view.currentLoad?.let { "from ${viewModel.formatWeight(it)}" }
    return listOfNotNull(load, target, current).joinToString(" · ")
}

@Composable
fun RecommendationDialog(viewModel: TrainingViewModel, view: RecommendationView, onDismiss: () -> Unit) {
    MetroMessageDialog(
        title = view.exerciseName,
        onDismissRequest = onDismiss,
        confirmLabel = "accept",
        onConfirm = { viewModel.acceptRecommendation(view, view.nextLoad, null); onDismiss() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MetroText(text = view.headline, style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.accent)
            view.nextLoad?.let {
                MetroText(text = viewModel.formatWeight(it), style = MetroTextStyle.PivotTab, modifier = Modifier.padding(vertical = 6.dp))
            }
            view.repTargetText?.let {
                MetroText(text = it, style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
            }
            MetroText(text = "why?", style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.secondaryText, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            view.explanation.forEach { line ->
                MetroText(text = line, style = MetroTextStyle.ListItemSubtitle, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
