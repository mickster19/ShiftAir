package com.shiftair.app.adapters

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shiftair.app.R
import com.shiftair.app.databinding.ItemRowBinding
import com.shiftair.app.model.FileItemModel
import com.shiftair.app.model.MediaItemModel
import com.shiftair.app.util.MediaStoreHelper

class PeerAdapter(
    private val onPeerClick: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<PeerAdapter.PeerViewHolder>() {

    private val peers = mutableListOf<WifiP2pDevice>()

    fun submitList(newPeers: List<WifiP2pDevice>) {
        peers.clear()
        peers.addAll(newPeers)
        notifyDataSetChanged()
    }

    inner class PeerViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PeerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val device = peers[position]
        holder.binding.ivIcon.setImageResource(R.drawable.ic_device)
        holder.binding.tvTitle.text = device.deviceName.ifBlank { "Kifaa kisichojulikana" }
        holder.binding.tvSubtitle.text = "Anwani: ${device.deviceAddress} • Bofya kuunganisha"
        holder.binding.root.setOnClickListener { onPeerClick(device) }
    }

    override fun getItemCount(): Int = peers.size
}

class MediaAdapter(
    private val isVideo: Boolean,
    private val onItemClick: (MediaItemModel) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val items = mutableListOf<MediaItemModel>()

    fun submitList(newItems: List<MediaItemModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class MediaViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = items[position]
        holder.binding.ivIcon.setImageResource(if (isVideo) R.drawable.ic_video else R.drawable.ic_audio)
        holder.binding.tvTitle.text = item.title
        holder.binding.tvSubtitle.text = item.subtitle
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

class FileAdapter(
    private val onFileClick: (FileItemModel) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val files = mutableListOf<FileItemModel>()

    fun submitList(newFiles: List<FileItemModel>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    inner class FileViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = files[position]
        holder.binding.ivIcon.setImageResource(if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_file)
        holder.binding.tvTitle.text = item.name
        holder.binding.tvSubtitle.text = if (item.isDirectory) "Folda" else MediaStoreHelper.formatSize(item.sizeBytes)
        holder.binding.root.setOnClickListener { onFileClick(item) }
    }

    override fun getItemCount(): Int = files.size
}
