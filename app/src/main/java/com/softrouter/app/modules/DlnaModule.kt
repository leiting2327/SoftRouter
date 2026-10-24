package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject

class DlnaModule : RouterModule {
    override val id = "dlna"
    override val name = "DLNA Media Server"
    override val category = "media"
    companion object { var running = false; var mediaDir = "/sdcard/Media"; var friendlyName = "SoftRouter DLNA" }
    override fun isEnabled() = running
    override fun onStart(): Boolean {
        ShizukuHelper.executeCommand("minidlnad -R -f /data/local/tmp/minidlna.conf 2>/dev/null || true")
        running = true; return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("killall minidlnad 2>/dev/null || true")
        running = false; return true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "configure" -> { val j = JSONObject(body); mediaDir = j.optString("mediaDir", mediaDir); friendlyName = j.optString("name", friendlyName); r.put("success", true) }
                "rescan" -> { ShizukuHelper.executeCommand("minidlnad -R 2>/dev/null || true"); r.put("success", true) }
                "status" -> { r.put("running", running).put("mediaDir", mediaDir).put("name", friendlyName) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
