package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class NasModule : RouterModule {
    override val id = "nas"
    override val name = "Light NAS"
    override val category = "storage"
    companion object {
        var nfsEnabled = false
        var shares = mutableListOf<JSONObject>()
        var cloudDrives = mutableListOf<JSONObject>()
    }
    override fun isEnabled() = shares.isNotEmpty() || nfsEnabled
    override fun onStart(): Boolean {
        if (nfsEnabled) ShizukuHelper.executeCommand("exportfs -a 2>/dev/null || true")
        return true
    }
    override fun onStop(): Boolean {
        ShizukuHelper.executeCommand("exportfs -ua 2>/dev/null || true")
        return true
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "addShare" -> { shares.add(JSONObject(body)); r.put("success", true) }
                "removeShare" -> { val j = JSONObject(body); shares.removeAt(j.getInt("index")); r.put("success", true) }
                "enableNfs" -> { nfsEnabled = JSONObject(body).optBoolean("enabled"); if(nfsEnabled) onStart(); r.put("success", true) }
                "addCloudDrive" -> { cloudDrives.add(JSONObject(body)); r.put("success", true) }
                "diskUsage" -> { val res = ShizukuHelper.executeCommand("df -h 2>/dev/null"); r.put("disks", res.stdout) }
                "status" -> { r.put("shares", JSONArray(shares.toList())).put("nfsEnabled", nfsEnabled).put("cloudDrives", JSONArray(cloudDrives.toList())) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
