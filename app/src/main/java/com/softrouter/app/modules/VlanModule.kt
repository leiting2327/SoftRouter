package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class VlanModule : RouterModule {
    override val id = "vlan"
    override val name = "VLAN Management"
    override val category = "network"
    companion object {
        var vlans = mutableListOf<JSONObject>()
        var guestIsolated = false
        var iotIsolated = false
    }
    override fun isEnabled() = vlans.isNotEmpty()
    override fun onStart(): Boolean { applyVlans(); return true }
    override fun onStop(): Boolean {
        vlans.forEach { v ->
            val vid = v.optInt("id", 0)
            ShizukuHelper.executeCommand("ip link del vlan$vid 2>/dev/null || true")
        }
        return true
    }
    private fun applyVlans() {
        vlans.forEach { v ->
            val parent = v.optString("parent", "eth0")
            val vid = v.optInt("id", 10)
            val subnet = v.optString("subnet", "192.168.$vid.1/24")
            ShizukuHelper.executeCommand("ip link add link $parent name vlan$vid type vlan id $vid 2>/dev/null || true")
            ShizukuHelper.executeCommand("ip addr add $subnet dev vlan$vid 2>/dev/null || true")
            ShizukuHelper.executeCommand("ip link set vlan$vid up 2>/dev/null || true")
        }
        if (guestIsolated) {
            ShizukuHelper.executeCommand("iptables -A FORWARD -i vlan+ -d 192.168.0.0/16 -j DROP 2>/dev/null || true")
        }
        if (iotIsolated) {
            vlans.filter { it.optString("type") == "iot" }.forEach { v ->
                val vid = v.optInt("id")
                ShizukuHelper.executeCommand("iptables -A FORWARD -i vlan$vid -o vlan+ -j DROP 2>/dev/null || true")
            }
        }
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "addVlan" -> { vlans.add(JSONObject(body)); applyVlans(); r.put("success", true) }
                "removeVlan" -> { val j = JSONObject(body); vlans.removeAt(j.getInt("index")); r.put("success", true) }
                "setGuestIsolation" -> { guestIsolated = JSONObject(body).optBoolean("enabled"); applyVlans(); r.put("success", true) }
                "setIotIsolation" -> { iotIsolated = JSONObject(body).optBoolean("enabled"); applyVlans(); r.put("success", true) }
                "status" -> { r.put("vlans", JSONArray(vlans.toList())).put("guestIsolated", guestIsolated).put("iotIsolated", iotIsolated) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
