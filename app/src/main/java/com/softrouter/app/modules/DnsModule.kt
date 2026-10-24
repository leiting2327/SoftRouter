package com.softrouter.app.modules

import com.softrouter.app.engine.RouterModule
import com.softrouter.app.ShizukuHelper
import org.json.JSONObject
import org.json.JSONArray

data class DnsRule(val domain: String, val server: String) {
    fun toJson(): JSONObject = JSONObject()
        .put("domain", domain)
        .put("server", server)
}

class DnsModule : RouterModule {
    override val id = "dns"
    override val name = "Smart DNS"
    override val category = "network"

    override fun isEnabled(): Boolean = enabled

    override fun onStart(): Boolean {
        enabled = true
        writeConfig()
        ShizukuHelper.executeCommand("killall dnsmasq")
        ShizukuHelper.executeCommand("dnsmasq -C /data/local/tmp/smartdns.conf")
        return true
    }

    override fun onStop(): Boolean {
        enabled = false
        ShizukuHelper.executeCommand("killall dnsmasq")
        return true
    }

    override fun handleAction(action: String, body: String): JSONObject {
        return try {
            when (action) {
                "getServers" -> JSONObject().put("success", true).put("servers", JSONArray(upstreams))
                "setServers" -> {
                    val b = JSONObject(body)
                    val arr = b.getJSONArray("servers")
                    upstreams.clear()
                    for (i in 0 until arr.length()) {
                        upstreams.add(arr.getString(i))
                    }
                    JSONObject().put("success", true)
                }
                "addRule" -> {
                    val b = JSONObject(body)
                    rules.add(DnsRule(b.getString("domain"), b.getString("server")))
                    JSONObject().put("success", true)
                }
                "query" -> {
                    val b = JSONObject(body)
                    val domain = b.getString("domain")
                    val server = b.optString("server", "")
                    val cmd = if (server.isEmpty()) "nslookup $domain" else "nslookup $domain $server"
                    val out = ShizukuHelper.executeCommand(cmd)
                    JSONObject().put("success", true).put("output", out)
                }
                else -> JSONObject().put("success", false).put("error", "Unknown action")
            }
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message)
        }
    }

    private fun writeConfig() {
        val lines = mutableListOf<String>()
        lines.add("port=53")
        lines.add("bind-interfaces")
        upstreams.forEach { lines.add("server=$it") }
        rules.forEach { lines.add("server=/${it.domain}/${it.server}") }
        val conf = lines.joinToString("\n")
        ShizukuHelper.executeCommand("printf '%s\n' '$conf' > /data/local/tmp/smartdns.conf")
    }

    companion object {
        private var enabled = false
        private val upstreams = mutableListOf("8.8.8.8", "1.1.1.1")
        private val rules = mutableListOf<DnsRule>()

        @JvmStatic
        fun toJson(): JSONObject {
            val arr = JSONArray()
            rules.forEach { arr.put(it.toJson()) }
            return JSONObject()
                .put("enabled", enabled)
                .put("upstreams", JSONArray(upstreams))
                .put("rules", arr)
        }

        @JvmStatic
        fun fromJson(json: JSONObject) {
            enabled = json.optBoolean("enabled", false)
            upstreams.clear()
            json.optJSONArray("upstreams")?.let {
                for (i in 0 until it.length()) upstreams.add(it.getString(i))
            }
            rules.clear()
            json.optJSONArray("rules")?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    rules.add(DnsRule(o.getString("domain"), o.getString("server")))
                }
            }
        }
    }
}
