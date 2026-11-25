package dev.rffrr.mateli.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Traffic counters reported by the sing-box core. Speeds are bytes per second. */
data class TrafficStats(
    val uplink: Long = 0L,
    val downlink: Long = 0L,
    val uplinkTotal: Long = 0L,
    val downlinkTotal: Long = 0L,
    val connectionsIn: Int = 0,
    val connectionsOut: Int = 0
)

/** Shared state between the VPN service and the UI. */
object CoreRuntime {
    private const val MAX_LOG_LINES = 500

    private val stateFlow = MutableStateFlow(CoreState.Idle)
    val state: StateFlow<CoreState> = stateFlow.asStateFlow()

    private val statsFlow = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = statsFlow.asStateFlow()

    private val logFlow = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = logFlow.asStateFlow()

    private val errorFlow = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = errorFlow.asStateFlow()

    private val startedFlow = MutableStateFlow(0L)
    val startedAt: StateFlow<Long> = startedFlow.asStateFlow()

    fun markConnecting() {
        errorFlow.value = null
        stateFlow.value = CoreState.Connecting
    }

    fun markConnected() {
        startedFlow.value = System.currentTimeMillis()
        stateFlow.value = CoreState.Connected
    }

    fun markIdle() {
        stateFlow.value = CoreState.Idle
        statsFlow.value = TrafficStats()
        startedFlow.value = 0L
    }

    fun markError(message: String) {
        errorFlow.value = message
        markIdle()
    }

    fun updateStats(value: TrafficStats) {
        statsFlow.value = value
    }

    @Synchronized
    fun addLog(line: String) {
        logFlow.value = (logFlow.value + line).takeLast(MAX_LOG_LINES)
    }

    fun clearLogs() {
        logFlow.value = emptyList()
    }
}
