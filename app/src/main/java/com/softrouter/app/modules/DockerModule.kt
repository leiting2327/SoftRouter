package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray

class DockerModule : RouterModule {
    override val id = "docker"
    override val name = "Docker Manager"
    override val category = "services"
    companion object { var available = false }
    override fun isEnabled() = available
    override fun onStart(): Boolean {
        val res = ShizukuHelper.executeCommand("which docker 2>/dev/null && docker info >/dev/null 2>&1 && echo ok || echo no")
        available = res.stdout.contains("ok"); return available
    }
    override fun onStop(): Boolean { return true }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try {
            when (action) {
                "ps" -> { val res = ShizukuHelper.executeCommand("docker ps --format '{{json .}}' 2>/dev/null || echo []"); r.put("containers", res.stdout) }
                "start" -> { val j = JSONObject(body); ShizukuHelper.executeCommand("docker start ${j.optString("id")} 2>/dev/null"); r.put("success", true) }
                "stop" -> { val j = JSONObject(body); ShizukuHelper.executeCommand("docker stop ${j.optString("id")} 2>/dev/null"); r.put("success", true) }
                "run" -> { val j = JSONObject(body); ShizukuHelper.executeCommand("docker run -d ${j.optString("args","")} ${j.optString("image")} 2>/dev/null"); r.put("success", true) }
                "rm" -> { val j = JSONObject(body); ShizukuHelper.executeCommand("docker rm -f ${j.optString("id")} 2>/dev/null"); r.put("success", true) }
                "logs" -> { val j = JSONObject(body); val res = ShizukuHelper.executeCommand("docker logs --tail 100 ${j.optString("id")} 2>&1"); r.put("logs", res.stdout) }
                "pull" -> { val j = JSONObject(body); ShizukuHelper.executeCommand("docker pull ${j.optString("image")} 2>/dev/null"); r.put("success", true) }
                "images" -> { val res = ShizukuHelper.executeCommand("docker images --format '{{json .}}' 2>/dev/null || echo []"); r.put("images", res.stdout) }
                "status" -> { onStart(); r.put("available", available) }
                else -> r.put("success", false).put("error", "Unknown")
            }
        } catch (e: Exception) { r.put("success", false).put("error", e.message) }
        return r
    }
}
