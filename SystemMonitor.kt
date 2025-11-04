\
    package com.surya.gamebooster

    import java.io.File
    import java.util.Scanner

    object SystemMonitor {

        // Attempt to read CPU temp from common thermal zones. Returns degrees Celsius or null.
        fun readCpuTemp(): Int? {
            val candidates = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/class/thermal/thermal_zone3/temp",
                "/sys/class/thermal/thermal_zone4/temp"
            )
            for (p in candidates) {
                try {
                    val f = File(p)
                    if (f.exists()) {
                        val s = Scanner(f).useDelimiter("\\A").next().trim()
                        val v = s.toIntOrNull() ?: continue
                        // many devices report in millidegree
                        return if (v > 1000) v / 1000 else v
                    }
                } catch (_: Exception) {}
            }
            return null
        }

        // Read CPU usage percent by parsing /proc/stat (simple, single-sample approximation)
        fun readCpuUsagePercent(): Double? {
            try {
                val stat1 = File("/proc/stat").readLines().firstOrNull() ?: return null
                val toks1 = stat1.split(Regex("\\s+")).drop(1).map { it.toLongOrNull() ?: 0L }
                val idle1 = toks1[3]
                val total1 = toks1.sum()
                Thread.sleep(200)
                val stat2 = File("/proc/stat").readLines().firstOrNull() ?: return null
                val toks2 = stat2.split(Regex("\\s+")).drop(1).map { it.toLongOrNull() ?: 0L }
                val idle2 = toks2[3]
                val total2 = toks2.sum()
                val idleDelta = idle2 - idle1
                val totalDelta = total2 - total1
                if (totalDelta <= 0) return null
                val usage = (totalDelta - idleDelta).toDouble() * 100.0 / totalDelta.toDouble()
                return usage
            } catch (_: Exception) { return null }
        }
    }
