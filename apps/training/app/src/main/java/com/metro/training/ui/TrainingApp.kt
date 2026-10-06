package com.metro.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.training.R
import com.metro.training.domain.workout.Workout
import com.metro.training.domain.workout.WorkoutStatus
import com.metro.training.ui.components.MetroBar
import com.metro.training.ui.components.MetroBarChart
import com.metro.training.ui.components.MetroLineChart
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroHubTitleMode
import com.metro.ui.MetroHubTitleRow
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrainingApp(viewModel: TrainingViewModel, modifier: Modifier = Modifier) {
    if (viewModel.showOnboarding) {
        OnboardingScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.pivot) {
        val target = when (viewModel.pivot) {
            TrainingPivot.Today -> 0
            TrainingPivot.Routines -> 1
            TrainingPivot.History -> 2
            TrainingPivot.Progress -> 3
        }
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage) { viewModel.setPivot(pagerState.currentPage) }

    MetroSubpageHost(
        route = viewModel.route,
        isRoot = { it == TrainingRoute.Main },
        parentOf = { TrainingRoute.Main },
        loadKeyOf = { it.name },
        onGoBack = viewModel::closeSubpage,
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
        rootContent = {
            RootContent(
                viewModel = viewModel,
                pagerState = pagerState,
                onSelectPage = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            )
        },
        subpageContent = { route ->
            val exit = LocalMetroSubpageExit.current
            when (route) {
                TrainingRoute.RoutineDetail -> RoutineDetailScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.ExercisePicker -> ExercisePickerScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.CustomExercise -> CustomExerciseScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.ActiveWorkout -> ActiveWorkoutScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.WorkoutSummary -> WorkoutSummaryScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.ExerciseDetail -> ExerciseDetailScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.Settings -> SettingsScreen(viewModel, onBack = { exit?.invoke() })
                TrainingRoute.Main -> Unit
            }
        },
    )

    viewModel.selectedRecommendation?.let { recommendation ->
        RecommendationDialog(
            viewModel = viewModel,
            view = recommendation,
            onDismiss = viewModel::closeRecommendation,
        )
    }

    viewModel.status?.let { message ->
        com.metro.ui.MetroMessageDialog(
            title = message,
            confirmLabel = "ok",
            onConfirm = viewModel::dismissStatus,
            onDismissRequest = viewModel::dismissStatus,
        )
    }
}

@Composable
private fun RootContent(
    viewModel: TrainingViewModel,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onSelectPage: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle(title = stringResource(R.string.app_name))
            MetroHubTitleRow(
                titles = listOf("today", "routines", "history", "progress"),
                selectedIndex = pagerState.currentPage,
                mode = MetroHubTitleMode.Pivot,
                onTitleClick = onSelectPage,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                beyondViewportPageCount = 1,
            ) { index ->
                when (index) {
                    0 -> TodayPane(viewModel)
                    1 -> RoutinesPane(viewModel)
                    2 -> HistoryPane(viewModel)
                    else -> ProgressPane(viewModel)
                }
            }
        }
        RootAppBar(viewModel, pagerState.currentPage)
    }
}

@Composable
private fun BoxScope.RootAppBar(viewModel: TrainingViewModel, page: Int) {
    val icons = when (page) {
        0 -> listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.Play,
                label = "start",
                onClick = {
                    viewModel.activeWorkout?.let { viewModel.resumeWorkout() }
                        ?: viewModel.routines.firstOrNull()?.let { viewModel.startWorkout(it) }
                },
            ),
        )
        1 -> listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.Add,
                label = "new",
                onClick = viewModel::newRoutine,
            ),
        )
        else -> emptyList()
    }
    MetroAppBar(
        icons = icons,
        menuItems = listOf(
            MetroAppBarMenuItem("settings", onClick = viewModel::openSettings),
        ),
        enterKey = page,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

// ---- panes ---------------------------------------------------------------

@Composable
private fun TodayPane(viewModel: TrainingViewModel) {
    val active = viewModel.activeWorkout
    val nextRoutine = viewModel.routines.firstOrNull()
    Column(modifier = Modifier.fillMaxSize()) {
        if (active != null) {
            SectionLabel("workout in progress")
            MetroText(
                text = active.routineName,
                style = MetroTextStyle.PivotTab,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            MetroText(
                text = "${active.exercises.size} exercises · ${active.completedExposureCount} logged",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            MetroText(
                text = "resume",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .metroClickable { viewModel.resumeWorkout() },
            )
        } else if (nextRoutine != null) {
            SectionLabel("today")
            MetroText(
                text = nextRoutine.name,
                style = MetroTextStyle.PivotTab,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            MetroText(
                text = "${nextRoutine.exercises.size} exercises · ${nextRoutine.workSetTotal} work sets",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            MetroText(
                text = "start workout",
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .metroClickable { viewModel.startWorkout(nextRoutine) },
            )
        } else {
            MetroEmptyState(
                message = "Create a routine to begin training.",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RoutinesPane(viewModel: TrainingViewModel) {
    if (viewModel.routines.isEmpty()) {
        MetroEmptyState(message = "No routines yet. Tap + to create one.", modifier = Modifier.fillMaxSize())
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(viewModel.routines, key = { it.id }) { routine ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable { viewModel.openRoutine(routine.id) }
                    .padding(vertical = 14.dp),
            ) {
                MetroText(text = routine.name, style = MetroTextStyle.ListItemTitle)
                MetroText(
                    text = "${routine.exercises.size} exercises · ${routine.workSetTotal} work sets",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
            Divider()
        }
    }
}

@Composable
private fun HistoryPane(viewModel: TrainingViewModel) {
    val workouts = viewModel.historySummaries
    if (workouts.isEmpty()) {
        MetroEmptyState(message = "No workouts yet.", modifier = Modifier.fillMaxSize())
        return
    }
    val monthFormat = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(workouts, key = { it.id }) { workout ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable { viewModel.openHistoryWorkout(workout.id) }
                    .padding(vertical = 14.dp),
            ) {
                MetroText(
                    text = monthFormat.format(Date(workout.startedAt)),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.secondaryText,
                )
                MetroText(text = workout.routineName.ifBlank { "Workout" }, style = MetroTextStyle.ListItemTitle)
                MetroText(
                    text = "${dayFormat.format(Date(workout.startedAt))} · " +
                        "${formatDuration(workout.durationMillis)} · ${workout.workSetCount} sets",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
            Divider()
        }
    }
}

@Composable
private fun ProgressPane(viewModel: TrainingViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item { SectionLabel("estimated weekly sets") }
        if (viewModel.weeklyMuscleSets.isEmpty()) {
            item {
                MetroText(
                    text = "No working sets logged this week.",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            item {
                MetroBarChart(
                    bars = viewModel.weeklyMuscleSets.map { (muscle, sets) ->
                        MetroBar(
                            label = muscle.name.lowercase().replace('_', ' '),
                            value = sets,
                            comparison = viewModel.lastWeekMuscleSets[muscle],
                        )
                    },
                    valueText = { formatSets(it) },
                )
            }
        }

        item { SectionLabel("weekly volume") }
        item {
            MetroLineChart(
                points = viewModel.weeklyVolumePoints,
                valueText = { viewModel.formatWeight(it) },
            )
        }

        item { SectionLabel("exercise progress") }
        items(viewModel.exercises, key = { it.id }) { exercise ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroClickable { viewModel.openExerciseProgress(exercise) }
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

// ---- small helpers -------------------------------------------------------

@Composable
internal fun SectionLabel(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.15f)),
    )
}

internal fun formatDuration(millis: Long): String {
    val minutes = (millis / 60000).coerceAtLeast(0)
    return "$minutes min"
}

internal fun formatSets(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} sets" else "${value} sets"

internal fun exerciseSubtitle(exercise: com.metro.training.domain.exercises.ExerciseDefinition): String {
    val mechanic = exercise.mechanic.name.lowercase()
    val equipment = exercise.equipment.name.lowercase().replace('_', ' ')
    val increment = exercise.defaultIncrement?.let { " · ${it} kg step" } ?: ""
    return "$mechanic · $equipment$increment"
}
