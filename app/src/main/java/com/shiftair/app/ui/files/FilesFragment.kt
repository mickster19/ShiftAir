package com.shiftair.app.ui.files

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.adapters.FileAdapter
import com.shiftair.app.databinding.FragmentFilesBinding
import com.shiftair.app.model.FileItemModel
import java.io.File

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FileAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FileAdapter { item ->
            if (item.isDirectory) {
                loadDirectory(item.file)
            } else {
                openFile(item.file)
            }
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter

        binding.btnUpFolder.setOnClickListener {
            val parent = currentDir.parentFile
            if (parent != null && parent.canRead()) {
                loadDirectory(parent)
            } else {
                Toast.makeText(requireContext(), "Uko kwenye folda kuu", Toast.LENGTH_SHORT).show()
            }
        }

        loadDirectory(currentDir)
    }

    private fun loadDirectory(dir: File) {
        currentDir = dir
        binding.tvCurrentPath.text = dir.absolutePath

        val rawFiles = dir.listFiles()?.toList().orEmpty()
        val sorted = rawFiles
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                FileItemModel(
                    file = file,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length()
                )
            }
        adapter.submitList(sorted)
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Fungua faili na"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Hakuna app ya kufungua faili hii", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
