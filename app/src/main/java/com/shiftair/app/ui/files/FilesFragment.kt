package com.shiftair.app.ui.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.adapters.FileAdapter
import com.shiftair.app.databinding.FragmentFilesBinding
import com.shiftair.app.model.FileEntry
import java.io.File

/**
 * Tab ya "Files": kivinjari rahisi cha faili za ndani ya simu -
 * unaweza kuvinjari folda, kufungua faili, na kuzitazama kwa
 * app nyingine zilizosakinishwa (kwa kutumia Intent ya kawaida).
 */
class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FileAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FileAdapter { entry -> onEntryClicked(entry) }
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        loadDirectory(currentDir)
    }

    private fun loadDirectory(dir: File) {
        currentDir = dir
        binding.tvCurrentPath.text = dir.absolutePath

        val entries = mutableListOf<FileEntry>()
        if (dir.parentFile != null) {
            entries.add(FileEntry("..", dir.parent ?: "/", true, 0, 0))
        }
        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { f ->
            entries.add(
                FileEntry(
                    name = f.name,
                    path = f.absolutePath,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isFile) f.length() else 0,
                    lastModified = f.lastModified()
                )
            )
        }
        adapter.submitList(entries)
    }

    private fun onEntryClicked(entry: FileEntry) {
        val file = File(entry.path)
        if (entry.name == "..") {
            loadDirectory(file)
            return
        }
        if (entry.isDirectory) {
            loadDirectory(file)
        } else {
            openFile(file)
        }
    }

    private fun openFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val mime = requireContext().contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Hakuna app ya kufungua aina hii ya faili - tunaipuuza kimya kimya
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
