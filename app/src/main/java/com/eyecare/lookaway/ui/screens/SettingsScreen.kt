package com.eyecare.lookaway.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.lookaway.R
import com.eyecare.lookaway.data.ThemeMode
import com.eyecare.lookaway.ui.AppViewModel
import com.eyecare.lookaway.ui.theme.Accents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenIntent: (android.content.Intent) -> Unit = {},
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
            }
            viewModel.setSoundUri(uri?.toString() ?: "")
        }
    }

    fun openSoundPicker() {
        val current = s.soundUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }
        runCatching { soundPicker.launch(intent) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ---- Timing ----
            Section(stringResource(R.string.settings_timing)) {
                SliderRow(
                    label = stringResource(R.string.settings_work_interval),
                    valueText = "${s.workMinutes} min",
                    value = s.workMinutes.toFloat(),
                    range = 1f..60f,
                    steps = 58,
                    onChange = { viewModel.setWorkMinutes(it.toInt()) },
                )
                SliderRow(
                    label = stringResource(R.string.settings_break_length),
                    valueText = "${s.breakSeconds} sec",
                    value = s.breakSeconds.toFloat(),
                    range = 5f..120f,
                    steps = 22,
                    onChange = { viewModel.setBreakSeconds(roundTo5(it)) },
                )
            }

            // ---- Behavior ----
            Section(stringResource(R.string.settings_behavior)) {
                SwitchRow(
                    title = stringResource(R.string.settings_fullscreen),
                    subtitle = stringResource(R.string.settings_fullscreen_desc),
                    checked = s.fullScreenBreak,
                    onChange = viewModel::setFullScreen,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_strict),
                    subtitle = stringResource(R.string.settings_strict_desc),
                    checked = s.strictMode,
                    onChange = viewModel::setStrict,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_screen_off),
                    subtitle = stringResource(R.string.settings_screen_off_desc),
                    checked = s.pauseWhenScreenOff,
                    onChange = viewModel::setPauseWhenScreenOff,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_pause_media),
                    subtitle = stringResource(R.string.settings_pause_media_desc),
                    checked = s.pauseMediaOnBreak,
                    onChange = { on ->
                        viewModel.setPauseMedia(on)
                        // Sending the user to grant access the first time they enable it.
                        if (on && !com.eyecare.lookaway.ui.Permissions.hasMediaAccess(context)) {
                            onOpenIntent(com.eyecare.lookaway.ui.Permissions.notificationListenerSettingsIntent())
                        }
                    },
                )
            }

            // ---- Feedback ----
            Section(stringResource(R.string.settings_feedback)) {
                SwitchRow(
                    title = stringResource(R.string.settings_sound),
                    subtitle = null,
                    checked = s.sound,
                    onChange = viewModel::setSound,
                )
                if (s.sound) {
                    ValueRow(
                        label = stringResource(R.string.settings_sound_pick),
                        value = soundName(context, s.soundUri),
                        onClick = { openSoundPicker() },
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.settings_vibrate),
                    subtitle = null,
                    checked = s.vibrate,
                    onChange = viewModel::setVibrate,
                )
            }

            // ---- Startup ----
            Section(stringResource(R.string.settings_startup)) {
                SwitchRow(
                    title = stringResource(R.string.settings_boot),
                    subtitle = stringResource(R.string.settings_boot_desc),
                    checked = s.startOnBoot,
                    onChange = viewModel::setStartOnBoot,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_autostart),
                    subtitle = null,
                    checked = s.startOnAppOpen,
                    onChange = viewModel::setStartOnOpen,
                )
            }

            // ---- Quiet hours ----
            Section(stringResource(R.string.settings_quiet)) {
                SwitchRow(
                    title = stringResource(R.string.settings_quiet_enable),
                    subtitle = stringResource(R.string.settings_quiet_desc),
                    checked = s.quietHoursEnabled,
                    onChange = viewModel::setQuietEnabled,
                )
                if (s.quietHoursEnabled) {
                    TimeRow(
                        label = stringResource(R.string.settings_quiet_from),
                        minutes = s.quietStartMinutes,
                        onPicked = viewModel::setQuietStart,
                    )
                    TimeRow(
                        label = stringResource(R.string.settings_quiet_to),
                        minutes = s.quietEndMinutes,
                        onPicked = viewModel::setQuietEnd,
                    )
                }
            }

            // ---- When turned off ----
            Section(stringResource(R.string.settings_when_off)) {
                SwitchRow(
                    title = stringResource(R.string.settings_remind_off),
                    subtitle = stringResource(R.string.settings_remind_off_desc),
                    checked = s.remindWhenOff,
                    onChange = viewModel::setRemindWhenOff,
                )
                if (s.remindWhenOff) {
                    SliderRow(
                        label = stringResource(R.string.settings_remind_off_delay),
                        valueText = stringResource(R.string.hours_short, s.remindWhenOffHours),
                        value = s.remindWhenOffHours.toFloat(),
                        range = 1f..48f,
                        steps = 46,
                        onChange = { viewModel.setRemindWhenOffHours(it.toInt()) },
                    )
                }
            }

            // ---- Mindful usage ----
            Section(stringResource(R.string.settings_mindful)) {
                SwitchRow(
                    title = stringResource(R.string.settings_mindful_enable),
                    subtitle = stringResource(R.string.settings_mindful_desc),
                    checked = s.mindfulUsageEnabled,
                    onChange = { on ->
                        viewModel.setMindfulEnabled(on)
                        if (on && !com.eyecare.lookaway.usage.UsageTracker.hasAccess(context)) {
                            onOpenIntent(com.eyecare.lookaway.ui.Permissions.usageAccessIntent())
                        }
                    },
                )
                if (s.mindfulUsageEnabled) {
                    SliderRow(
                        label = stringResource(R.string.settings_mindful_after),
                        valueText = durationLabel(s.mindfulUsageThresholdMin),
                        value = s.mindfulUsageThresholdMin.toFloat(),
                        range = 30f..480f,
                        steps = 29,
                        onChange = { viewModel.setMindfulThreshold((Math.round(it / 15f) * 15)) },
                    )
                    SliderRow(
                        label = stringResource(R.string.settings_mindful_repeat),
                        valueText = durationLabel(s.mindfulUsageRepeatMin),
                        value = s.mindfulUsageRepeatMin.toFloat(),
                        range = 10f..120f,
                        steps = 10,
                        onChange = { viewModel.setMindfulRepeat((Math.round(it / 10f) * 10)) },
                    )
                }
            }

            // ---- Appearance ----
            Section(stringResource(R.string.settings_appearance)) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                val currentLang = com.eyecare.lookaway.util.LocaleManager.getLanguage(context)
                val applyLang: (String) -> Unit = { lang ->
                    if (lang != currentLang) {
                        com.eyecare.lookaway.util.LocaleManager.setLanguage(context, lang)
                        // Unwrap to the hosting Activity (context may be a wrapper).
                        var c: android.content.Context? = context
                        while (c is android.content.ContextWrapper && c !is android.app.Activity) {
                            c = c.baseContext
                        }
                        (c as? android.app.Activity)?.recreate()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(
                        stringResource(R.string.lang_system),
                        currentLang == com.eyecare.lookaway.util.LocaleManager.SYSTEM,
                    ) { applyLang(com.eyecare.lookaway.util.LocaleManager.SYSTEM) }
                    ThemeChip(stringResource(R.string.lang_english), currentLang == "en") { applyLang("en") }
                    ThemeChip(stringResource(R.string.lang_arabic), currentLang == "ar") { applyLang("ar") }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(stringResource(R.string.theme_system), s.themeMode == ThemeMode.SYSTEM) {
                        viewModel.setTheme(ThemeMode.SYSTEM)
                    }
                    ThemeChip(stringResource(R.string.theme_light), s.themeMode == ThemeMode.LIGHT) {
                        viewModel.setTheme(ThemeMode.LIGHT)
                    }
                    ThemeChip(stringResource(R.string.theme_dark), s.themeMode == ThemeMode.DARK) {
                        viewModel.setTheme(ThemeMode.DARK)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_accent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accents.forEachIndexed { index, accent ->
                        AccentDot(
                            color = accent.seed,
                            selected = s.accentIndex == index,
                            onClick = { viewModel.setAccent(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onPicked: (Int) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TimePickerDialog(
                    context,
                    { _, h, m -> onPicked(h * 60 + m) },
                    minutes / 60,
                    minutes % 60,
                    android.text.format.DateFormat.is24HourFormat(context),
                ).show()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            formatMinutes(minutes),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** Human-readable name of the chosen break sound (or "Default"). */
private fun soundName(context: android.content.Context, soundUri: String): String {
    if (soundUri.isBlank()) return context.getString(R.string.sound_default)
    return runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context)
    }.getOrNull() ?: context.getString(R.string.sound_default)
}

@Composable
private fun AccentDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

private fun roundTo5(v: Float): Int = (Math.round(v / 5f) * 5).coerceAtLeast(5)

private fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
