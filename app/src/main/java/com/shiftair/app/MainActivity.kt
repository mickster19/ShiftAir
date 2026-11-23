package com.shiftair.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("shiftair_spa_prefs", Context.MODE_PRIVATE) }
    private var activeCategory = "Apps"
    private val items = mutableListOf<Uri>()
    private val checkedSet = mutableSetOf<Uri>()
    private var mediaPlayer: MediaPlayer? = null
    private var activeMediaUri: Uri? = null
    private var radarAnimator: AnimatorSet? = null

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            prefs.edit().putString("avatar_uri", it.toString()).apply()
            findViewById<ImageView>(R.id.ivAvatar).setImageURI(it)
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            items.clear()
            checkedSet.clear()
            items.addAll(uris)
            checkedSet.addAll(uris)
            updateSelectionLabel()
            renderSelectableList()
            previewUri(uris.first())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val defaultDevice = Build.MODEL ?: "ShiftAir Peer"
        val savedDevice = prefs.getString("device_name", defaultDevice) ?: defaultDevice
        findViewById<EditText>(R.id.etDeviceName).setText(savedDevice)
        findViewById<TextView>(R.id.tvDeviceBadge).text = savedDevice

        prefs.getString("avatar_uri", null)?.let {
            runCatching { findViewById<ImageView>(R.id.ivAvatar).setImageURI(Uri.parse(it)) }
        }

        findViewById<ImageView>(R.id.ivAvatar).setOnClickListener { pickAvatar.launch("image/*") }

        findViewById<Button>(R.id.btnMode).setOnClickListener {
            val dark = prefs.getBoolean("dark_mode", true)
            prefs.edit().putBoolean("dark_mode", !dark).apply()
            applyTheme(!dark)
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val name = findViewById<EditText>(R.id.etDeviceName).text.toString().trim().ifEmpty { defaultDevice }
            prefs.edit().putString("device_name", name).apply()
            findViewById<TextView>(R.id.tvDeviceBadge).text = name
            Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.tabApps).setOnClickListener { switchTab("Apps", false) }
        findViewById<Button>(R.id.tabVideo).setOnClickListener { switchTab("Video", false) }
        findViewById<Button>(R.id.tabAudio).setOnClickListener { switchTab("Audio", false) }
        findViewById<Button>(R.id.tabImages).setOnClickListener { switchTab("Images", false) }
        findViewById<Button>(R.id.tabFiles).setOnClickListener { switchTab("Files", false) }
        findViewById<Button>(R.id.tabSettings).setOnClickListener { switchTab("Settings", true) }

        findViewById<Button>(R.id.btnPickItems).setOnClickListener {
            pickMedia.launch(mimeFor(activeCategory))
        }

        findViewById<Button>(R.id.btnSendSelected).setOnClickListener {
            val chosen = items.filter { checkedSet.contains(it) }
            if (chosen.isEmpty()) {
                pickMedia.launch(mimeFor(activeCategory))
                return@setOnClickListener
            }
            val intent = if (chosen.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeFor(activeCategory)
                    putExtra(Intent.EXTRA_STREAM, chosen.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeFor(activeCategory)
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(chosen))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(Intent.createChooser(intent, "Send via ShiftAir"))
        }

        findViewById<Button>(R.id.btnPlayPause).setOnClickListener {
            val videoView = findViewById<VideoView>(R.id.spaVideoView)
            if (videoView.visibility == View.VISIBLE) {
                if (videoView.isPlaying) videoView.pause() else videoView.start()
                return@setOnClickListener
            }
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    findViewById<TextView>(R.id.tvNowPlaying).text = "Paused • ${activeMediaUri?.lastPathSegment ?: "Audio"}"
                } else {
                    mp.start()
                    findViewById<TextView>(R.id.tvNowPlaying).text = "Playing • ${activeMediaUri?.lastPathSegment ?: "Audio"}"
                }
            } ?: run {
                activeMediaUri?.let { previewUri(it) } ?: pickMedia.launch("audio/*")
            }
        }

        findViewById<Button>(R.id.btnStopMedia).setOnClickListener {
            stopInlineMedia()
            findViewById<TextView>(R.id.tvNowPlaying).text = "Stopped"
        }

        findViewById<LinearLayout>(R.id.spaRadarCard).setOnClickListener {
            findViewById<TextView>(R.id.tvRadarStatus).text = "Scanning Nearby Peers..."
            startRadarAnimation()
        }

        applyTheme(prefs.getBoolean("dark_mode", true))
        renderSelectableList()
        startRadarAnimation()
    }

    private fun switchTab(tab: String, isSettings: Boolean) {
        activeCategory = tab
        findViewById<LinearLayout>(R.id.inlineSettingsBox).visibility =
            if (isSettings) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvNowPlaying).text = "Inline Player • $tab"
        renderSelectableList()
    }

    private fun mimeFor(category: String): String = when (category) {
        "Apps" -> "application/vnd.android.package-archive"
        "Video" -> "video/*"
        "Audio" -> "audio/*"
        "Images" -> "image/*"
        else -> "*/*"
    }

    private fun updateSelectionLabel() {
        findViewById<TextView>(R.id.tvSelectedCount).text = "${checkedSet.size} selected"
    }

    private fun renderSelectableList() {
        val container = findViewById<LinearLayout>(R.id.spaListContainer)
        container.removeAllViews()
        val isDark = prefs.getBoolean("dark_mode", true)
        val labelColor = if (isDark) Color.WHITE else Color.parseColor("#0F172A")

        if (items.isEmpty()) {
            val hint = TextView(this).apply {
                text = "Tap + Select to load $activeCategory here"
                setTextColor(if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569"))
                setBackgroundResource(R.drawable.glass_card)
                setPadding(32, 26, 32, 26)
            }
            container.addView(hint)
            return
        }

        items.forEach { uri ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.glass_card)
                setPadding(24, 18, 24, 18)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 14) }
            }

            val cb = CheckBox(this).apply {
                isChecked = checkedSet.contains(uri)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) checkedSet.add(uri) else checkedSet.remove(uri)
                    updateSelectionLabel()
                }
            }

            val title = TextView(this).apply {
                text = uri.lastPathSegment ?: uri.toString()
                setTextColor(labelColor)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { previewUri(uri) }
            }

            row.addView(cb)
            row.addView(title)
            container.addView(row)
        }
    }

    private fun previewUri(uri: Uri) {
        activeMediaUri = uri
        stopInlineMedia()
        val mime = contentResolver.getType(uri) ?: ""
        val videoView = findViewById<VideoView>(R.id.spaVideoView)
        val imageView = findViewById<ImageView>(R.id.spaImagePreview)
        val nowPlaying = findViewById<TextView>(R.id.tvNowPlaying)

        when {
            mime.startsWith("video/") || activeCategory == "Video" -> {
                videoView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                videoView.setVideoURI(uri)
                videoView.start()
                nowPlaying.text = "Playing Video • ${uri.lastPathSegment ?: "Video"}"
            }
            mime.startsWith("image/") || activeCategory == "Images" -> {
                imageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE
                imageView.setImageURI(uri)
                nowPlaying.text = "Previewing Image • ${uri.lastPathSegment ?: "Image"}"
            }
            else -> {
                videoView.visibility = View.GONE
                imageView.visibility = View.GONE
                runCatching {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@MainActivity, uri)
                        prepare()
                        start()
                    }
                    nowPlaying.text = "Playing Audio • ${uri.lastPathSegment ?: "Audio"}"
                }.onFailure {
                    nowPlaying.text = "Selected • ${uri.lastPathSegment ?: "File"}"
                }
            }
        }
    }

    private fun stopInlineMedia() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        findViewById<VideoView>(R.id.spaVideoView).stopPlayback()
    }

    private fun applyTheme(isDark: Boolean) {
        val scroll = findViewById<ScrollView>(R.id.spaScroll)
        val btnMode = findViewById<Button>(R.id.btnMode)
        val nowPlaying = findViewById<TextView>(R.id.tvNowPlaying)
        val radarStatus = findViewById<TextView>(R.id.tvRadarStatus)

        if (isDark) {
            scroll.setBackgroundColor(Color.parseColor("#070A12"))
            nowPlaying.setTextColor(Color.WHITE)
            radarStatus.setTextColor(Color.WHITE)
            btnMode.text = "Light"
        } else {
            scroll.setBackgroundColor(Color.parseColor("#E2E8F0"))
            nowPlaying.setTextColor(Color.parseColor("#0F172A"))
            radarStatus.setTextColor(Color.parseColor("#0F172A"))
            btnMode.text = "Dark"
        }
        renderSelectableList()
    }

    private fun startRadarAnimation() {
        radarAnimator?.cancel()
        val pulse = findViewById<View>(R.id.radarPulse)
        val rotor = findViewById<View>(R.id.radarRotor)

        val sx = ObjectAnimator.ofFloat(pulse, View.SCALE_X, 0.78f, 1.32f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1150L
        }
        val sy = ObjectAnimator.ofFloat(pulse, View.SCALE_Y, 0.78f, 1.32f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1150L
        }
        val al = ObjectAnimator.ofFloat(pulse, View.ALPHA, 0.9f, 0.2f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1150L
        }
        val rot = ObjectAnimator.ofFloat(rotor, View.ROTATION, 0f, 360f).apply {
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            duration = 2000L
        }

        radarAnimator = AnimatorSet().apply {
            playTogether(sx, sy, al, rot)
            start()
        }
    }

    override fun onDestroy() {
        stopInlineMedia()
        radarAnimator?.cancel()
        super.onDestroy()
    }
}
