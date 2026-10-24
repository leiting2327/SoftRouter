package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
class Aria2Module : RouterModule {
    override val id = "aria2"; override val name = "Aria2 Download"; override val category = "services"
    companion object { var running = false; var rpcPort = 6800; var secret = "softrouter" }
    override fun isEnabled() = running
    override fun onStart(): Boolean { ShizukuHelper.executeCommand("aria2c --enable-rpc --rpc-listen-all --rpc-port=$rpcPort --rpc-secret=$secret -D 2>/dev/null||true");running=true;return true }
    override fun onStop(): Boolean { ShizukuHelper.executeCommand("killall aria2c 2>/dev/null||true");running=false;return true }
    override fun handleAction(action: String, body: String): JSONObject {
        val r = JSONObject()
        try { when(action) { "addUri"->{val j=JSONObject(body);val u=j.getString("uri");val d=j.optString("dir","/sdcard/Download");r.put("result",ShizukuHelper.executeCommand("curl -s http://127.0.0.1:$rpcPort/jsonrpc -d '{\"jsonrpc\":\"2.0\",\"method\":\"aria2.addUri\",\"id\":1,\"params\":[\"token:$secret\",[\"$u\"],{\"dir\":\"$d\"}]}'").stdout).put("success",true)}; "listActive"->{r.put("result",ShizukuHelper.executeCommand("curl -s http://127.0.0.1:$rpcPort/jsonrpc -d '{\"jsonrpc\":\"2.0\",\"method\":\"aria2.tellActive\",\"id\":1,\"params\":[\"token:$secret\"]}'").stdout).put("success",true)}; "pause"->{val j=JSONObject(body);ShizukuHelper.executeCommand("curl -s http://127.0.0.1:$rpcPort/jsonrpc -d '{\"jsonrpc\":\"2.0\",\"method\":\"aria2.pause\",\"id\":1,\"params\":[\"token:$secret\",\"${j.getString("gid")}\"]}'");r.put("success",true)}; "status"->{r.put("running",running).put("rpcPort",rpcPort)}; else->r.put("success",false).put("error","Unknown") } } catch(e:Exception){r.put("success",false).put("error",e.message)}
        return r
    }
}
