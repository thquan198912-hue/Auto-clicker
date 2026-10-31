package com.example.autoclicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var rbTap: RadioButton
    private lateinit var rbSwipe: RadioButton
    private lateinit var etInterval: EditText
    private lateinit var etRepeat: EditText
    private lateinit var etSwipeDuration: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        status = findViewById(R.id.status)
        rbTap = findViewById(R.id.rbTap)
        rbSwipe = findViewById(R.id.rbSwipe)
        etInterval = findViewById(R.id.etInterval)
        etRepeat = findViewById(R.id.etRepeat)
        etSwipeDuration = findViewById(R.id.etSwipeDuration)

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnAppInfo).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnApply).setOnClickListener {
            saveSettings()
            val service = AutoClickService.instance
            if (service == null) {
                Toast.makeText(this, R.string.toast_need_service, Toast.LENGTH_LONG).show()
            } else {
                service.showOverlay()
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        status.setText(
            if (AutoClickService.instance != null) R.string.status_on else R.string.status_off
        )
    }

    override fun onPause() {
        super.onPause()
        saveSettings()
    }

    private fun loadSettings() {
        if (prefs.swipeMode) {
            rbSwipe.isChecked = true
        } else {
            rbTap.isChecked = true
        }
        etInterval.setText(prefs.intervalMs.toString())
        etRepeat.setText(prefs.repeatCount.toString())
        etSwipeDuration.setText(prefs.swipeDurationMs.toString())
    }

    private fun saveSettings() {
        prefs.swipeMode = rbSwipe.isChecked
        prefs.intervalMs = (etInterval.text.toString().toLongOrNull() ?: 500L)
            .coerceIn(20L, 3_600_000L)
        prefs.repeatCount = (etRepeat.text.toString().toIntOrNull() ?: 0)
            .coerceAtLeast(0)
        prefs.swipeDurationMs = (etSwipeDuration.text.toString().toLongOrNull() ?: 300L)
            .coerceIn(50L, 10_000L)
    }
}
