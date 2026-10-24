package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
class DiagnosticsModule : RouterModule {
    override val id = "diagnostics"; override val name = "Diagnostics"; override val category = "tools"
    override fun isEnabled() = true; override fun onStart() = true; override fun onStop() = true
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { val j=if(body.isNotEmpty())JSONObject(body)else JSONObject(); when(action) { "ping"->{val h=j.optString("host","8.8.8.8");val c=j.optInt("count",4);val res=ShizukuHelper.executeCommand("ping -c $c $h 2>&1");r.put("output",res.stdout+"\n"+res.stderr).put("success",true)}; "traceroute"->{val res=ShizukuHelper.executeCommand("traceroute -n ${j.optString("host","8.8.8.8")} 2>&1||tracepath ${j.optString("host","8.8.8.8")} 2>&1");r.put("output",res.stdout+"\n"+res.stderr).put("success",true)}; "dnsquery"->{val res=ShizukuHelper.executeCommand("nslookup ${j.optString("domain","example.com")} ${j.optString("server","8.8.8.8")} 2>&1");r.put("output",res.stdout+"\n"+res.stderr).put("success",true)}; "tcpdump"->{val res=ShizukuHelper.executeCommand("tcpdump -i ${j.optString("interface","wlan0")} -c ${j.optInt("count",100)} -nn 2>&1||echo tcpdump_not_available");r.put("output",res.stdout+"\n"+res.stderr).put("success",true)}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
