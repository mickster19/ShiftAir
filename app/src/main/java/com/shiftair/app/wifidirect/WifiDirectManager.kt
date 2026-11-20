package com.shiftair.app.wifidirect

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import com.shiftair.app.model.Peer

/**
 * Wrapper juu ya WifiP2pManager (Wi-Fi Direct) ya Android.
 * Hii ndiyo inayowezesha ShiftAir kutafuta na kuunganisha na vifaa
 * jirani BILA kutumia intaneti au data ya simu - moja kwa moja (peer-to-peer).
 */
class WifiDirectManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onPeersChanged(peers: List<Peer>)
        fun onConnectionChanged(connected: Boolean, isGroupOwner: Boolean, groupOwnerAddress: String?)
        fun onWifiStateChanged(enabled: Boolean)
    }

    private val manager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WiFiDirectBroadcastReceiver? = null

    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    fun start() {
        channel = manager.initialize(context, context.mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager, channel!!, listener)
        context.registerReceiver(receiver, intentFilter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // tayari haijasajiliwa, hakuna tatizo
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onFailure: (String) -> Unit = {}) {
        val c = channel ?: return
        manager.discoverPeers(c, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { /* peers zitakuja kupitia broadcast receiver */ }
            override fun onFailure(reason: Int) = onFailure(reasonToText(reason))
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, onFailure: (String) -> Unit = {}) {
        val c = channel ?: return
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            wps.setup = android.net.wifi.WpsInfo.PBC
        }
        manager.connect(c, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { /* connection state itaje kupitia broadcast */ }
            override fun onFailure(reason: Int) = onFailure(reasonToText(reason))
        })
    }

    fun disconnect() {
        val c = channel ?: return
        manager.removeGroup(c, null)
    }

    private fun reasonToText(reason: Int): String = when (reason) {
        WifiP2pManager.ERROR -> "Hitilafu ya ndani"
        WifiP2pManager.P2P_UNSUPPORTED -> "Kifaa hiki hakina Wi-Fi Direct"
        WifiP2pManager.BUSY -> "Mfumo una shughuli, jaribu tena"
        else -> "Hitilafu isiyojulikana ($reason)"
    }
}
