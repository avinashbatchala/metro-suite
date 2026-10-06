package com.pranshulgg.weather_master_app.feature.settings.about.terms

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pranshulgg.weather_master_app.R
import com.pranshulgg.weather_master_app.core.ui.components.Gap
import com.pranshulgg.weather_master_app.core.ui.components.WeatherPageHeader
import com.pranshulgg.weather_master_app.core.ui.theme.weatherMasterTitleFont

@Composable
fun TermsConditionsScreen() {


    Column(modifier = Modifier.fillMaxSize()) {
        WeatherPageHeader(title = stringResource(R.string.about_terms_conditions))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp)
        ) {
            MetroText(
                text = buildAnnotatedString {
                    append("These Terms & Conditions apply to the ")

                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontFamily = weatherMasterTitleFont
                        )
                    ) {
                        append("WeatherMaster")
                    }

                    append(" app (the \"Application\") for mobile devices.\n")

                    append("This app was created by ")

                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("Pranshul")
                    }

                    append(" as an open-source. By using the Application, you agree to the following:")
                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Use of the Application",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("- The Application is provided ")

                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("as-is")
                    }

                    append(" ,free of charge, and without any guarantees of reliability, availability, or accuracy.")

                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = "- You may use, modify, and distribute the Application in accordance with its open-source license",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("- You may ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("not")
                    }
                    append(" misrepresent the origin of the Application or use its name/trademarks without permission.")
                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Data & Privacy",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("- The Application does ")

                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("not collect, store, or share")
                    }

                    append(" any personal information.")

                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("- The only permission requested is ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("location access")
                    }
                    append(" ,which is optional and used solely within the Application to provide weather information. This data never leaves your device.")
                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = "- For more details, please see the Privacy Policy.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Liability",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("- The Service Provider (Pranshul) is ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append("not liable")
                    }
                    append(" for any direct or indirect damages, losses, or issues that may arise from using the Application.")
                },
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = "- You are responsible for ensuring your device is compatible and has sufficient internet and battery to use the Application.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Updates & Availability",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = "- The Application may be updated from time to time.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = "- There is no guarantee that the Application will always remain available, functional, or supported on all operating system versions.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            Gap(6.dp)
            MetroText(
                text = "- The Service Provider may discontinue the Application at any time without prior notice.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Changes to These Terms",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = "These Terms & Conditions may be updated in the future. Updates will be posted in the project repository or within the Application. Continued use of the Application means you accept any revised terms.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText
            )
            MetroDivider()
            MetroText(
                "Contact",
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent
            )
            Gap(8.dp)
            MetroText(
                text = buildAnnotatedString {
                    append("If you have any questions about these Terms & Conditions, please contact ")
                    withLink(
                        LinkAnnotation.Url(
                            "mailto:pranshul.devmain@gmail.com"
                        )
                    ) {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                color = MetroTheme.colors.accent
                            ),

                            ) {
                            append("pranshul.devmain@gmail.com")
                        }
                    }
                    append(" for any direct or indirect damages, losses, or issues that may arise from using the Application.")
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
