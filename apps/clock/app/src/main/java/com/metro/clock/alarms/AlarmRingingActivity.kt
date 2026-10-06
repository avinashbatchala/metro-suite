package com.metro.clock.alarms

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.metro.clock.R
import com.metro.clock.data.AlarmEntity
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

/** Full-screen alarm ringing surface (dismiss / snooze). */
class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0L) {
            finish()
            return
        }
        val repository = AlarmRepository(this)
        var alarm by mutableStateOf<AlarmEntity?>(null)
        var use24 by mutableStateOf(DateFormat.is24HourFormat(this))
        lifecycleScope.launch {
            alarm = repository.alarm(alarmId)
            alarm?.let { startRinging(it) }
        }
        handleAction(intent.action)
        setContent {
            MetroSystemTheme {
                val current = alarm
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .statusBarsPadding()
                        .metroNavBarPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MetroText(
                        text = stringResource(R.string.alarm_ringing).uppercase(),
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.secondaryText,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MetroText(
                        text = current?.let { AlarmFormat.time(it, use24) } ?: "",
                        style = MetroTextStyle.PageTitle,
                    )
                    if (!current?.label.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MetroText(text = current!!.label, style = MetroTextStyle.HubTitle)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    MetroText(
                        text = stringResource(
                            R.string.alarm_snooze_minutes,
                            current?.snoozeMinutes ?: 10,
                        ),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetroBorderButton(
                            text = stringResource(R.string.alarm_snooze_action),
                            onClick = { snooze() },
                        )
                        MetroBorderButton(
                            text = stringResource(R.string.alarm_dismiss),
                            onClick = { dismiss() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra(EXTRA_ALARM_ID, -1L).takeIf { it >= 0L }?.let { alarmId = it }
        handleAction(intent.action)
    }

    private fun handleAction(action: String?) {
        when (action) {
            ACTION_DISMISS -> dismiss()
            ACTION_SNOOZE -> snooze()
        }
    }

    private fun dismiss() {
        stopRinging()
        AlarmNotifications.cancelRinging(this, alarmId)
        val repository = AlarmRepository(this)
        lifecycleScope.launch { repository.dismissAlarm(alarmId) }
        finishAndRemoveTask()
    }

    private fun snooze() {
        stopRinging()
        AlarmNotifications.cancelRinging(this, alarmId)
        val repository = AlarmRepository(this)
        lifecycleScope.launch { repository.snoozeAlarm(alarmId) }
        finishAndRemoveTask()
    }

    private fun startRinging(alarm: AlarmEntity) {
        // Per-alarm override, else the suite's Metro ALARM sound, else the user's system alarm
        // default, else the platform default.
        val uri: Uri = alarm.soundUri?.let { Uri.parse(it) }
            ?: com.metro.system.MetroSoundContract.resolveUri(
                contentResolver,
                com.metro.system.MetroSoundRole.ALARM,
            )
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            runCatching { play() }
        }
        if (alarm.vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            runCatching {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0L, 800L, 800L), 0),
                )
            }
        }
    }

    private fun stopRinging() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val ACTION_DISMISS = "com.metro.clock.action.RING_DISMISS"
        const val ACTION_SNOOZE = "com.metro.clock.action.RING_SNOOZE"

        fun intent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmRingingActivity::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
