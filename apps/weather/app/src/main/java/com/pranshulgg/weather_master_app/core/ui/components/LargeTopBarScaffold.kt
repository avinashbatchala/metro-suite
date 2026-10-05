package com.pranshulgg.weather_master_app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

/**
 * Windows 10 Mobile page shell, a drop-in replacement for the old Material large top bar:
 * a small uppercase `METROWEATHER` label, a big lowercase page title, a square back
 * chevron and flat black canvas. No Material top-app-bar, tonal surfaces or collapsing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LargeTopBarScaffold(
    title: String,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    bottomBar: @Composable (() -> Unit) = {},
    defaultCollapsed: Boolean = false,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    navigationIcon()
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            MetroAppTitle(title = "METROWEATHER")
            if (title.isNotBlank()) {
                MetroPageHeader(title = title.lowercase())
            } else {
                Spacer(modifier = Modifier.padding(bottom = 6.dp))
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content(PaddingValues(0.dp))
        }

        bottomBar()
    }
}
