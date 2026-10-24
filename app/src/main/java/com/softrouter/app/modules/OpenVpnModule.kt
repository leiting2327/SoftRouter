package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class OpenVpnModule : RouterModule {
    override val id = "openvpn"
    override val name = "OpenVPN"
    override val category = "vpn"
    companion object {
        var serverConfigs = mutableListOf<JSONObject>()
        var clientConfigs = mutableListOf<JSONObject>()
        var serverRunning = false
        var clientConnected = false
    }
    override fun isEnabled() = serverRunning || clientConnected
    override fun onStart(): Boolean {
        if (serverConfigs.isNotEmpty()) { startServer(); return true }
        return false
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("killall openvpn 2>/dev/null || true")
        serverRunning = false; clientConnected = false
        return true
    }
    private fun startServer() {
        val cfg = serverConfigs.firstOrNull() ?: return
        val confPath = "/data/local/tmp/openvpn_server.conf"
        ShizukuHelper.executeCommand("echo '${cfg.optString("config")}' > $confPath")
        ShizukuHelper.executeCommand("openvpn --config $confPath --daemon 2>/dev/null || true")
        serverRunning = true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "addServerConfig" -> { serverConfigs.add(JSONObject(body)); r.put("success", true) }
                "addClientConfig" -> { clientConfigs.add(JSONObject(body)); r.put("success", true) }
                "startServer" -> { startServer(); r.put("success", true) }
                "connectClient" -> {
                    val cfg = JSONObject(body)
                    val confPath = "/data/local/tmp/openvpn_client.conf"
                    ShizukuHelper.executeCommand("echo '${cfg.optString("config")}' > $confPath")
                    ShizukuHelper.executeCommand("openvpn --config $confPath --daemon 2>/dev/null || true")
                    clientConnected = true; r.put("success", true)
                }
                "disconnect" -> { onStop(); r.put("success", true) }
                "status" -> { r.put("serverRunning", serverRunning).put("clientConnected", clientConnected).put("servers", JSONArray(serverConfigs.toList())).put("clients", JSONArray(clientConfigs.toList())) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
