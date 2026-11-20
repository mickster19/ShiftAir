package com.shiftair.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shiftair.app.databinding.ItemPeerBinding
import com.shiftair.app.model.Peer

class PeerAdapter(
    private val onClick: (Peer) -> Unit
) : RecyclerView.Adapter<PeerAdapter.PeerViewHolder>() {

    private val peers = mutableListOf<Peer>()

    fun submitList(newPeers: List<Peer>) {
        peers.clear()
        peers.addAll(newPeers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val binding = ItemPeerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PeerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(peers[position], onClick)
    }

    override fun getItemCount(): Int = peers.size

    class PeerViewHolder(private val binding: ItemPeerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(peer: Peer, onClick: (Peer) -> Unit) {
            binding.tvPeerName.text = peer.deviceName
            binding.tvPeerAddress.text = peer.deviceAddress
            binding.root.setOnClickListener { onClick(peer) }
        }
    }
}
