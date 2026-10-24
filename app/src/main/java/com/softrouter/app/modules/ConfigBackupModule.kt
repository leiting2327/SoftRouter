package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
class ConfigBackupModule : RouterModule {
    override val id = "configbackup"; override val name = "Config Backup"; override val category = "system"
    override fun isEnabled() = true; override fun onStart() = true; override fun onStop() = true
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "backup"->{r.put("output",ShizukuHelper.executeCommand("mkdir -p /data/local/tmp/srbak&&iptables-save>/data/local/tmp/srbak/iptables.bak 2>/dev/null;cp /data/local/tmp/*.conf /data/local/tmp/srbak/ 2>/dev/null;echo done").stdout).put("success",true)}; "restore"->{r.put("output",ShizukuHelper.executeCommand("iptables-restore</data/local/tmp/srbak/iptables.bak 2>/dev/null;echo done").stdout).put("success",true)}; "validate"->{val res=ShizukuHelper.executeCommand("iptables-restore --test</data/local/tmp/srbak/iptables.bak 2>&1&&echo VALID||echo INVALID");r.put("valid",res.stdout.contains("VALID")).put("output",res.stdout)}; "list"->{r.put("files",ShizukuHelper.executeCommand("ls -la /data/local/tmp/srbak/ 2>/dev/null||echo no_backups").stdout).put("success",true)}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
