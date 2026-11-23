package com.shiftair.app.ui.audio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.R
import com.shiftair.app.adapters.MediaAdapter
import com.shiftair.app.databinding.FragmentAudioBinding
import com.shiftair.app.model.MediaItemModel
import com.shiftair.app.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioFragment : Fragment() {

    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MediaAdapter
    private var audioPlayer: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioPlayer = ExoPlayer.Builder(requireContext()).build()

        adapter = MediaAdapter(isVideo = false) { track ->
            playTrack(track)
        }

        binding.rvAudio.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAudio.adapter = adapter

        binding.btnMiniPlayPause.setOnClickListener {
            val player = audioPlayer ?: return@setOnClickListener
            if (player.isPlaying) {
                player.pause()
                binding.btnMiniPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                player.play()
                binding.btnMiniPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }

        loadAudio()
    }

    private fun playTrack(track: MediaItemModel) {
        val player = audioPlayer ?: return
        binding.miniPlayerCard.visibility = View.VISIBLE
        binding.tvMiniTitle.text = track.title
        binding.tvMiniArtist.text = track.subtitle
        binding.btnMiniPlayPause.setImageResource(R.drawable.ic_pause)

        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
    }

    private fun loadAudio() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tracks = withContext(Dispatchers.IO) {
                MediaStoreHelper.getAllAudio(requireContext())
            }
            binding.tvAudioHeader.text = "Nyimbo zilizopo simuni (${tracks.size})"
            adapter.submitList(tracks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer?.release()
        audioPlayer = null
        _binding = null
    }
}
