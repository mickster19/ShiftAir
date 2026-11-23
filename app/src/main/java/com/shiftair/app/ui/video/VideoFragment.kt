package com.shiftair.app.ui.video

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.adapters.MediaAdapter
import com.shiftair.app.databinding.FragmentVideoBinding
import com.shiftair.app.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MediaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MediaAdapter(isVideo = true) { video ->
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, video.uri.toString())
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.title)
            }
            startActivity(intent)
        }

        binding.rvVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideos.adapter = adapter

        loadVideos()
    }

    private fun loadVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val videos = withContext(Dispatchers.IO) {
                MediaStoreHelper.getAllVideos(requireContext())
            }
            binding.tvVideoCount.text = "Video zilizopo simuni (${videos.size})"
            adapter.submitList(videos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
