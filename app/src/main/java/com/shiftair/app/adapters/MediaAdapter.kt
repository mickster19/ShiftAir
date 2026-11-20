package com.shiftair.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shiftair.app.R
import com.shiftair.app.databinding.ItemMediaBinding
import com.shiftair.app.model.MediaItem
import java.util.concurrent.TimeUnit

class MediaAdapter(
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val items = mutableListOf<MediaItem>()

    fun submitList(newItems: List<MediaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount() = items.size

    class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem, onClick: (MediaItem) -> Unit) {
            binding.tvMediaTitle.text = item.title
            val minutes = TimeUnit.MILLISECONDS.toMinutes(item.durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(item.durationMs) % 60
            val sizeMb = item.sizeBytes / (1024f * 1024f)
            binding.tvMediaSubtitle.text = if (item.durationMs > 0)
                String.format("%02d:%02d  •  %.1f MB", minutes, seconds, sizeMb)
            else
                String.format("%.1f MB", sizeMb)
            binding.ivIcon.setImageResource(R.drawable.ic_video)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
