package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray
class TrafficSplitModule : RouterModule {
    override val id = "trafficsplit"; override val name = "Traffic Split"; override val category = "routing"
    companion object { var rules = mutableListOf<JSONObject>(); var enabled = false }
    override fun isEnabled() = enabled
    override fun onStart(): Boolean { enabled = true; return true }
    override fun onStop(): Boolean { enabled = false; return true }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "addRule"->{val j=JSONObject(body);rules.add(j);val s=j.optString("source","");val d=j.optString("destination","");val t=j.optString("table","100");if(s.isNotEmpty())ShizukuHelper.executeCommand("ip rule add from $s table $t 2>/dev/null||true");if(d.isNotEmpty())ShizukuHelper.executeCommand("ip rule add to $d table $t 2>/dev/null||true");r.put("success",true)}; "removeRule"->{val j=JSONObject(body);if(j.getInt("index")<rules.size)rules.removeAt(j.getInt("index"));r.put("success",true)}; "status"->{r.put("enabled",enabled).put("rules",JSONArray(rules.toList()))}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
