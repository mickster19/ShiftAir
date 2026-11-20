package com.shiftair.app.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import com.shiftair.app.model.Peer

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val listener: WifiDirectManager.Listener
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                listener.onWifiStateChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { deviceList: WifiP2pDeviceList ->
                    val peers = deviceList.deviceList.map {
                        Peer(deviceName = it.deviceName, deviceAddress = it.deviceAddress)
                    }
                    listener.onPeersChanged(peers)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                manager.requestConnectionInfo(channel) { info ->
                    val connected = info.groupFormed
                    val isOwner = info.isGroupOwner
                    val ownerAddress = info.groupOwnerAddress?.hostAddress
                    listener.onConnectionChanged(connected, isOwner, ownerAddress)
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Jina/anwani ya kifaa chetu limebadilika - hakuna kitendo cha lazima hapa
            }
        }
    }
}
