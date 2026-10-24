package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray
class PortForwardModule : RouterModule {
    override val id = "portforward"; override val name = "Port Forwarding"; override val category = "network"
    companion object { var rules = mutableListOf<JSONObject>(); var dmzIp = "" }
    override fun isEnabled() = rules.isNotEmpty()
    override fun onStart(): Boolean { applyRules(); return true }
    override fun onStop(): Boolean { ShizukuHelper.executeCommand("iptables -t nat -F PREROUTING 2>/dev/null || true"); return true }
    private fun applyRules() {
        rules.forEach { r -> val e=r.optString("extPort");val i=r.optString("intIp");val p=r.optString("intPort",e);val pr=r.optString("proto","tcp"); ShizukuHelper.executeCommand("iptables -t nat -A PREROUTING -p $pr --dport $e -j DNAT --to-destination $i:$p 2>/dev/null||true") }
        if (dmzIp.isNotEmpty()) ShizukuHelper.executeCommand("iptables -t nat -A PREROUTING -j DNAT --to-destination $dmzIp 2>/dev/null||true")
    }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "addRule"->{rules.add(JSONObject(body));applyRules();r.put("success",true)}; "removeRule"->{val j=JSONObject(body);if(j.getInt("index")<rules.size)rules.removeAt(j.getInt("index"));onStop();applyRules();r.put("success",true)}; "setDmz"->{dmzIp=JSONObject(body).optString("ip","");applyRules();r.put("success",true)}; "status"->{r.put("rules",JSONArray(rules.toList())).put("dmz",dmzIp)}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
