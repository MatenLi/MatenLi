package dev.rffrr.mateli.core

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.VpnService
import dev.rffrr.mateli.R
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionEvents
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LogIterator
import io.nekohasekai.libbox.OutboundGroupIterator
import io.nekohasekai.libbox.OutboundGroupItemIterator
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.SetupOptions
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import java.io.File

/**
 * Hosts the sing-box core inside the VPN service.
 * Start with [start], stop with [stop]. State is published in [CoreRuntime].
 */
class CoreService : VpnService() {

    private var server: CommandServer? = null
    private var client: CommandClient? = null
    private var platform: PlatformBridge? = null
    private var lastConfig: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCore(intent.getStringExtra(EXTRA_CONFIG).orEmpty())
            ACTION_STOP -> stopCore(force = false)
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        stopCore(force = true)
    }

    override fun onDestroy() {
        stopCore(force = true)
        super.onDestroy()
    }

    private fun startCore(config: String) {
        // A previous core that failed to close still holds its ports. Retry the close first.
        if (server != null) {
            stopCore(force = false)
            if (server != null) return
        }
        CoreRuntime.markConnecting()
        PlatformBridge.ensureChannel(this)
        startForeground(NOTIFICATION_ID, foregroundNotification())
        try {
            setupLibbox()
            lastConfig = config
            val bridge = PlatformBridge(this)
            platform = bridge
            // Keep the reference from the start so a failed start is still closed by stopCore.
            val commandServer = Libbox.newCommandServer(serverHandler, bridge)
            server = commandServer
            commandServer.start()
            commandServer.startOrReloadService(config, OverrideOptions())
            startClient()
            CoreRuntime.markConnected()
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            stopCore(force = true)
            CoreRuntime.markError(message)
        }
    }

    private fun setupLibbox() = ensureLibbox(this)

    private fun startClient() {
        val options = CommandClientOptions()
        options.addCommand(Libbox.CommandStatus)
        options.addCommand(Libbox.CommandLog)
        options.setStatusInterval(1_000_000_000L)
        val c = Libbox.newCommandClient(clientHandler, options)
        c.connect()
        client = c
    }

    /**
     * Stops the core. If the core cannot be closed and [force] is false, the server reference is kept
     * so the next start or stop retries the close, and the error is shown instead of a false Idle.
     */
    private fun stopCore(force: Boolean = true) {
        client?.let { runCatching { it.disconnect() } }
        client = null

        server?.let { s ->
            val failure = runCatching {
                s.closeService()
                s.close()
            }.exceptionOrNull()
            if (failure != null && !force) {
                CoreRuntime.markError(failure.message ?: failure.javaClass.simpleName)
                return
            }
        }
        server = null

        platform?.release()
        platform = null
        CoreRuntime.markIdle()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun foregroundNotification(): Notification =
        Notification.Builder(this, PlatformBridge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running))
            .setOngoing(true)
            .build()

    private val serverHandler = object : CommandServerHandler {
        override fun serviceReload() {
            server?.startOrReloadService(lastConfig, OverrideOptions())
        }

        override fun serviceStop() {
            stopCore()
        }

        override fun writeDebugMessage(message: String) {
            CoreRuntime.addLog(message)
        }

        override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()

        override fun setSystemProxyEnabled(enabled: Boolean) {}

        override fun connectSSHAgent(): Int {
            throw UnsupportedOperationException("SSH agent is not available in this build")
        }

        override fun triggerNativeCrash() {}
    }

    private val clientHandler = object : CommandClientHandler {
        override fun connected() {}

        override fun disconnected(message: String) {}

        override fun clearLogs() {
            CoreRuntime.clearLogs()
        }

        override fun initializeClashMode(modes: StringIterator, currentMode: String) {}

        override fun setDefaultLogLevel(level: Int) {}

        override fun updateClashMode(newMode: String) {}

        override fun writeConnectionEvents(events: ConnectionEvents) {}

        override fun writeGroups(groups: OutboundGroupIterator) {}

        override fun writeLogs(messageList: LogIterator) {
            while (messageList.hasNext()) {
                CoreRuntime.addLog(messageList.next().message)
            }
        }

        override fun writeOutbounds(outbounds: OutboundGroupItemIterator) {}

        override fun writeStatus(message: StatusMessage) {
            CoreRuntime.updateStats(
                TrafficStats(
                    uplink = message.uplink,
                    downlink = message.downlink,
                    uplinkTotal = message.uplinkTotal,
                    downlinkTotal = message.downlinkTotal,
                    connectionsIn = message.connectionsIn,
                    connectionsOut = message.connectionsOut
                )
            )
        }
    }

    companion object {
        const val ACTION_START = "dev.rffrr.mateli.action.START"
        const val ACTION_STOP = "dev.rffrr.mateli.action.STOP"
        const val EXTRA_CONFIG = "config"
        private const val NOTIFICATION_ID = 1

        @Volatile
        private var setupDone = false

        /** Sets up libbox once per process. Shared by the VPN core and the URL-test probe. */
        @Synchronized
        fun ensureLibbox(context: Context) {
            if (setupDone) return
            val base = File(context.filesDir, "sing-box").apply { mkdirs() }
            val work = File(base, "work").apply { mkdirs() }
            val options = SetupOptions()
            options.setBasePath(base.absolutePath)
            options.setWorkingPath(work.absolutePath)
            options.setTempPath(context.cacheDir.absolutePath)
            options.setCommandServerListenPort(Libbox.availablePort(19090))
            options.setCommandServerSecret(Libbox.randomHex(16).value)
            options.setLogMaxLines(500L)
            options.setDebug(false)
            options.setFixAndroidStack(true)
            Libbox.setup(options)
            setupDone = true
        }

        /** Starts the core with a complete sing-box config. Call only after VpnService.prepare returned null. */
        fun start(context: Context, config: String) {
            val intent = Intent(context, CoreService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CONFIG, config)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CoreService::class.java).setAction(ACTION_STOP))
        }
    }
}
