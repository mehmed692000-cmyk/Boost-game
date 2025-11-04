package com.surya.gamebooster

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BoostUtils {

    private var oldBrightness: Int? = null
    fun doBoost(ctx: Context) {
        enableDnd(ctx)
        lowerBrightness(ctx)
        killBackgroundApps(ctx)
        requestIgnoreBatteryOptimizations(ctx)
    }

    fun restore(ctx: Context) {
        // restore DND and brightness
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        if (oldBrightness != null && Settings.System.canWrite(ctx)) {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, oldBrightness!!)
        }
    }

    private fun enableDnd(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
    }

    private fun lowerBrightness(ctx: Context) {
        try {
            if (Settings.System.canWrite(ctx)) {
                val cur = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                oldBrightness = cur
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (255 * 0.4).toInt())
            }
        } catch (_: Exception) {}
    }

    private fun killBackgroundApps(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val apps = ctx.packageManager.getInstalledApplications(0)
            for (app in apps) {
                if (app.packageName == ctx.packageName) continue
                if ((app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue
                try { am.killBackgroundProcesses(app.packageName) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun requestIgnoreBatteryOptimizations(ctx: Context) {
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + ctx.packageName)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(intent)
            }
        } catch (_: Exception) {}
    }
}
