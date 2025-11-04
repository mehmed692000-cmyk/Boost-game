package com.surya.gamebooster

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*

class UsageMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var lastPackage = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            while (isActive) {
                val topApp = getTopApp()
                if (topApp != null && topApp != lastPackage) {
                    lastPackage = topApp
                    if (isGame(topApp)) {
                        sendBroadcast(Intent("BOOST_NOW"))
                        // also apply boost locally (best-effort)
                        BoostUtils.doBoost(applicationContext)
                    } else {
                        sendBroadcast(Intent("BOOST_END"))
                        BoostUtils.restore(applicationContext)
                    }
                }
                delay(3000)
            }
        }
        return START_STICKY
    }

    private fun getTopApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10000
        val events = usm.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var lastApp: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND)
                lastApp = event.packageName
        }
        return lastApp
    }

    private fun isGame(pkg: String): Boolean {
        return pkg.contains("game", ignoreCase = true) ||
               pkg.contains("pubg", ignoreCase = true) ||
               pkg.contains("mobilelegends", ignoreCase = true) ||
               pkg.contains("freefire", ignoreCase = true) ||
               pkg.contains("callofduty", ignoreCase = true)
    }

    override fun onDestroy() {
        super.onDestroy(); scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
