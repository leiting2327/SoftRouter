package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray
class ParentalControlModule : RouterModule {
    override val id = "parental"; override val name = "Parental Control"; override val category = "security"
    companion object { var rules = mutableListOf<JSONObject>(); var enabled = false }
    override fun isEnabled() = enabled
    override fun onStart(): Boolean { enabled = true; return true }
    override fun onStop(): Boolean { enabled = false; return true }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "addSchedule"->{val j=JSONObject(body);rules.add(j);val m=j.optString("mac");val sh=j.optInt("startHour",0);val eh=j.optInt("endHour",24);if(m.isNotEmpty())ShizukuHelper.executeCommand("iptables -A FORWARD -m mac --mac-source $m -m time --timestart ${sh}:00 --timestop ${eh}:00 -j DROP 2>/dev/null||true");r.put("success",true)}; "blockSite"->{val j=JSONObject(body);ShizukuHelper.executeCommand("iptables -A FORWARD -m mac --mac-source ${j.optString("mac")} -d ${j.optString("site")} -j DROP 2>/dev/null||true");r.put("success",true)}; "removeRule"->{val j=JSONObject(body);if(j.getInt("index")<rules.size)rules.removeAt(j.getInt("index"));r.put("success",true)}; "status"->{r.put("enabled",enabled).put("rules",JSONArray(rules.toList()))}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
