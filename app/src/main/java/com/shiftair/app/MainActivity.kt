package com.shiftair.app

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("shiftair_prefs", Context.MODE_PRIVATE) }
    private var currentTab = "Apps"
    private val selectedUris = mutableListOf<Uri>()

    private val pickFiles = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
            renderCategoryItems(currentTab)
            Toast.makeText(this, "${uris.size} selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            prefs.edit().putString("avatar_uri", it.toString()).apply()
            findViewById<ImageView>(R.id.ivAvatar).setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val defaultDevice = Build.MODEL ?: "ShiftAir Device"
        val etDevice = findViewById<EditText>(R.id.etDeviceName)
        etDevice.setText(prefs.getString("device_name", defaultDevice))

        prefs.getString("avatar_uri", null)?.let {
            runCatching { findViewById<ImageView>(R.id.ivAvatar).setImageURI(Uri.parse(it)) }
        }
        findViewById<ImageView>(R.id.ivAvatar).setOnClickListener { pickAvatar.launch("image/*") }

        findViewById<Button>(R.id.btnSaveName).setOnClickListener {
            val name = etDevice.text.toString().trim().ifEmpty { defaultDevice }
            prefs.edit().putString("device_name", name).apply()
            Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
        }

        val toggleTheme = {
            val isDark = prefs.getBoolean("dark_mode", true)
            prefs.edit().putBoolean("dark_mode", !isDark).apply()
            applyTheme(!isDark)
        }
        findViewById<Button>(R.id.btnToggleTheme).setOnClickListener { toggleTheme() }
        findViewById<Button>(R.id.btnModeToggle).setOnClickListener { toggleTheme() }

        findViewById<Button>(R.id.tabHistory).setOnClickListener { selectTab("History", "*/*") }
        findViewById<Button>(R.id.tabApps).setOnClickListener { selectTab("Apps", "application/vnd.android.package-archive") }
        findViewById<Button>(R.id.tabPhotos).setOnClickListener { selectTab("Photos", "image/*") }
        findViewById<Button>(R.id.tabVideos).setOnClickListener { selectTab("Videos", "video/*") }
        findViewById<Button>(R.id.tabMusic).setOnClickListener { selectTab("Music", "audio/*") }
        findViewById<Button>(R.id.tabFiles).setOnClickListener { selectTab("Files", "*/*") }

        findViewById<Button>(R.id.btnSendAction).setOnClickListener {
            val mime = when (currentTab) {
                "Apps" -> "application/vnd.android.package-archive"
                "Photos" -> "image/*"
                "Videos" -> "video/*"
                "Music" -> "audio/*"
                else -> "*/*"
            }
            pickFiles.launch(mime)
        }

        findViewById<Button>(R.id.btnReceiveAction).setOnClickListener {
            val dev = prefs.getString("device_name", defaultDevice)
            Toast.makeText(this, "$dev • Receive", Toast.LENGTH_SHORT).show()
        }

        updateStorageStats()
        applyTheme(prefs.getBoolean("dark_mode", true))
        renderCategoryItems(currentTab)
    }

    private fun selectTab(tab: String, mime: String) {
        currentTab = tab
        findViewById<TextView>(R.id.tvActiveTabSummary).text = tab
        renderCategoryItems(tab)
        if (tab != "History" && tab != "Files") {
            pickFiles.launch(mime)
        }
    }

    private fun updateStorageStats() {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val totalGb = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val usedGb = (totalGb - freeGb).coerceAtLeast(0.0)
        findViewById<TextView>(R.id.tvStorageInfo).text =
            String.format("%.2f GB / %.2f GB", usedGb, totalGb)
    }

    private fun applyTheme(isDark: Boolean) {
        val bg = ContextCompat.getColor(this, if (isDark) R.color.bg_dark else R.color.bg_light)
        val surface = ContextCompat.getColor(this, if (isDark) R.color.surface_dark else R.color.surface_light)
        val textPrimary = ContextCompat.getColor(this, if (isDark) R.color.text_dark_primary else R.color.text_light_primary)
        val textSecondary = ContextCompat.getColor(this, if (isDark) R.color.text_dark_secondary else R.color.text_light_secondary)

        findViewById<LinearLayout>(R.id.rootContainer).setBackgroundColor(bg)
        findViewById<LinearLayout>(R.id.settingsPanel).setBackgroundColor(surface)
        findViewById<LinearLayout>(R.id.storageCard).setBackgroundColor(surface)
        findViewById<LinearLayout>(R.id.bottomActionBar).setBackgroundColor(surface)
        findViewById<TextView>(R.id.tvDeviceLabel).setTextColor(textPrimary)
        findViewById<EditText>(R.id.etDeviceName).setTextColor(textPrimary)
        findViewById<TextView>(R.id.tvStorageInfo).setTextColor(textPrimary)
        findViewById<TextView>(R.id.tvActiveTabSummary).setTextColor(textSecondary)
        findViewById<Button>(R.id.btnToggleTheme).text = getString(if (isDark) R.string.theme_light else R.string.theme_dark)
    }

    private fun renderCategoryItems(tab: String) {
        val container = findViewById<LinearLayout>(R.id.itemsContainer)
        container.removeAllViews()
        val isDark = prefs.getBoolean("dark_mode", true)
        val cardBg = ContextCompat.getColor(this, if (isDark) R.color.surface_dark else R.color.surface_light)
        val textColor = ContextCompat.getColor(this, if (isDark) R.color.text_dark_primary else R.color.text_light_primary)

        val entries = if (selectedUris.isNotEmpty()) {
            selectedUris.map { it.lastPathSegment ?: "Item" }
        } else when (tab) {
            "History" -> listOf("Received", "Sent")
            "Files" -> listOf("Documents", "Ebooks", "APKs", "Archives", "Big files")
            else -> listOf(tab)
        }

        entries.forEach { label ->
            val row = TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(textColor)
                setBackgroundColor(cardBg)
                setPadding(36, 28, 36, 28)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                layoutParams = lp
            }
            container.addView(row)
        }
    }
}
