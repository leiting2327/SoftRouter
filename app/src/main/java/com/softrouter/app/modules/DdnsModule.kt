package com.softrouter.app.modules

import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject

class DdnsModule : RouterModule {
    override val id = "ddns"
    override val name = "DDNS"
    override val category = "network"
    companion object { var provider = "cloudflare"; var domain = ""; var token = ""; var enabled = false }
    override fun isEnabled() = enabled
    override fun onStart(): Boolean { enabled = true; updateDdns(); return true }
    override fun onStop(): Boolean { enabled = false; return true }
    private fun updateDdns() {
        val ip = ShizukuHelper.executeCommand("curl -s ifconfig.me").stdout.trim()
        if (provider == "cloudflare" && domain.isNotEmpty()) {
            ShizukuHelper.executeCommand("curl -s -X PUT https://api.cloudflare.com/client/v4/zones/dns_records -H Authorization:Bearer\\ $token -d type=A,name=$domain,content=$ip 2>/dev/null || true")
        }
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "configure" -> { val j = JSONObject(body); provider = j.optString("provider", provider); domain = j.optString("domain", domain); token = j.optString("token", token); r.put("success", true) }
                "update" -> { updateDdns(); r.put("success", true) }
                "status" -> { r.put("enabled", enabled).put("provider", provider).put("domain", domain) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}

</content>