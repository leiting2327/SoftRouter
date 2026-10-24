package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
import org.json.JSONArray
class SambaModule : RouterModule {
    override val id = "samba"; override val name = "Samba Share"; override val category = "services"
    companion object { var shares = mutableListOf<JSONObject>(); var running = false }
    override fun isEnabled() = running
    override fun onStart(): Boolean { val c="[global]\nworkgroup=WORKGROUP\nserver string=SoftRouter\nsecurity=user\nmap to guest=Bad User\n";shares.forEachIndexed{i,s->c.plus("[Share$i]\npath=${s.optString("path","/sdcard")}\nread only=no\nguest ok=yes\n")};ShizukuHelper.executeCommand("mkdir -p /data/local/tmp");ShizukuHelper.executeCommand("smbd --configfile=/data/local/tmp/smb.conf -D 2>/dev/null||true");running=true;return true }
    override fun onStop(): Boolean { ShizukuHelper.executeCommand("killall smbd 2>/dev/null||true");running=false;return true }
    override fun handleAction(action: String, body: String): JSONObject { val r=JSONObject();try{when(action){"addShare"->{shares.add(JSONObject(body));if(running){onStop();onStart()};r.put("success",true)};"removeShare"->{val j=JSONObject(body);if(j.getInt("index")<shares.size)shares.removeAt(j.getInt("index"));r.put("success",true)};"status"->{r.put("running",running).put("shares",JSONArray(shares.toList()))};else->r.put("success",false).put("error","Unknown")}}catch(e:Exception){r.put("success",false).put("error",e.message)};return r }
}
