package com.shiftair.app.wifidirect

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build

class WifiDirectManager(private val context: Context) {

    interface Listener {
        fun onStatusChanged(status: String)
        fun onPeersUpdated(peers: List<WifiP2pDevice>)
        fun onConnected(info: WifiP2pInfo)
        fun onDisconnected()
    }

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? =
        manager?.initialize(context, context.mainLooper, null)
    private var receiver: WiFiDirectBroadcastReceiver? = null
    var listener: Listener? = null
    var connectionInfo: WifiP2pInfo? = null
        private set

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    fun register() {
        if (receiver == null) {
            receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, intentFilter)
            }
        }
    }

    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {
            }
            receiver = null
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        listener?.onStatusChanged("Inatafuta vifaa jirani kwa Wi-Fi Direct...")
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                listener?.onStatusChanged("Utafutaji umeanza. Subiri vifaa vionekane...")
            }

            override fun onFailure(reason: Int) {
                listener?.onStatusChanged("Imeshindwa kutafuta vifaa (Code: $reason). Hakikisha Wi-Fi na Location zimewashwa.")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        listener?.onStatusChanged("Inaunganisha na ${device.deviceName}...")
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                listener?.onStatusChanged("Ombi la muunganisho limetumwa kwa ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                listener?.onStatusChanged("Imeshindwa kuunganisha (Code: $reason)")
            }
        })
    }

    fun onWifiStateChanged(enabled: Boolean) {
        if (!enabled) {
            listener?.onStatusChanged("Tahadhari: Tafadhali washa Wi-Fi kwenye simu yako.")
        }
    }

    fun onPeersAvailable(peers: List<WifiP2pDevice>) {
        listener?.onPeersUpdated(peers)
        if (peers.isEmpty()) {
            listener?.onStatusChanged("Hakuna kifaa kilichopatikana bado. Hakikisha simu ya pili imefungua ShiftAir.")
        } else {
            listener?.onStatusChanged("Vifaa ${peers.size} vimepatikana.")
        }
    }

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        connectionInfo = info
        listener?.onConnected(info)
    }

    fun onDisconnected() {
        connectionInfo = null
        listener?.onDisconnected()
    }
}
