package com.softrouter.app.modules

import com.softrouter.app.engine.RouterModule
import com.softrouter.app.ShizukuHelper
import org.json.JSONObject
import org.json.JSONArray

class AdBlockModule : RouterModule {
    override val id = "adblock"
    override val name = "Ad Block"
    override val category = "security"

    override fun isEnabled(): Boolean = enabled

    override fun onStart(): Boolean {
        enabled = true
        apply()
        return true
    }

    override fun onStop(): Boolean {
        enabled = false
        removeRedirect()
        ShizukuHelper.executeCommand("killall dnsmasq")
        return true
    }

    override fun handleAction(action: String, body: String): JSONObject {
        return try {
            when (action) {
                "enable" -> {
                    enabled = true
                    apply()
                    JSONObject().put("success", true)
                }
                "disable" -> {
                    enabled = false
                    removeRedirect()
                    ShizukuHelper.executeCommand("killall dnsmasq")
                    JSONObject().put("success", true)
                }
                "update" -> {
                    val count = updateHosts()
                    JSONObject().put("success", true).put("blockedDomains", count)
                }
                "stats" -> {
                    val count = countBlocked()
                    JSONObject().put("success", true).put("blockedDomains", count)
                }
                else -> JSONObject().put("success", false).put("error", "Unknown action")
            }
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message)
        }
    }

    private fun apply() {
        removeRedirect()
        updateHosts()
        ShizukuHelper.executeCommand("iptables -t nat -N ADBLOCK_DNS")
        ShizukuHelper.executeCommand("iptables -t nat -A ADBLOCK_DNS -p udp --dport 53 -j REDIRECT --to-port 5353")
        ShizukuHelper.executeCommand("iptables -t nat -A ADBLOCK_DNS -p tcp --dport 53 -j REDIRECT --to-port 5353")
        ShizukuHelper.executeCommand("iptables -t nat -A PREROUTING -j ADBLOCK_DNS")
        ShizukuHelper.executeCommand("killall dnsmasq")
        ShizukuHelper.executeCommand("dnsmasq --port=5353 --addn-hosts=/data/local/tmp/adblock.hosts --no-dhcp-interface= --bind-interfaces")
    }

    private fun removeRedirect() {
        ShizukuHelper.executeCommand("iptables -t nat -D PREROUTING -j ADBLOCK_DNS 2>/dev/null")
        ShizukuHelper.executeCommand("iptables -t nat -F ADBLOCK_DNS 2>/dev/null")
        ShizukuHelper.executeCommand("iptables -t nat -X ADBLOCK_DNS 2>/dev/null")
    }

    private fun updateHosts(): Int {
        val sb = StringBuilder()
        for (url in blocklistUrls) {
            sb.append(ShizukuHelper.executeCommand("curl -sL \"$url\"")).append("\n")
        }
        val escaped = sb.toString().replace("'", "'\\''")
        ShizukuHelper.executeCommand("printf '%s\n' '$escaped' > /data/local/tmp/adblock.hosts")
        return countBlocked()
    }

    private fun countBlocked(): Int {
        val out = ShizukuHelper.executeCommand(
            "grep -cE '^(0\\.0\\.0\\.0|127\\.0\\.0\\.1)\\s+' /data/local/tmp/adblock.hosts 2>/dev/null || echo 0"
        )
        return out.trim().toIntOrNull() ?: 0
    }

    companion object {
        private var enabled = false
        private val blocklistUrls = mutableListOf(
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
        )

        @JvmStatic
        fun toJson(): JSONObject = JSONObject()
            .put("enabled", enabled)
            .put("blocklistUrls", JSONArray(blocklistUrls))

        @JvmStatic
        fun fromJson(json: JSONObject) {
            enabled = json.optBoolean("enabled", false)
            blocklistUrls.clear()
            json.optJSONArray("blocklistUrls")?.let {
                for (i in 0 until it.length()) blocklistUrls.add(it.getString(i))
            }
        }
    }
}
