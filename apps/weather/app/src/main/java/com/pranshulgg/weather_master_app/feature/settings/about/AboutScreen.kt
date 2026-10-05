package com.pranshulgg.weather_master_app.feature.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.weather_master_app.core.ui.components.NavigateUpBtn
import com.pranshulgg.weather_master_app.core.ui.components.SettingSection
import com.pranshulgg.weather_master_app.core.ui.components.SettingTile
import com.pranshulgg.weather_master_app.core.ui.components.SettingsTileIcon
import com.pranshulgg.weather_master_app.core.ui.components.Symbol
import com.pranshulgg.weather_master_app.core.ui.navigation.NavRoutes
import com.pranshulgg.weather_master_app.feature.shared.ui.SharedBottomSheet
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val viewModel: AboutScreenViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    val isLoadingNewVersion = viewModel.loading
    val uriHandler = LocalUriHandler.current

    var isChangelogSheetOpen by remember { mutableStateOf(false) }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden)
    )


    LargeTopBarScaffold(
        title = stringResource(R.string.setting_about_app),
        navigationIcon = { NavigateUpBtn(navController) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
        ) {
            AppVersionTile(
                description = "v${packageInfo.versionName} • ${packageInfo.versionCode}",
                isLoadingNewVersion = isLoadingNewVersion,
                onClick = {
                    scope.launch {
                        viewModel.isNewVersionAvailable(
                            "v${packageInfo.versionName}",
                            onAction = { uriHandler.openUri("https://github.com/PranshulGG/WeatherMaster/releases/latest") })
                    }
                }
            )
            Gap(10.dp)
            SettingSection(
                tiles = listOf(
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.article_24px) },
                        title = stringResource(R.string.about_terms_conditions),
                        onClick = { navController.navigate(NavRoutes.TERMS_CONDITIONS) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.policy_24px) },
                        title = stringResource(R.string.about_privacy_policy),
                        onClick = { navController.navigate(NavRoutes.PRIVACY_POLICY) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.license_24px) },
                        title = stringResource(R.string.about_license),
                        description = "GNU General Public License v3.0",
                        onClick = { navController.navigate(NavRoutes.LICENSE) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.bug_report_24px) },
                        title = stringResource(R.string.about_create_issue),
                        onClick = { uriHandler.openUri("https://github.com/PranshulGG/WeatherMaster/issues/new") }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.text_snippet_24px) },
                        title = stringResource(R.string.about_changelog),
                        onClick = {
                            isChangelogSheetOpen = true
                        }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.mail_24px) },
                        title = stringResource(R.string.about_email),
                        description = "pranshul.devmain@gmail.com",
                        onClick = { uriHandler.openUri("mailto:pranshul.devmain@gmail.com") }
                    ),

                    )
            )
        }
    }

    SharedBottomSheet.ChangelogBottomSheet(
        sheetState,
        onDismiss = { isChangelogSheetOpen = false },
        show = isChangelogSheetOpen
    )
}

@Composable
private fun AppVersionTile(
    description: String,
    onClick: () -> Unit,
    isLoadingNewVersion: Boolean = false
) {

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.app_icon_512px),
            contentDescription = null,
            Modifier.size(44.dp)
        )
        Gap(14.dp)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            MetroText(
                text = "MetroWeather",
                color = MetroTheme.colors.primaryText,
                style = MetroTextStyle.ListItemTitle
            )
            MetroText(
                text = description,
                color = MetroTheme.colors.secondaryText,
                style = MetroTextStyle.ListItemSubtitle
            )
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(enabled = !isLoadingNewVersion) { onClick() },
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Symbol(R.drawable.refresh_24px, color = MetroTheme.colors.primaryText)
        }
    }
}