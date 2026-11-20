package com.shiftair.app.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.adapters.PeerAdapter
import com.shiftair.app.databinding.FragmentTransferBinding
import com.shiftair.app.model.Peer
import com.shiftair.app.wifidirect.TransferHelper
import com.shiftair.app.wifidirect.TransferService
import com.shiftair.app.wifidirect.WifiDirectManager

/**
 * Tab ya "Transfer": inatafuta vifaa jirani kupitia Wi-Fi Direct
 * na kuruhusu kutuma/kupokea faili bila intaneti.
 */
class TransferFragment : Fragment(), WifiDirectManager.Listener {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var peerAdapter: PeerAdapter
    private var groupOwnerAddress: String? = null
    private var pendingFileUriToSend: android.net.Uri? = null

    private val pickFileLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingFileUriToSend = uri
            binding.tvStatus.text = "Chagua kifaa cha kutuma faili..."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiDirectManager = WifiDirectManager(requireContext(), this)

        peerAdapter = PeerAdapter { peer -> onPeerSelected(peer) }
        binding.rvPeers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPeers.adapter = peerAdapter

        binding.btnSend.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        binding.btnReceive.setOnClickListener {
            binding.tvStatus.text = "Inasubiri faili kutoka kwa kifaa kingine..."
            TransferHelper.startReceive(requireContext())
        }

        TransferService.progressListener = { sent, total ->
            activity?.runOnUiThread {
                binding.progressTransfer.visibility = View.VISIBLE
                if (total > 0) {
                    binding.progressTransfer.progress = ((sent * 100) / total).toInt()
                }
            }
        }
        TransferService.completionListener = { success, path ->
            activity?.runOnUiThread {
                binding.progressTransfer.visibility = View.GONE
                binding.tvStatus.text = if (success)
                    "Imekamilika: $path"
                else
                    "Uhamisho umeshindikana"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wifiDirectManager.start()
        wifiDirectManager.discoverPeers { error ->
            activity?.runOnUiThread { binding.tvStatus.text = error }
        }
    }

    override fun onPause() {
        super.onPause()
        wifiDirectManager.stop()
    }

    private fun onPeerSelected(peer: Peer) {
        wifiDirectManager.connect(peer.deviceAddress) { error ->
            activity?.runOnUiThread { binding.tvStatus.text = error }
        }
    }

    override fun onPeersChanged(peers: List<Peer>) {
        activity?.runOnUiThread {
            peerAdapter.submitList(peers)
            binding.tvStatus.text = if (peers.isEmpty())
                getString(com.shiftair.app.R.string.no_devices)
            else
                "Vifaa vilivyopatikana: ${peers.size}"
        }
    }

    override fun onConnectionChanged(connected: Boolean, isGroupOwner: Boolean, groupOwnerAddress: String?) {
        this.groupOwnerAddress = groupOwnerAddress
        activity?.runOnUiThread {
            if (connected) {
                binding.tvStatus.text = "Imeunganishwa ✔"
                val uri = pendingFileUriToSend
                if (uri != null && !isGroupOwner && groupOwnerAddress != null) {
                    TransferHelper.startSend(requireContext(), uri, groupOwnerAddress)
                    pendingFileUriToSend = null
                }
            }
        }
    }

    override fun onWifiStateChanged(enabled: Boolean) {
        if (!enabled) {
            activity?.runOnUiThread {
                binding.tvStatus.text = "Wi-Fi imezimwa - washa Wi-Fi kutumia Transfer"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        TransferService.progressListener = null
        TransferService.completionListener = null
        _binding = null
    }
}
