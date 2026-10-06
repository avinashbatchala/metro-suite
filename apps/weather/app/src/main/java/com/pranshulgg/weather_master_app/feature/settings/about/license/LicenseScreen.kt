package com.pranshulgg.weather_master_app.feature.settings.about.license

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader

@Composable
fun LicenseScreen() {

    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.about_license))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp)
        ) {

            MetroText(
                text = "GNU General Public License v3.0",
                style = MetroTextStyle.DialogTitle,
                color = MetroTheme.colors.accent
            )

            Gap(8.dp)

            MetroText(
                text =
                    "This application is licensed under the GNU General Public License Version 3 (GPLv3).\n\n" +
                            "You are free to use, study, modify, and distribute this software under the terms of the GPLv3 license.\n\n" +
                            "Any modified versions or redistributed copies must also remain open-source and include the same license.\n\n" +
                            "This software is provided \"as is\", without warranty of any kind, including merchantability or fitness for a particular purpose.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )

            MetroDivider()

            MetroText(
                text = "Your Rights",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )

            Gap(6.dp)

            MetroText(
                text =
                    "• Use the software for any purpose\n" +
                            "• Access and modify the source code\n" +
                            "• Share copies of the software\n" +
                            "• Distribute modified versions under GPLv3",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )

            MetroDivider()

            MetroText(
                text = "Conditions",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )

            Gap(6.dp)

            MetroText(
                text =
                    "• You must include the GPLv3 license when redistributing\n" +
                            "• Modified versions must clearly state changes made\n" +
                            "• Derivative works must also be licensed under GPLv3\n" +
                            "• Source code must remain available",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )

            MetroDivider()

            MetroText(
                text = "Full License",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )

            Gap(6.dp)

            MetroText(
                text = buildAnnotatedString {
                    append("You can read the full GNU GPLv3 license at: ")
                    withLink(
                        LinkAnnotation.Url("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                color = MetroTheme.colors.accent
                            ),

                            ) {
                            append("https://www.gnu.org/licenses/gpl-3.0.en.html")
                        }
                    }
                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )

            Gap(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 30.dp)

        }
    }
}
@Composable
private fun MetroDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MetroTheme.colors.secondaryText.copy(alpha = 0.4f))
    )
}
