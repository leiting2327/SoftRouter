package com.softrouter.app.engine

import com.softrouter.app.ShizukuHelper
import com.softrouter.app.modules.*
import org.json.JSONObject

object RouterEngine {
    val modules = mutableMapOf<String, RouterModule>()
    var isRunning = false
    var startedAt = 0L

    fun init() {
        register(FirewallModule())
        register(DhcpModule())
        register(PortForwardModule())
        register(DnsModule())
        register(AdBlockModule())
        register(WireGuardModule())
        register(DdnsModule())
        register(QoSModule())
        register(DiagnosticsModule())
        register(MonitorModule())
        register(ConfigBackupModule())
        register(Ipv6Module())
        register(ParentalControlModule())
        register(SambaModule())
        register(Aria2Module())
        register(TrafficSplitModule())
    }

    private fun register(module: RouterModule) {
        modules[module.id] = module
    }

    fun startAll(): JSONObject {
        val result = JSONObject()
        val cmd = buildString {
            appendLine("sysctl -w net.ipv4.ip_forward=1")
            appendLine("iptables -t nat -A POSTROUTING -o wlan0 -j MASQUERADE 2>/dev/null")
            appendLine("iptables -A FORWARD -i wlan1 -o wlan0 -j ACCEPT 2>/dev/null")
            appendLine("iptables -A FORWARD -i wlan0 -o wlan1 -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null")
        }
        val r = ShizukuHelper.executeCommand(cmd)
        result.put("core", r.success)
        modules.forEach { (id, mod) ->
            try {
                val mr = mod.onStart()
                result.put(id, mr)
            } catch (e: Exception) {
                result.put(id, "error: ${e.message}")
            }
        }
        isRunning = true
        startedAt = System.currentTimeMillis()
        return result
    }

    fun stopAll(): JSONObject {
        val result = JSONObject()
        modules.forEach { (id, mod) ->
            try { result.put(id, mod.onStop()) } catch (_: Exception) {}
        }
        ShizukuHelper.executeCommand("killall dnsmasq 2>/dev/null; killall wg-quick 2>/dev/null; iptables -t nat -F POSTROUTING 2>/dev/null; iptables -F FORWARD 2>/dev/null; sysctl -w net.ipv4.ip_forward=0")
        isRunning = false
        startedAt = 0
        result.put("core", true)
        return result
    }

    fun getStatus(): JSONObject {
        val status = JSONObject()
        status.put("running", isRunning)
        status.put("uptime", if (isRunning) (System.currentTimeMillis() - startedAt) / 1000 else 0L)
        status.put("modules", JSONObject().also { m ->
            modules.forEach { (id, mod) ->
                m.put(id, JSONObject().apply {
                    put("name", mod.name)
                    put("enabled", mod.isEnabled())
                    put("category", mod.category)
                })
            }
        })
        return status
    }

    fun handleApi(moduleId: String, action: String, body: String): JSONObject {
        val mod = modules[moduleId] ?: return JSONObject().put("error", "Module not found")
        return mod.handleAction(action, body)
    }
}

interface RouterModule {
    val id: String
    val name: String
    val category: String
    fun isEnabled(): Boolean
    fun onStart(): Boolean
    fun onStop(): Boolean
    fun handleAction(action: String, body: String): JSONObject
}

</content>