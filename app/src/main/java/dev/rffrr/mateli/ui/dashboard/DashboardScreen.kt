package dev.rffrr.mateli.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.core.CoreState
import dev.rffrr.mateli.core.Probes
import dev.rffrr.mateli.core.TrafficStats
import dev.rffrr.mateli.core.formatBytes
import dev.rffrr.mateli.core.formatSpeed
import dev.rffrr.mateli.core.formatUptime
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.ui.theme.MatenLiShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val URL_TEST = "http://cp.cloudflare.com/"

/**
 * Live dashboard. Speeds, totals and uptime are real while connected and "--" otherwise.
 * The URL test and TCP ping only run while connected, so they measure the active path.
 */
@Composable
fun DashboardScreen(
    state: CoreState,
    stats: TrafficStats,
    startedAt: Long,
    profile: Profile?,
    modifier: Modifier = Modifier
) {
    val connected = state == CoreState.Connected
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var urlMs by remember { mutableStateOf<Long?>(null) }
    var pingMs by remember { mutableStateOf<Long?>(null) }
    var tested by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(connected, startedAt) {
        while (connected && startedAt > 0L) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    LaunchedEffect(connected) {
        if (!connected) {
            urlMs = null
            pingMs = null
            tested = false
        }
    }

    val context = LocalContext.current
    val emptyText = stringResource(R.string.value_empty)
    val testingText = stringResource(R.string.dash_testing)
    val failedText = stringResource(R.string.dash_failed)
    val statusText = stringResource(
        when (state) {
            CoreState.Idle -> R.string.status_disconnected
            CoreState.Connecting -> R.string.status_connecting
            CoreState.Connected -> R.string.status_connected
        }
    )
    val uptime = if (connected && startedAt > 0L) {
        formatUptime(((now - startedAt) / 1000L).toInt().coerceAtLeast(0))
    } else {
        emptyText
    }
    val profileLine = if (profile != null) {
        stringResource(R.string.dash_selected, profile.name)
    } else {
        stringResource(R.string.dash_no_profile)
    }

    fun speed(bytesPerSecond: Long): String = if (connected) formatSpeed(bytesPerSecond) else emptyText
    fun total(bytes: Long): String = if (connected) formatBytes(bytes) else emptyText
    fun testResult(ms: Long?): String = when {
        testing -> testingText
        !tested -> emptyText
        ms == null -> failedText
        else -> context.getString(R.string.dash_ms, ms)
    }

    fun runTests() {
        testing = true
        scope.launch {
            urlMs = Probes.urlTest(URL_TEST)
            pingMs = profile?.let { p ->
                val host = p.outbound.optString("server")
                val port = p.outbound.optInt("server_port")
                if (host.isNotBlank() && port > 0) Probes.tcpPing(host, port) else null
            }
            testing = false
            tested = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MatenLiShapes.card,
            colors = CardDefaults.cardColors(
                containerColor = if (connected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stringResource(R.string.dash_uptime)}: $uptime",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = profileLine,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = stringResource(R.string.dash_upload),
                value = speed(stats.uplink),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.dash_download),
                value = speed(stats.downlink),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = stringResource(R.string.dash_total_upload),
                value = total(stats.uplinkTotal),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.dash_total_download),
                value = total(stats.downlinkTotal),
                modifier = Modifier.weight(1f)
            )
        }

        MetricCard(
            label = stringResource(R.string.dash_connections),
            value = if (connected) "${stats.connectionsIn} / ${stats.connectionsOut}" else emptyText,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MatenLiShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                DetailRow(stringResource(R.string.stat_url_test), testResult(urlMs))
                DetailRow(stringResource(R.string.stat_tcp_ping), testResult(pingMs))
                Button(
                    onClick = { runTests() },
                    enabled = connected && !testing,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        stringResource(
                            if (testing) R.string.dash_testing else R.string.dash_run_tests
                        )
                    )
                }
                if (!connected) {
                    Text(
                        text = stringResource(R.string.dash_not_connected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MatenLiShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
