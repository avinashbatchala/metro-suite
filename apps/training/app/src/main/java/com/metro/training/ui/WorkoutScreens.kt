@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.metro.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val workout = viewModel.activeWorkout ?: return
    var qualityTarget by remember { mutableStateOf<Pair<String, Rect>?>(null) }
    val qualityVisible = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    MetroContextMenuClearOnDismiss(qualityVisible) { qualityTarget = null }

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
            RestBanner(viewModel, workout)
            workout.exercises.forEach { exercise ->
                WorkoutExerciseBlock(
                    viewModel = viewModel,
                    exercise = exercise,
                    onOpenQuality = { setId, rect ->
                        qualityTarget = setId to rect
                        qualityVisible.targetState = true
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
                MetroAppBarMenuItem("discard workout", onClick = viewModel::discardWorkout),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        qualityTarget?.let { (setId, rect) ->
            MetroContextMenuPopup(
                visibleState = qualityVisible,
                anchorBounds = rect,
                rootBounds = rootBounds,
                items = SetQuality.values().map { quality ->
                    MetroContextMenuItem(
                        label = quality.name.lowercase().replace('_', ' '),
                        onClick = {
                            viewModel.updateSet(setId) { it.copy(quality = quality) }
                            qualityVisible.targetState = false
                        },
                    )
                },
                onDismissRequest = { qualityVisible.targetState = false },
            )
        }
    }

    viewModel.prEvent?.let { event ->
        PrCelebration(event = event, onDismiss = viewModel::consumePrEvent)
    }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .padding(top = 6.dp),
        ) {
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
private fun WorkoutExerciseBlock(
    viewModel: TrainingViewModel,
    exercise: WorkoutExercise,
    onOpenQuality: (String, Rect) -> Unit,
) {
    val snapshot = exercise.snapshot
    val imageRes = exerciseImageRes(exercise.exerciseId, 0)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (imageRes != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                MetroText(text = exercise.exerciseName, style = MetroTextStyle.ListItemTitle)
                val rir = if (snapshot.targetRirMin != null && snapshot.targetRirMax != null) {
                    " · ${snapshot.targetRirMin}–${snapshot.targetRirMax} RIR"
                } else {
                    " · no RIR"
                }
                MetroText(
                    text = "next · ${snapshot.repMin}–${snapshot.repMax} reps$rir",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            HeaderCell("SET", 0.7f)
            HeaderCell(viewModel.weightSuffix.uppercase(), 1.5f)
            HeaderCell("REPS", 1.1f)
            HeaderCell("RIR", 1.6f)
            HeaderCell("", 0.5f)
        }
        exercise.sets.forEach { set ->
            InlineSetRow(
                viewModel = viewModel,
                exercise = exercise,
                set = set,
                onOpenQuality = onOpenQuality,
            )
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

@Composable
private fun InlineSetRow(
    viewModel: TrainingViewModel,
    exercise: WorkoutExercise,
    set: WorkoutSet,
    onOpenQuality: (String, Rect) -> Unit,
) {
    val increment = exercise.snapshot.incrementKg
    val isWarmup = set.setType == SetType.WARMUP
    val label = if (isWarmup) "W${set.setIndex}" else "${set.setIndex}"
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(set.completed) {
        if (set.completed) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(450))
        }
    }
    val rowBackground = lerp(
        MetroTheme.colors.background,
        MetroTheme.colors.accent,
        flash.value * 0.22f,
    )
    val qualityColor = if (set.quality == SetQuality.NORMAL) {
        if (isWarmup) MetroTheme.colors.secondaryText else MetroTheme.colors.primaryText
    } else {
        MetroTheme.colors.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .background(rowBackground)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            color = qualityColor,
            modifier = Modifier
                .weight(0.7f)
                .metroClickable { onOpenQuality(set.id, bounds) },
        )
        InlineNumberCell(
            valueText = viewModel.weightNumber(set.load),
            weight = 1.5f,
            keyboardType = KeyboardType.Decimal,
            onCommit = { text ->
                viewModel.updateSet(set.id) { it.copy(load = viewModel.parseWeight(text) ?: it.load) }
            },
            onStep = { direction ->
                val current = set.load
                val step = increment
                if (current != null && step != null) {
                    val next = if (direction > 0) current + step else (current - step).coerceAtLeast(0.0)
                    viewModel.updateSet(set.id) { it.copy(load = WeightMath.clean(next)) }
                }
            },
        )
        InlineNumberCell(
            valueText = if (set.repsCompleted > 0) set.repsCompleted.toString() else "",
            weight = 1.1f,
            keyboardType = KeyboardType.Number,
            onCommit = { text ->
                viewModel.updateSet(set.id) { it.copy(repsCompleted = text.toIntOrNull() ?: it.repsCompleted) }
            },
            onStep = { direction ->
                val next = (set.repsCompleted + direction).coerceAtLeast(0)
                viewModel.updateSet(set.id) { it.copy(repsCompleted = next) }
            },
        )
        RirSegmented(
            selected = set.rir,
            onSelect = { rir -> viewModel.updateSet(set.id) { it.copy(rir = rir) } },
            modifier = Modifier.weight(1.6f),
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
private fun androidx.compose.foundation.layout.RowScope.InlineNumberCell(
    valueText: String,
    weight: Float,
    keyboardType: KeyboardType,
    onCommit: (String) -> Unit,
    onStep: (Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(valueText) }
    LaunchedEffect(valueText, focused) { if (!focused) text = valueText }

    Row(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = "−",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.metroClickable { onStep(-1) }.padding(horizontal = 4.dp),
        )
        BasicTextField(
            value = text,
            onValueChange = { text = it; onCommit(it) },
            singleLine = true,
            textStyle = TextStyle(
                color = MetroTheme.colors.primaryText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MetroTheme.colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
            modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(requester)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    if (state.isFocused) scope.launch { requester.bringIntoView() }
                },
        )
        MetroText(
            text = "+",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.metroClickable { onStep(1) }.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun RirSegmented(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Int?> = listOf(null, 0, 1, 2, 3, 4, 5)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .metroClickable { onSelect(option) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                MetroText(
                    text = option?.let { if (it == 5) "5+" else it.toString() } ?: "–",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = if (active) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                )
            }
        }
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
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
                text = "${formatDuration(workout.durationMillis)} · ${workout.completedExposureCount} work sets",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
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
            icons = listOf(
                MetroAppBarIcon(MetroSystemIconType.Back, "done", onClick = onBack),
            ),
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
fun RecommendationDialog(
    viewModel: TrainingViewModel,
    view: RecommendationView,
    onDismiss: () -> Unit,
) {
    MetroMessageDialog(
        title = view.exerciseName,
        onDismissRequest = onDismiss,
        confirmLabel = "accept",
        onConfirm = {
            viewModel.acceptRecommendation(view, view.nextLoad, null)
            onDismiss()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MetroText(text = view.headline, style = MetroTextStyle.SectionHeader, color = MetroTheme.colors.accent)
            view.nextLoad?.let {
                MetroText(
                    text = viewModel.formatWeight(it),
                    style = MetroTextStyle.PivotTab,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            view.repTargetText?.let {
                MetroText(text = it, style = MetroTextStyle.ListItemSubtitle, color = MetroTheme.colors.secondaryText)
            }
            MetroText(
                text = "why?",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            )
            view.explanation.forEach { line ->
                MetroText(
                    text = line,
                    style = MetroTextStyle.ListItemSubtitle,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
