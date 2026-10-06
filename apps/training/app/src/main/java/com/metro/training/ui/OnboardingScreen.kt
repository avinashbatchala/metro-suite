package com.metro.training.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.training.R
import com.metro.training.domain.exercises.LoadUnit
import com.metro.training.domain.plan.SplitPreset
import com.metro.training.domain.plan.TrainingGoal
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

/**
 * First-run onboarding / plan builder. Builds routines from the chosen weekly frequency, goal and
 * per-day muscle focus using [com.metro.training.domain.plan.RoutineTemplateGenerator].
 */
@Composable
fun OnboardingScreen(viewModel: TrainingViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.onboarding
    var selectedDay by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
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
            MetroAppTitle(title = "set up training")
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                StepHeader(state.step)

                when (state.step) {
                    0 -> UnitsAndGoalStep(viewModel)
                    1 -> EffortStep(viewModel)
                    2 -> DaysStep(viewModel)
                    else -> FocusStep(
                        viewModel = viewModel,
                        selectedDay = selectedDay,
                        onSelectDay = { selectedDay = it },
                    )
                }
            }
        }

        MetroAppBar(
            icons = buildList {
                if (state.step > 0) {
                    add(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Back,
                            label = "back",
                            onClick = viewModel::onboardingBack,
                        ),
                    )
                }
                add(
                    MetroAppBarIcon(
                        type = if (state.step < TrainingViewModel.ONBOARDING_STEPS - 1) {
                            MetroSystemIconType.Forward
                        } else {
                            MetroSystemIconType.Check
                        },
                        label = if (state.step < TrainingViewModel.ONBOARDING_STEPS - 1) "next" else "build",
                        onClick = {
                            if (state.step < TrainingViewModel.ONBOARDING_STEPS - 1) {
                                viewModel.onboardingNext()
                            } else {
                                viewModel.buildPlanFromOnboarding()
                            }
                        },
                    ),
                )
            },
            menuItems = listOf(
                MetroAppBarMenuItem(text = "skip", onClick = viewModel::skipOnboarding),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StepHeader(step: Int) {
    val title = when (step) {
        0 -> "units & goal"
        1 -> "effort logging"
        2 -> "training days"
        else -> "muscle focus"
    }
    MetroText(
        text = title,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun UnitsAndGoalStep(viewModel: TrainingViewModel) {
    val state = viewModel.onboarding
    Body("Choose how loads are shown and what your training emphasises. You can change both later.")
    MetroListPicker(
        options = listOf("kilograms (kg)", "pounds (lb)"),
        selectedOptionIndex = if (state.unit == LoadUnit.KG) 0 else 1,
        onSelectOption = { viewModel.onboardingSetUnit(if (it == 0) LoadUnit.KG else LoadUnit.LB) },
        label = "units",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    MetroListPicker(
        options = TrainingGoal.values().map { it.label },
        selectedOptionIndex = TrainingGoal.values().indexOf(state.goal),
        onSelectOption = { viewModel.onboardingSetGoal(TrainingGoal.values()[it]) },
        label = "goal",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    Body(
        when (state.goal) {
            TrainingGoal.STRENGTH -> "Compound lifts first, lower reps, slightly more conservative effort."
            TrainingGoal.HYPERTROPHY -> "Compounds plus isolations, moderate reps, a range of exercises."
            TrainingGoal.GENERAL -> "A balanced mix of compounds and isolations."
        },
    )
}

@Composable
private fun EffortStep(viewModel: TrainingViewModel) {
    val state = viewModel.onboarding
    Body(
        "RIR is how many good reps you think you could still perform when a set ends. " +
            "It helps progression decide when to add load.",
    )
    MetroListPicker(
        options = listOf("on (recommended)", "off"),
        selectedOptionIndex = if (state.rirEnabled) 0 else 1,
        onSelectOption = { viewModel.onboardingSetRir(it == 0) },
        label = "log reps in reserve",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

@Composable
private fun DaysStep(viewModel: TrainingViewModel) {
    val state = viewModel.onboarding
    Body("How many days a week will you train? We'll suggest a split you can adjust.")
    MetroListPicker(
        options = (1..6).map { "$it ${if (it == 1) "day" else "days"}" },
        selectedOptionIndex = state.days - 1,
        onSelectOption = { viewModel.onboardingSetDays(it + 1) },
        label = "training days",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    MetroListPicker(
        options = SplitPreset.values().map { it.label },
        selectedOptionIndex = SplitPreset.values().indexOf(state.preset),
        onSelectOption = { viewModel.onboardingSetPreset(SplitPreset.values()[it]) },
        label = "split",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    Body("This builds ${state.focus.size} routine(s): " + state.focus.joinToString(", ") { it.name } + ".")
}

@Composable
private fun FocusStep(
    viewModel: TrainingViewModel,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
) {
    val state = viewModel.onboarding
    val safeDay = selectedDay.coerceIn(0, (state.focus.size - 1).coerceAtLeast(0))
    Body("Fine-tune which muscle groups each day targets.")
    MetroListPicker(
        options = state.focus.map { it.name },
        selectedOptionIndex = safeDay,
        onSelectOption = onSelectDay,
        label = "day",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    val day = state.focus.getOrNull(safeDay)
    if (day != null) {
        MuscleMultiSelect(
            selected = day.muscles,
            onChange = { muscles -> viewModel.onboardingReplaceMuscles(safeDay, muscles) },
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun Body(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.ListItemSubtitle,
        color = MetroTheme.colors.secondaryText,
    )
}
