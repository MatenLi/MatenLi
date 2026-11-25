package dev.rffrr.mateli.core

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.SystemProxyStatus
import java.io.File

/**
 * Runs a probe config in its own process, so URL tests never touch the VPN core. It is a VpnService
 * only so its outbound sockets can be protected from the VPN. It never opens a TUN.
 * After the core starts, it writes "ok" or "error: ..." to [STATUS_FILE] in the cache folder.
 */
class ProbeService : VpnService() {

    private var server: CommandServer? = null
    private var platform: PlatformBridge? = null
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = Runnable { stopProbe() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProbe(intent.getStringExtra(EXTRA_CONFIG).orEmpty())
            ACTION_STOP -> stopProbe()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopProbe()
        super.onDestroy()
    }

    private fun startProbe(config: String) {
        val status = File(cacheDir, STATUS_FILE)
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_MS)
        try {
            CoreService.ensureLibbox(this)
            val bridge = PlatformBridge(this)
            platform = bridge
            val commandServer = Libbox.newCommandServer(serverHandler, bridge)
            server = commandServer
            commandServer.start()
            commandServer.startOrReloadService(config, OverrideOptions())
            status.writeText("ok")
        } catch (e: Exception) {
            status.writeText("error: " + (e.message ?: e.javaClass.simpleName))
            stopProbe()
        }
    }

    private fun stopProbe() {
        handler.removeCallbacks(watchdog)
        server?.let { s -> runCatching { s.closeService() ; s.close() } }
        server = null
        platform?.release()
        platform = null
        stopSelf()
    }

    private val serverHandler = object : CommandServerHandler {
        override fun serviceReload() {}

        override fun serviceStop() {
            stopProbe()
        }

        override fun writeDebugMessage(message: String) {}

        override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()

        override fun setSystemProxyEnabled(enabled: Boolean) {}

        override fun connectSSHAgent(): Int {
            throw UnsupportedOperationException("SSH agent is not used by URL tests")
        }

        override fun triggerNativeCrash() {}
    }

    companion object {
        const val STATUS_FILE = "probe-status.txt"
        private const val ACTION_START = "dev.rffrr.mateli.action.PROBE_START"
        private const val ACTION_STOP = "dev.rffrr.mateli.action.PROBE_STOP"
        private const val EXTRA_CONFIG = "config"
        private const val WATCHDOG_MS = 120_000L

        fun start(context: Context, config: String) {
            context.startService(
                Intent(context, ProbeService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_CONFIG, config)
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ProbeService::class.java).setAction(ACTION_STOP))
        }
    }
}
