package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject

class QbittorrentModule : RouterModule {
    override val id = "qbittorrent"
    override val name = "qBittorrent"
    override val category = "download"
    companion object { var running = false; var webuiPort = 8081; var downloadDir = "/sdcard/Downloads" }
    override fun isEnabled() = running
    override fun onStart(): Boolean {
        ShizukuHelper.executeCommand("qbittorrent-nox --webui-port=$webuiPort -d 2>/dev/null || true")
        running = true; return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("killall qbittorrent-nox 2>/dev/null || true")
        running = false; return true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "configure" -> { val j = JSONObject(body); webuiPort = j.optInt("port", webuiPort); downloadDir = j.optString("dir", downloadDir); r.put("success", true) }
                "status" -> { r.put("running", running).put("webuiPort", webuiPort).put("downloadDir", downloadDir) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
