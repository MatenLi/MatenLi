package dev.rffrr.mateli.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.core.CoreState
import dev.rffrr.mateli.core.ExitInfo
import dev.rffrr.mateli.core.TrafficStats
import dev.rffrr.mateli.core.formatSpeed
import dev.rffrr.mateli.profiles.Profile

/**
 * Bottom bar. Left: exit IP and country, then the latency with a lock for the selected profile.
 * Right: upload and download speeds. The connect button sits in the middle, overlapping the top edge.
 */
@Composable
fun ConfigurationBottomBar(
    state: CoreState,
    stats: TrafficStats,
    latency: Long?,
    exit: ExitInfo?,
    selected: Profile?,
    onToggle: () -> Unit
) {
    val statusText = stringResource(
        when (state) {
            CoreState.Idle -> R.string.status_disconnected
            CoreState.Connecting -> R.string.status_connecting
            CoreState.Connected -> R.string.status_connected
        }
    )
    val exitText = when {
        state != CoreState.Connected -> statusText
        exit != null -> {
            val parts = listOf(flagOf(exit.countryCode), exit.ip, exit.countryCode).filter { it.isNotBlank() }
            val place = if (exit.region.isNotBlank()) ", ${exit.region}" else ""
            parts.joinToString(" ") + place
        }
        else -> stringResource(R.string.exit_loading)
    }
    val tls = selected?.outbound?.optJSONObject("tls")
    val unlocked = tls?.optBoolean("insecure", false) == true
    val latencyText = if (latency == null) {
        stringResource(R.string.latency_none)
    } else {
        stringResource(R.string.latency_format, latency)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 44.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exitText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tls != null) {
                            Icon(
                                imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = stringResource(
                                    if (unlocked) R.string.lock_insecure else R.string.lock_secure
                                ),
                                modifier = Modifier.size(14.dp),
                                tint = if (unlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = latencyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(96.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    SpeedLine(Icons.Filled.ArrowUpward, formatSpeed(stats.uplink))
                    SpeedLine(Icons.Filled.ArrowDownward, formatSpeed(stats.downlink))
                }
            }
        }
        ConnectFab(
            state = state,
            onClick = onToggle,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SpeedLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Regional-indicator flag emoji for a two-letter country code, or empty when the code is unusable. */
private fun flagOf(code: String): String {
    if (code.length != 2 || !code.all { it in 'A'..'Z' || it in 'a'..'z' }) return ""
    return code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

/** Connect button with a pulse ring and a progress ring while connecting. */
@Composable
fun ConnectFab(state: CoreState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (targetContainer, targetContent) = MaterialTheme.colorScheme.accentFor(state)
    val container by animateColorAsState(targetContainer, tween(500), label = "fabContainer")
    val labelColor by animateColorAsState(targetContent, tween(500), label = "fabContent")
    val transition = rememberInfiniteTransition(label = "fab")
    val pulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val actionLabel = stringResource(
        if (state == CoreState.Idle) R.string.action_connect else R.string.action_disconnect
    )

    Box(
        modifier = modifier
            .size(88.dp)
            .drawBehind {
                if (state != CoreState.Idle) {
                    drawCircle(
                        color = container.copy(alpha = (1f - pulse) * 0.5f),
                        radius = size.minDimension / 2f * (0.6f + 0.4f * pulse),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (state == CoreState.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                color = container,
                strokeWidth = 3.dp
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = container,
            contentColor = labelColor
        ) {
            Icon(Icons.Filled.PowerSettingsNew, contentDescription = actionLabel)
        }
    }
}

/** Idle uses primary, connecting uses tertiary, connected uses secondary. */
private fun ColorScheme.accentFor(state: CoreState): Pair<Color, Color> = when (state) {
    CoreState.Idle -> primary to onPrimary
    CoreState.Connecting -> tertiary to onTertiary
    CoreState.Connected -> secondary to onSecondary
}
