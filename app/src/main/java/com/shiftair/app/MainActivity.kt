package com.shiftair.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("shiftair_profile", Context.MODE_PRIVATE) }
    private lateinit var ivAvatar: ImageView
    private lateinit var etDeviceName: EditText
    private lateinit var tvActiveDevice: TextView
    private lateinit var radarPulse: View

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            prefs.edit().putString("avatar_uri", it.toString()).apply()
            ivAvatar.setImageURI(it)
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "${uris.size} item(s) selected to share", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivAvatar = findViewById(R.id.ivProfileAvatar)
        etDeviceName = findViewById(R.id.etDeviceName)
        tvActiveDevice = findViewById(R.id.tvActiveDevice)
        radarPulse = findViewById(R.id.radarPulse)

        val defaultName = Build.MODEL ?: "ShiftAir Device"
        val savedName = prefs.getString("device_name", defaultName) ?: defaultName
        etDeviceName.setText(savedName)
        tvActiveDevice.text = "Broadcasting as: $savedName"

        prefs.getString("avatar_uri", null)?.let {
            runCatching { ivAvatar.setImageURI(Uri.parse(it)) }
        }

        ivAvatar.setOnClickListener { pickAvatar.launch("image/*") }

        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val name = etDeviceName.text.toString().trim().ifEmpty { defaultName }
            prefs.edit().putString("device_name", name).apply()
            tvActiveDevice.text = "Broadcasting as: $name"
            Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnShareApps).setOnClickListener { pickMedia.launch("application/vnd.android.package-archive") }
        findViewById<Button>(R.id.btnShareVideo).setOnClickListener { pickMedia.launch("video/*") }
        findViewById<Button>(R.id.btnShareAudio).setOnClickListener { pickMedia.launch("audio/*") }
        findViewById<Button>(R.id.btnShareImages).setOnClickListener { pickMedia.launch("image/*") }
        findViewById<Button>(R.id.btnShareFiles).setOnClickListener { pickMedia.launch("*/*") }

        startRadarAnimation()
        findViewById<View>(R.id.radarCard).setOnClickListener {
            findViewById<TextView>(R.id.tvRadarStatus).text = getString(R.string.radar_searching)
            startRadarAnimation()
        }
    }

    private fun startRadarAnimation() {
        val scaleX = ObjectAnimator.ofFloat(radarPulse, View.SCALE_X, 0.7f, 1.45f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1100L
        }
        val scaleY = ObjectAnimator.ofFloat(radarPulse, View.SCALE_Y, 0.7f, 1.45f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1100L
        }
        val alpha = ObjectAnimator.ofFloat(radarPulse, View.ALPHA, 0.95f, 0.25f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1100L
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }
}
