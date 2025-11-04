package com.surya.gamebooster

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Choreographer
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), Choreographer.FrameCallback {

    private lateinit var tvStatus: TextView
    private lateinit var btnTurbo: Button
    private lateinit var btnAutoToggle: Button
    private lateinit var tvFps: TextView
    private lateinit var tvCpuTemp: TextView
    private lateinit var tvCpuUsage: TextView

    private var frameCount = 0
    private var lastTs = 0L
    private val choreo = Choreographer.getInstance()
    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnTurbo = findViewById(R.id.btnTurbo)
        btnAutoToggle = findViewById(R.id.btnAutoToggle)
        tvFps = findViewById(R.id.tvFps)
        tvCpuTemp = findViewById(R.id.tvCpuTemp)
        tvCpuUsage = findViewById(R.id.tvCpuUsage)

        btnTurbo.setOnClickListener { BoostUtils.doBoost(this); tvStatus.append("\nManual Turbo applied") }
        btnAutoToggle.setOnClickListener {
            val intent = Intent(this, UsageMonitorService::class.java)
            startService(intent)
            tvStatus.append("\nAuto-Boost service started")
        }

        // request WRITE_SETTINGS if needed
        if (!Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
        }

        choreo.postFrameCallback(this)
        monitorScope.launch { monitorLoop() }
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameCount++
        val now = System.currentTimeMillis()
        if (lastTs == 0L) lastTs = now
        val elapsed = now - lastTs
        if (elapsed >= 1000) {
            val fps = (frameCount * 1000 / elapsed.toDouble())
            runOnUiThread { tvFps.text = "FPS: " + String.format("%.1f", fps) }
            frameCount = 0
            lastTs = now
        }
        choreo.postFrameCallback(this)
    }

    private suspend fun monitorLoop() {
        while (isActive) {
            val temp = SystemMonitor.readCpuTemp()
            val usage = SystemMonitor.readCpuUsagePercent()
            withContext(Dispatchers.Main) {
                tvCpuTemp.text = "CPU Temp: " + (if (temp!=null) (temp.toString() + "°C") else "-")
                tvCpuUsage.text = "CPU Usage: " + (if (usage!=null) (String.format("%.1f", usage) + "%") else "-")
                // warn if temp high
                if (temp!=null && temp>55) tvStatus.append("\nWarning: High CPU temp: ${temp}°C")
            }
            delay(3000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        choreo.removeFrameCallback(this)
        monitorScope.cancel()
    }
}
