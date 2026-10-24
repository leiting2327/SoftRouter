package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray
class MonitorModule : RouterModule {
    override val id = "monitor"; override val name = "System Monitor"; override val category = "tools"
    override fun isEnabled() = true; override fun onStart() = true; override fun onStop() = true
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "traffic"->{r.put("data",ShizukuHelper.executeCommand("cat /proc/net/dev|tail -n +3").stdout).put("success",true)}; "connections"->{r.put("count",ShizukuHelper.executeCommand("cat /proc/net/tcp|wc -l").stdout.trim().toIntOrNull()?:0).put("success",true)}; "cpu"->{r.put("data",ShizukuHelper.executeCommand("cat /proc/stat|head -1").stdout).put("success",true)}; "memory"->{r.put("data",ShizukuHelper.executeCommand("cat /proc/meminfo|head -5").stdout).put("success",true)}; "clients"->{val res=ShizukuHelper.executeCommand("cat /proc/net/arp|tail -n +2");val arr=JSONArray();res.stdout.split("\n").filter{it.isNotBlank()}.forEach{l->val p=l.trim().split("\\s+".toRegex());if(p.size>=6){val o=JSONObject();o.put("ip",p[0]).put("mac",p[3]).put("iface",p[5]);arr.put(o)}};r.put("clients",arr).put("success",true)}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
