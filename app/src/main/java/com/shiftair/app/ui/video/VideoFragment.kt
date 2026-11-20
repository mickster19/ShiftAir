package com.shiftair.app.ui.video

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.shiftair.app.adapters.MediaAdapter
import com.shiftair.app.databinding.FragmentVideoBinding
import com.shiftair.app.util.MediaStoreHelper

class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MediaAdapter { item ->
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
            intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.uri.toString())
            intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.title)
            startActivity(intent)
        }
        binding.rvVideos.layoutManager = GridLayoutManager(requireContext(), 1)
        binding.rvVideos.adapter = adapter
        adapter.submitList(MediaStoreHelper.queryVideos(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
