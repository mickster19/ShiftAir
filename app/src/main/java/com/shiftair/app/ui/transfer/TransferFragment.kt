package com.shiftair.app.ui.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.adapters.PeerAdapter
import com.shiftair.app.databinding.FragmentTransferBinding
import com.shiftair.app.wifidirect.TransferService
import com.shiftair.app.wifidirect.WifiDirectManager

class TransferFragment : Fragment(), WifiDirectManager.Listener {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var peerAdapter: PeerAdapter
    private var groupOwnerHost: String? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val host = groupOwnerHost
            if (host != null) {
                val intent = Intent(requireContext(), TransferService::class.java).apply {
                    action = TransferService.ACTION_SEND_FILE
                    putExtra(TransferService.EXTRA_FILE_URI, uri.toString())
                    putExtra(TransferService.EXTRA_HOST_ADDRESS, host)
                }
                requireContext().startService(intent)
            } else {
                Toast.makeText(requireContext(), "Unganisha simu mbili kwanza!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TransferService.BROADCAST_TRANSFER_PROGRESS) {
                val msg = intent.getStringExtra(TransferService.EXTRA_STATUS_MSG) ?: ""
                val progress = intent.getIntExtra(TransferService.EXTRA_PROGRESS, 0)
                binding.tvTransferLog.text = msg
                binding.progressTransfer.visibility = View.VISIBLE
                binding.progressTransfer.progress = progress
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiDirectManager = WifiDirectManager(requireContext()).apply {
            listener = this@TransferFragment
        }

        peerAdapter = PeerAdapter { device ->
            wifiDirectManager.connectToPeer(device)
        }

        binding.rvPeers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPeers.adapter = peerAdapter

        binding.btnDiscover.setOnClickListener {
            wifiDirectManager.discoverPeers()
        }

        binding.btnSendFile.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        binding.btnReceiveMode.setOnClickListener {
            val intent = Intent(requireContext(), TransferService::class.java).apply {
                action = TransferService.ACTION_START_SERVER
            }
            requireContext().startService(intent)
            binding.tvTransferLog.text = "Inasubiri faili kutoka kwa mtumaji..."
        }
    }

    override fun onResume() {
        super.onResume()
        wifiDirectManager.register()
        val filter = IntentFilter(TransferService.BROADCAST_TRANSFER_PROGRESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(progressReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        wifiDirectManager.unregister()
        try {
            requireContext().unregisterReceiver(progressReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onStatusChanged(status: String) {
        binding.tvConnectionStatus.text = status
    }

    override fun onPeersUpdated(peers: List<WifiP2pDevice>) {
        peerAdapter.submitList(peers)
    }

    override fun onConnected(info: WifiP2pInfo) {
        groupOwnerHost = info.groupOwnerAddress?.hostAddress
        binding.btnSendFile.isEnabled = true

        if (info.groupFormed && info.isGroupOwner) {
            binding.tvConnectionStatus.text = "Imeunganishwa (Group Owner / Mpokeaji)"
            val intent = Intent(requireContext(), TransferService::class.java).apply {
                action = TransferService.ACTION_START_SERVER
            }
            requireContext().startService(intent)
        } else if (info.groupFormed) {
            binding.tvConnectionStatus.text = "Imeunganishwa na Host: $groupOwnerHost"
        }
    }

    override fun onDisconnected() {
        groupOwnerHost = null
        binding.btnSendFile.isEnabled = false
        binding.tvConnectionStatus.text = "Muunganisho umekatika."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
