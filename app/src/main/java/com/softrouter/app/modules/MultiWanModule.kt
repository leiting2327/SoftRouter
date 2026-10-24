package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class MultiWanModule : RouterModule {
    override val id = "multiwan"
    override val name = "Multi-WAN"
    override val category = "network"
    companion object {
        var wans = mutableListOf<JSONObject>()
        var lbMode = "balance-rr"
        var failoverEnabled = true
        var policyRules = mutableListOf<JSONObject>()
    }
    override fun isEnabled() = wans.size > 1
    override fun onStart(): Boolean {
        applyLoadBalance()
        return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("ip rule flush 2>/dev/null; ip route flush table 100 2>/dev/null; ip route flush table 101 2>/dev/null")
        return true
    }
    private fun applyLoadBalance() {
        wans.forEachIndexed { idx, wan ->
            val iface = wan.optString("interface", "eth$idx")
            val gw = wan.optString("gateway", "")
            val table = 100 + idx
            ShizukuHelper.executeCommand("ip route add default via $gw dev $iface table $table 2>/dev/null || true")
            ShizukuHelper.executeCommand("ip rule add from all lookup $table priority ${1000 + idx} 2>/dev/null || true")
        }
        when (lbMode) {
            "balance-rr" -> ShizukuHelper.executeCommand("ip route replace default scope global nexthop ${wans.joinToString(" ") { "dev " + it.optString("interface","eth0") + " weight 1" }} 2>/dev/null || true")
            "active-backup" -> {
                if (wans.isNotEmpty()) {
                    val primary = wans[0]
                    ShizukuHelper.executeCommand("ip route replace default via ${primary.optString("gateway")} dev ${primary.optString("interface")} 2>/dev/null || true")
                }
            }
        }
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "addWan" -> { wans.add(JSONObject(body)); applyLoadBalance(); r.put("success", true) }
                "removeWan" -> { val j = JSONObject(body); wans.removeAt(j.getInt("index")); applyLoadBalance(); r.put("success", true) }
                "setMode" -> { lbMode = JSONObject(body).optString("mode", lbMode); applyLoadBalance(); r.put("success", true) }
                "setFailover" -> { failoverEnabled = JSONObject(body).optBoolean("enabled", true); r.put("success", true) }
                "addPolicy" -> { policyRules.add(JSONObject(body)); r.put("success", true) }
                "status" -> {
                    r.put("wans", JSONArray(wans.toList())).put("lbMode", lbMode).put("failover", failoverEnabled).put("policies", JSONArray(policyRules.toList()))
                    val res = ShizukuHelper.executeCommand("ip route show 2>/dev/null")
                    r.put("routes", res.stdout)
                }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
