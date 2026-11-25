package dev.rffrr.mateli.core

/** Connection states of the core. */
enum class CoreState { Idle, Connecting, Connected }

/** Formats a number of seconds as HH:MM:SS. */
fun formatUptime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
