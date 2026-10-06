package com.metro.training.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.training.domain.exercises.MuscleGroup
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

/** Shared multi-select list of muscle groups (single column of WP8.1 checkboxes). */
@Composable
internal fun MuscleMultiSelect(
    selected: Set<MuscleGroup>,
    modifier: Modifier = Modifier,
    onChange: (Set<MuscleGroup>) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MuscleGroup.values().forEach { muscle ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCheckBox(
                    checked = muscle in selected,
                    onCheckedChange = {
                        onChange(if (muscle in selected) selected - muscle else selected + muscle)
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetroText(
                    text = muscle.name.lowercase().replace('_', ' '),
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
