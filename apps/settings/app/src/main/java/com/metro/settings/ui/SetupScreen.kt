package com.metro.settings.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * "Set up Windows Phone" — a guided, mostly one-time onboarding that grants the shell its
 * system-level capabilities and applies the immersive takeover, without the user hunting
 * through Android Settings. Everything here is either a deep link to the relevant Android
 * setting, or a secure-settings write that requires `WRITE_SECURE_SETTINGS` (granted by
 * `scripts/provision.sh`).
 */
@Composable
fun SetupScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    SettingsDetailScaffold(pageTitle = "set up windows phone", modifier = modifier) {
        SettingsBodyText(
            "Make this phone behave like Windows Phone. Tap each step to grant it, or run " +
                "scripts/provision.sh over adb to apply everything at once.",
        )

        SetupStepRow(
            title = "default home",
            granted = isDefaultHome(context),
            onOpen = { context.openSettingsAction(Settings.ACTION_HOME_SETTINGS) },
        )
        SetupStepRow(
            title = "keyboard",
            granted = isKeyboardSelected(context),
            onOpen = { context.openSettingsAction(Settings.ACTION_INPUT_METHOD_SETTINGS) },
        )
        SetupStepRow(
            title = "draw over other apps",
            granted = hasAppOp(context, "com.metro.statusbar", AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW),
            onOpen = { context.openAppOverlaySettings("com.metro.statusbar") },
        )
        SetupStepRow(
            title = "accessibility",
            granted = hasMetroAccessibility(context),
            onOpen = { context.openSettingsAction(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
        )
        SetupStepRow(
            title = "notification access",
            granted = hasMetroListener(context),
            onOpen = { context.openSettingsAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS") },
        )
        SetupStepRow(
            title = "battery exemption",
            granted = isBatteryExempt(context, "com.metro.statusbar"),
            onOpen = { context.requestIgnoreBattery("com.metro.statusbar") },
        )
        SetupStepRow(
            title = "lock screen over keyguard",
            granted = hasAppOp(context, "com.metro.lockscreen", "android:use_full_screen_intent"),
            onOpen = { context.openFullScreenIntentSettings("com.metro.lockscreen") },
        )

        SettingsSpacer(height = 16)
        SettingsFieldLabel(text = "immersive mode")

        val current = readPolicyControl(context)
        val selectedIndex = when {
            current.contains("immersive.navigation") -> 2
            current.contains("immersive.status") -> 1
            else -> 0
        }
        MetroListPicker(
            options = listOf("off", "status bar", "status + navigation"),
            selectedOptionIndex = selectedIndex,
            onSelectOption = { index ->
                val value = when (index) {
                    1 -> "immersive.status=*"
                    2 -> "immersive.status=*:immersive.navigation=*"
                    else -> ""
                }
                writePolicyControl(context, value)
            },
            label = "hide android system bars",
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(
            "Hides Android's status bar so the Metro tray is the chrome (navigation stays " +
                "gesture-based). Needs WRITE_SECURE_SETTINGS — applied by provision.sh.",
        )

        SettingsSpacer(height = 24)
        MetroBorderButton(
            text = "restore android defaults",
            onClick = { restoreAndroidDefaults(context) },
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText("For a full restore (listeners, accessibility, home, keyboard) run scripts/provision.sh --restore.")
    }
}

@Composable
private fun SetupStepRow(title: String, granted: Boolean, onOpen: () -> Unit) {
    MetroListItem(
        title = title,
        subtitle = if (granted) "granted" else "tap to set up",
        trailing = {
            MetroText(
                text = if (granted) "done" else "set",
                style = MetroTextStyle.ListItemSubtitle,
                color = if (granted) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
            )
        },
        onClick = onOpen,
    )
}

// --- status checks ----------------------------------------------------------

private fun isDefaultHome(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    return resolved == "com.metro.launcher"
}

private fun isKeyboardSelected(context: Context): Boolean {
    val current = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD,
    ) ?: return false
    return current.startsWith("com.metro.keyboard/")
}

private fun hasAppOp(context: Context, pkg: String, op: String): Boolean = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val uid = context.packageManager.getApplicationInfo(pkg, 0).uid
    appOps.checkOpNoThrow(op, uid, pkg) == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

private fun hasMetroAccessibility(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.contains("com.metro.")
}

private fun hasMetroListener(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    return enabled.contains("com.metro.")
}

private fun isBatteryExempt(context: Context, pkg: String): Boolean = runCatching {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    pm.isIgnoringBatteryOptimizations(pkg)
}.getOrDefault(false)

// --- immersive (policy_control) ---------------------------------------------

private fun readPolicyControl(context: Context): String =
    Settings.Global.getString(context.contentResolver, "policy_control").orEmpty()

private fun writePolicyControl(context: Context, value: String) {
    runCatching {
        if (value.isBlank()) {
            Settings.Global.putString(context.contentResolver, "policy_control", "null")
        } else {
            Settings.Global.putString(context.contentResolver, "policy_control", value)
        }
    }
}

private fun restoreAndroidDefaults(context: Context) {
    writePolicyControl(context, "")
    // Remove the suite's accessibility services (keep any non-suite ones).
    runCatching {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return@runCatching
        val remaining = enabled.split(':').filterNot { it.contains("com.metro.") }.joinToString(":")
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            remaining,
        )
    }
}

// --- intent helpers ---------------------------------------------------------

private fun Context.openSettingsAction(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun Context.openAppOverlaySettings(pkg: String) {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.requestIgnoreBattery(pkg: String) {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.openFullScreenIntentSettings(pkg: String) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            openAppOverlaySettings(pkg)
        }
    }
}
