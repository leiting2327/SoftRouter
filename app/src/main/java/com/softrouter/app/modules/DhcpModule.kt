package com.softrouter.app.modules

import com.softrouter.app.engine.RouterModule
import com.softrouter.app.ShizukuHelper
import org.json.JSONObject
import org.json.JSONArray

class DhcpModule : RouterModule {
    override val id = "dhcp"
    override val name = "DHCP Server"
    override val category = "network"

    override fun isEnabled(): Boolean = enabled

    override fun onStart(): Boolean {
        enabled = true
        writeConfig()
        ShizukuHelper.executeCommand("killall dnsmasq")
        ShizukuHelper.executeCommand("dnsmasq -C /data/local/tmp/dnsmasq.conf")
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
                "get" -> JSONObject().put("success", true).put("config", configJson())
                "set" -> {
                    val b = JSONObject(body)
                    if (b.has("rangeStart")) rangeStart = b.getString("rangeStart")
                    if (b.has("rangeEnd")) rangeEnd = b.getString("rangeEnd")
                    if (b.has("leaseTime")) leaseTime = b.getString("leaseTime")
                    if (b.has("dns")) dns = b.getString("dns")
                    if (b.has("gateway")) gateway = b.getString("gateway")
                    if (b.has("interface")) iface = b.getString("interface")
                    JSONObject().put("success", true)
                }
                else -> JSONObject().put("success", false).put("error", "Unknown action")
            }
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message)
        }
    }

    private fun configJson(): JSONObject {
        return JSONObject()
            .put("rangeStart", rangeStart)
            .put("rangeEnd", rangeEnd)
            .put("leaseTime", leaseTime)
            .put("dns", dns)
            .put("gateway", gateway)
            .put("interface", iface)
    }

    private fun writeConfig() {
        val conf = """
            interface=$iface
            dhcp-range=$rangeStart,$rangeEnd,$leaseTime
            dhcp-option=6,$dns
            dhcp-option=3,$gateway
            bind-interfaces
            except-interface=lo
        """.trimIndent()
        ShizukuHelper.executeCommand("printf '%s\n' '$conf' > /data/local/tmp/dnsmasq.conf")
    }

    companion object {
        private var enabled = false
        private var rangeStart = "192.168.50.100"
        private var rangeEnd = "192.168.50.200"
        private var leaseTime = "12h"
        private var dns = "8.8.8.8"
        private var gateway = "192.168.50.1"
        private var iface = "wlan0"

        @JvmStatic
        fun toJson(): JSONObject = JSONObject()
            .put("enabled", enabled)
            .put("rangeStart", rangeStart)
            .put("rangeEnd", rangeEnd)
            .put("leaseTime", leaseTime)
            .put("dns", dns)
            .put("gateway", gateway)
            .put("interface", iface)

        @JvmStatic
        fun fromJson(json: JSONObject) {
            enabled = json.optBoolean("enabled", false)
            rangeStart = json.optString("rangeStart", rangeStart)
            rangeEnd = json.optString("rangeEnd", rangeEnd)
            leaseTime = json.optString("leaseTime", leaseTime)
            dns = json.optString("dns", dns)
            gateway = json.optString("gateway", gateway)
            iface = json.optString("interface", iface)
        }
    }
}
