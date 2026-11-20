package com.shiftair.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shiftair.app.R
import com.shiftair.app.databinding.ItemFileBinding
import com.shiftair.app.model.FileEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val onClick: (FileEntry) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val entries = mutableListOf<FileEntry>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun submitList(newEntries: List<FileEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(entries[position], onClick, dateFormat)
    }

    override fun getItemCount() = entries.size

    class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: FileEntry, onClick: (FileEntry) -> Unit, dateFormat: SimpleDateFormat) {
            binding.tvFileName.text = entry.name
            binding.ivFileIcon.setImageResource(
                if (entry.isDirectory) R.drawable.ic_files else R.drawable.ic_transfer
            )
            binding.tvFileMeta.text = if (entry.isDirectory)
                "Folda"
            else
                String.format("%.1f KB • %s", entry.sizeBytes / 1024f, dateFormat.format(Date(entry.lastModified)))
            binding.root.setOnClickListener { onClick(entry) }
        }
    }
}
