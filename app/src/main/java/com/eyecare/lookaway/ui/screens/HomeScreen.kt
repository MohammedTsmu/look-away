package com.eyecare.lookaway.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.lookaway.R
import com.eyecare.lookaway.service.Phase
import com.eyecare.lookaway.service.ReminderService
import com.eyecare.lookaway.ui.AppViewModel
import com.eyecare.lookaway.ui.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    permissions: PermissionStatus,
    onOpenSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenIntent: (android.content.Intent) -> Unit,
) {
    val state by viewModel.engineState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!permissions.allEssentialGranted || !permissions.overlay || !permissions.batteryUnrestricted) {
                Spacer(Modifier.height(8.dp))
                PermissionCard(
                    permissions = permissions,
                    onRequestNotifications = onRequestNotifications,
                    onOpenIntent = onOpenIntent,
                )
            }

            Spacer(Modifier.height(24.dp))

            val statusLabel = when {
                !state.isRunning -> stringResource(R.string.home_state_idle)
                state.paused -> stringResource(R.string.home_state_paused)
                else -> stringResource(R.string.home_state_running)
            }

            StatusRing(
                progress = state.progress,
                centerTop = statusLabel,
                centerBig = if (state.isRunning) {
                    ReminderService.formatClock(state.secondsRemaining)
                } else "20·20·20",
                centerBottom = if (state.isRunning && state.phase == Phase.WORKING) {
                    stringResource(R.string.home_next_break)
                } else if (state.phase == Phase.BREAK) {
                    stringResource(R.string.break_headline)
                } else "",
            )

            Spacer(Modifier.height(28.dp))

            // Primary control: Start / Stop.
            if (!state.isRunning) {
                Button(
                    onClick = { viewModel.start() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_start), fontSize = 16.sp)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (state.paused) R.string.home_resume else R.string.home_pause,
                            ),
                        )
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_stop))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.previewBreak() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Visibility, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_test_break))
            }

            Spacer(Modifier.height(20.dp))

            // Today's tally + the current rule summary.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = state.breaksTaken.toString(),
                    label = stringResource(R.string.home_cycles_today),
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${settings.workMinutes}m / ${settings.breakSeconds}s",
                    label = stringResource(R.string.settings_work_interval),
                )
            }
        }
    }
}

@Composable
private fun StatusRing(
    progress: Float,
    centerTop: String,
    centerBig: String,
    centerBottom: String,
) {
    val animated by animateFloatAsState(progress, tween(400), label = "home-ring")
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f, sweepAngle = 360f * animated.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerTop,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                centerBig,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimatedVisibility(visible = centerBottom.isNotEmpty()) {
                Text(
                    centerBottom,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    permissions: PermissionStatus,
    onRequestNotifications: () -> Unit,
    onOpenIntent: (android.content.Intent) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.perm_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.perm_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))

            if (!permissions.notifications) {
                PermissionRow(
                    label = stringResource(R.string.perm_notif),
                    desc = stringResource(R.string.perm_notif_desc),
                    onGrant = onRequestNotifications,
                )
            }
            if (!permissions.exactAlarm) {
                PermissionRow(
                    label = stringResource(R.string.perm_alarm),
                    desc = stringResource(R.string.perm_alarm_desc),
                    onGrant = { onOpenIntent(com.eyecare.lookaway.ui.Permissions.exactAlarmIntent(context)) },
                )
            }
            if (!permissions.overlay) {
                PermissionRow(
                    label = stringResource(R.string.perm_overlay),
                    desc = stringResource(R.string.perm_overlay_desc),
                    onGrant = { onOpenIntent(com.eyecare.lookaway.ui.Permissions.overlayIntent(context)) },
                )
            }
            if (!permissions.batteryUnrestricted) {
                PermissionRow(
                    label = stringResource(R.string.perm_battery),
                    desc = stringResource(R.string.perm_battery_desc),
                    onGrant = { onOpenIntent(com.eyecare.lookaway.ui.Permissions.batteryIntent(context)) },
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, desc: String, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onGrant, shape = RoundedCornerShape(12.dp)) {
            Text(stringResource(R.string.perm_grant))
        }
    }
}
