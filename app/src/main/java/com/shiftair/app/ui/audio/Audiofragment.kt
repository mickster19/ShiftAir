package com.shiftair.app.ui.audio

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiftair.app.R
import com.shiftair.app.adapters.MediaAdapter
import com.shiftair.app.databinding.FragmentAudioBinding
import com.shiftair.app.util.MediaStoreHelper

/**
 * Tab ya "Audio": inaonyesha nyimbo zote zilizopo simuni na
 * inazicheza kwa MediaPlayer ya asili ya Android, na mini-player chini.
 */
class AudioFragment : Fragment() {

    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MediaAdapter { item ->
            playTrack(item.uri.toString(), item.title)
        }
        binding.rvAudios.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAudios.adapter = adapter
        adapter.submitList(MediaStoreHelper.queryAudios(requireContext()))

        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }
    }

    private fun playTrack(uriStr: String, title: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), android.net.Uri.parse(uriStr))
            setOnPreparedListener {
                start()
                isPlaying = true
                updatePlayIcon()
            }
            prepareAsync()
        }
        binding.miniPlayer.visibility = View.VISIBLE
        binding.tvNowPlaying.text = title
    }

    private fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            mp.start()
            isPlaying = true
        }
        updatePlayIcon()
    }

    private fun updatePlayIcon() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_audio else R.drawable.ic_audio
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}
