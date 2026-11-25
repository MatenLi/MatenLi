package dev.rffrr.mateli.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.IpPrefix
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.RoutePrefix
import io.nekohasekai.libbox.RoutePrefixIterator
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterface as LibboxInterface
import io.nekohasekai.libbox.Notification as LibboxNotification
import java.net.InetAddress

/**
 * What the sing-box core asks the Android side for: the TUN device, the
 * interface list, the default network and notifications.
 * Features that do not apply to a phone VPN throw UnsupportedOperationException.
 */
class PlatformBridge(private val service: VpnService) : PlatformInterface {

    private val connectivity: ConnectivityManager? =
        service.getSystemService(ConnectivityManager::class.java)
    private val notifications: NotificationManager? =
        service.getSystemService(NotificationManager::class.java)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Stops the default-network watcher. Called when the core stops. */
    fun release() {
        networkCallback?.let { callback ->
            runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
    }

    override fun autoDetectInterfaceControl(fd: Int) {
        if (!service.protect(fd)) {
            throw IllegalStateException("Could not protect socket $fd from the VPN")
        }
    }

    override fun openTun(options: TunOptions): Int {
        val builder = service.Builder()
            .setSession("MatenLi")
            .setMtu(options.getMTU())

        options.getInet4Address().toList().forEach { builder.addAddress(it.address(), it.prefix()) }
        options.getInet6Address().toList().forEach { builder.addAddress(it.address(), it.prefix()) }

        val routes4 = options.getInet4RouteAddress().toList()
            .ifEmpty { options.getInet4RouteRange().toList() }
        routes4.forEach { builder.addRoute(it.address(), it.prefix()) }

        val routes6 = options.getInet6RouteAddress().toList()
            .ifEmpty { options.getInet6RouteRange().toList() }
        routes6.forEach { builder.addRoute(it.address(), it.prefix()) }

        if (Build.VERSION.SDK_INT >= 33) {
            options.getInet4RouteExcludeAddress().toList().forEach {
                builder.excludeRoute(IpPrefix(InetAddress.getByName(it.address()), it.prefix()))
            }
            options.getInet6RouteExcludeAddress().toList().forEach {
                builder.excludeRoute(IpPrefix(InetAddress.getByName(it.address()), it.prefix()))
            }
        }

        options.getDNSServerAddress().toList().forEach { builder.addDnsServer(it) }

        val descriptor = builder.establish()
            ?: throw IllegalStateException("VPN permission was not granted")
        return descriptor.detachFd()
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val list = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .map { nif -> toLibboxInterface(nif) }
        return ListInterfaceIterator(list)
    }

    private fun toLibboxInterface(nif: java.net.NetworkInterface): LibboxInterface {
        val addresses = nif.interfaceAddresses.map { address ->
            "${address.address.hostAddress.substringBefore('%')}/${address.networkPrefixLength.toInt()}"
        }
        var flags = 1 // Go net.FlagUp
        if (nif.isPointToPoint) flags = flags or 8
        if (nif.supportsMulticast()) flags = flags or 16

        val type = when {
            nif.name.startsWith("wlan") -> Libbox.InterfaceTypeWIFI
            nif.name.startsWith("rmnet") || nif.name.startsWith("ccmni") -> Libbox.InterfaceTypeCellular
            nif.name.startsWith("eth") -> Libbox.InterfaceTypeEthernet
            else -> Libbox.InterfaceTypeOther
        }

        val item = LibboxInterface()
        item.setIndex(nif.index)
        item.setMTU(nif.mtu)
        item.setName(nif.name)
        item.setAddresses(ListStringIterator(addresses))
        item.setFlags(flags)
        item.setType(type)
        item.setDNSServer(ListStringIterator(emptyList()))
        item.setGateway(ListStringIterator(emptyList()))
        item.setMetered(false)
        return item
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        val cm = connectivity ?: throw IllegalStateException("ConnectivityManager is not available")
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                reportDefault(cm, network, capabilities, listener)
            }
        }
        networkCallback = callback
        cm.registerDefaultNetworkCallback(callback)
        cm.activeNetwork?.let { network ->
            cm.getNetworkCapabilities(network)?.let { caps ->
                reportDefault(cm, network, caps, listener)
            }
        }
    }

    private fun reportDefault(
        cm: ConnectivityManager,
        network: Network,
        capabilities: NetworkCapabilities,
        listener: InterfaceUpdateListener
    ) {
        val name = cm.getLinkProperties(network)?.interfaceName ?: return
        val index = runCatching { java.net.NetworkInterface.getByName(name)?.index }.getOrNull() ?: 0
        val expensive = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        listener.updateDefaultInterface(name, index, expensive, false)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        release()
    }

    override fun sendNotification(notification: LibboxNotification) {
        val nm = notifications ?: return
        ensureChannel(service)
        val built = Notification.Builder(service, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .build()
        nm.notify(notification.identifier.hashCode(), built)
    }

    override fun cancelNotification(identifier: String, typeId: Int) {
        notifications?.cancel(identifier.hashCode())
    }

    override fun checkPlatformShell() {}

    override fun clearDNSCache() {}

    override fun closeNeighborMonitor(listener: NeighborUpdateListener) {}

    override fun startNeighborMonitor(listener: NeighborUpdateListener) {}

    override fun createBridge(options: BridgeOptions): BridgeSession {
        throw unsupported("Bridge sessions")
    }

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int
    ): ConnectionOwner {
        throw unsupported("Connection owner lookup")
    }

    override fun includeAllNetworks(): Boolean = false

    override fun localDNSTransport(): LocalDNSTransport? = null

    override fun lookupSFTPServer(): String {
        throw unsupported("SFTP")
    }

    override fun lookupUser(name: String): PlatformUser {
        throw unsupported("User lookup")
    }

    override fun openShellSession(
        user: PlatformUser,
        command: String,
        args: StringIterator,
        workingDirectory: String,
        width: Int,
        height: Int
    ): ShellSession {
        throw unsupported("Shell sessions")
    }

    override fun readSystemSSHHostKey(): String {
        throw unsupported("SSH host keys")
    }

    override fun readWIFIState(): WIFIState? = null

    override fun registerMyInterface(name: String) {}

    override fun tailscaleHostname(): String = ""

    override fun underNetworkExtension(): Boolean = false

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun usePlatformBridge(): Boolean = false

    override fun usePlatformShell(): Boolean = false

    private fun unsupported(feature: String) =
        UnsupportedOperationException("$feature is not available in this build")

    companion object {
        const val CHANNEL_ID = "matenli_core"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "MatenLi core", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}

private fun StringIterator.toList(): List<String> = buildList {
    while (hasNext()) add(next())
}

private fun RoutePrefixIterator.toList(): List<RoutePrefix> = buildList {
    while (hasNext()) add(next())
}

private class ListStringIterator(private val items: List<String>) : StringIterator {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun len(): Int = items.size
    override fun next(): String = items[index++]
}

private class ListInterfaceIterator(private val items: List<LibboxInterface>) : NetworkInterfaceIterator {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): LibboxInterface = items[index++]
}
