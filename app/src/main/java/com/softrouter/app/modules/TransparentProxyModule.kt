package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class TransparentProxyModule : RouterModule {
    override val id = "tproxy"
    override val name = "Transparent Proxy"
    override val category = "proxy"
    companion object {
        var enabled = false
        var proxyPort = 7890
        var dnsPort = 5353
        var bypassList = mutableListOf<String>()
        var proxyRules = mutableListOf<JSONObject>()
    }
    override fun isEnabled() = enabled
    override fun onStart(): Boolean {
        ShizukuHelper.executeCommand("iptables -t nat -N TPROXY 2>/dev/null || true")
        ShizukuHelper.executeCommand("iptables -t nat -A PREROUTING -j TPROXY 2>/dev/null || true")
        ShizukuHelper.executeCommand("iptables -t nat -A TPROXY -p tcp -j REDIRECT --to-ports $proxyPort 2>/dev/null || true")
        ShizukuHelper.executeCommand("iptables -t nat -A TPROXY -p udp --dport 53 -j REDIRECT --to-ports $dnsPort 2>/dev/null || true")
        bypassList.forEach { cidr ->
            ShizukuHelper.executeCommand("iptables -t nat -I TPROXY -d $cidr -j RETURN 2>/dev/null || true")
        }
        enabled = true
        return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("iptables -t nat -D PREROUTING -j TPROXY 2>/dev/null || true")
        ShizukuHelper.executeCommand("iptables -t nat -F TPROXY 2>/dev/null || true")
        ShizukuHelper.executeCommand("iptables -t nat -X TPROXY 2>/dev/null || true")
        enabled = false
        return true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "configure" -> { val j = JSONObject(body); proxyPort = j.optInt("proxyPort", proxyPort); dnsPort = j.optInt("dnsPort", dnsPort); r.put("success", true) }
                "addBypass" -> { bypassList.add(JSONObject(body).optString("cidr")); if (enabled) { onStop(); onStart() }; r.put("success", true) }
                "addRule" -> { proxyRules.add(JSONObject(body)); r.put("success", true) }
                "status" -> { r.put("enabled", enabled).put("proxyPort", proxyPort).put("dnsPort", dnsPort).put("bypass", JSONArray(bypassList)).put("rules", JSONArray(proxyRules.toList())) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
