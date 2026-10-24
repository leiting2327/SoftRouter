package com.softrouter.app.modules

import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class WireGuardModule : RouterModule {
    override val id = "wireguard"
    override val name = "WireGuard VPN"
    override val category = "vpn"
    companion object { var configs = mutableListOf<JSONObject>(); var activeIdx = -1 }
    override fun isEnabled() = activeIdx >= 0
    override fun onStart(): Boolean {
        if (configs.isEmpty()) return false
        ShizukuHelper.executeCommand("wg-quick up /data/local/tmp/wg0.conf 2>/dev/null || true")
        activeIdx = 0; return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("wg-quick down /data/local/tmp/wg0.conf 2>/dev/null || true")
        activeIdx = -1; return true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "addConfig" -> { configs.add(JSONObject(body)); r.put("success", true) }
                "removeConfig" -> { val j = JSONObject(body); configs.removeAt(j.getInt("index")); r.put("success", true) }
                "connect" -> { val j = JSONObject(body); activeIdx = j.getInt("index"); onStart(); r.put("success", true) }
                "disconnect" -> { onStop(); r.put("success", true) }
                "status" -> {
                    val res = ShizukuHelper.executeCommand("wg show 2>/dev/null || echo not_running")
                    r.put("active", activeIdx >= 0).put("info", res.stdout).put("configs", JSONArray(configs.toList()))
                }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}

</content>