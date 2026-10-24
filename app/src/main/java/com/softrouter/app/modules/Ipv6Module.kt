package com.softrouter.app.modules
import com.softrouter.app.ShizukuHelper
import com.softrouter.app.engine.RouterModule
import org.json.JSONObject
class Ipv6Module : RouterModule {
    override val id = "ipv6"; override val name = "IPv6 Support"; override val category = "network"
    companion object { var enabled = false }
    override fun isEnabled() = enabled
    override fun onStart(): Boolean { ShizukuHelper.executeCommand("sysctl -w net.ipv6.conf.all.disable_ipv6=0 2>/dev/null||true");ShizukuHelper.executeCommand("sysctl -w net.ipv6.conf.all.forwarding=1 2>/dev/null||true");ShizukuHelper.executeCommand("ip6tables -P FORWARD ACCEPT 2>/dev/null||true");enabled=true;return true }
    override fun onStop(): Boolean { ShizukuHelper.executeCommand("sysctl -w net.ipv6.conf.all.disable_ipv6=1 2>/dev/null||true");enabled=false;return true }
    override fun handleAction(action: String, body: String): JSONObject { val r=JSONObject();try{when(action){"status"->{r.put("enabled",enabled).put("addresses",ShizukuHelper.executeCommand("ip -6 addr show 2>/dev/null|grep inet6").stdout).put("success",true)};else->r.put("success",false).put("error","Unknown")}}catch(e:Exception){r.put("success",false).put("error",e.message)};return r }
}
