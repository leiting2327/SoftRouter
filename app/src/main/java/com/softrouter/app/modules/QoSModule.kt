package com.softrouter.app.modules

import com.softrouter.app.engine.RouterModule
import com.softrouter.app.ShizukuHelper
import org.json.JSONObject
import org.json.JSONArray

data class BandwidthLimit(
    val iface: String,
    val downloadKbps: Int,
    val uploadKbps: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("iface", iface)
        .put("downloadKbps", downloadKbps)
        .put("uploadKbps", uploadKbps)
}

class QoSModule : RouterModule {
    override val id = "qos"
    override val name = "QoS"
    override val category = "performance"

    override fun isEnabled(): Boolean = enabled

    override fun onStart(): Boolean {
        enabled = true
        applyLimits()
        return true
    }

    override fun onStop(): Boolean {
        enabled = false
        clearLimits()
        return true
    }

    override fun handleAction(action: String, body: String): JSONObject {
        return try {
            when (action) {
                "setLimit" -> {
                    val b = JSONObject(body)
                    val iface = b.getString("iface")
                    val dl = b.getInt("download")
                    val ul = b.getInt("upload")
                    limits.removeAll { it.iface == iface }
                    limits.add(BandwidthLimit(iface, dl, ul))
                    applyLimit(iface, dl, ul)
                    JSONObject().put("success", true)
                }
                "clear" -> {
                    val b = JSONObject(body)
                    if (b.has("iface")) {
                        val iface = b.getString("iface")
                        limits.removeAll { it.iface == iface }
                        clearLimit(iface)
                    } else {
                        limits.clear()
                        clearAllLimits()
                    }
                    JSONObject().put("success", true)
                }
                "status" -> {
                    val b = JSONObject(body)
                    val iface = if (b.has("iface")) b.getString("iface") else ""
                    val cmd = if (iface.isNotEmpty()) "tc -s qdisc show dev $iface" else "tc -s qdisc show"
                    val out = ShizukuHelper.executeCommand(cmd)
                    JSONObject().put("success", true).put("output", out)
                }
                else -> JSONObject().put("success", false).put("error", "Unknown action")
            }
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message)
        }
    }

    private fun applyLimits() {
        limits.forEach { applyLimit(it.iface, it.downloadKbps, it.uploadKbps) }
    }

    private fun clearAllLimits() {
        limits.forEach { clearLimit(it.iface) }
    }

    private fun applyLimit(iface: String, dl: Int, ul: Int) {
        clearLimit(iface)
        ShizukuHelper.executeCommand("tc qdisc add dev $iface root tbf rate ${ul}kbit burst 32k latency 50ms")
        ShizukuHelper.executeCommand("tc qdisc add dev $iface ingress")
        ShizukuHelper.executeCommand(
            "tc filter add dev $iface parent ffff: protocol ip u32 match u32 0 0 police rate ${dl}kbit burst 32k drop"
        )
    }

    private fun clearLimit(iface: String) {
        ShizukuHelper.executeCommand("tc qdisc del dev $iface root 2>/dev/null")
        ShizukuHelper.executeCommand("tc qdisc del dev $iface ingress 2>/dev/null")
    }

    companion object {
        private var enabled = false
        private val limits = mutableListOf<BandwidthLimit>()

        @JvmStatic
        fun toJson(): JSONObject {
            val arr = JSONArray()
            limits.forEach { arr.put(it.toJson()) }
            return JSONObject().put("enabled", enabled).put("limits", arr)
        }

        @JvmStatic
        fun fromJson(json: JSONObject) {
            enabled = json.optBoolean("enabled", false)
            limits.clear()
            json.optJSONArray("limits")?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    limits.add(
                        BandwidthLimit(
                            o.getString("iface"),
                            o.getInt("downloadKbps"),
                            o.getInt("uploadKbps")
                        )
                    )
                }
            }
        }
    }
}
