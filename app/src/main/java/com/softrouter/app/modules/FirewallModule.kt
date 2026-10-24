package com.softrouter.app.modules

import com.softrouter.app.engine.RouterModule
import com.softrouter.app.ShizukuHelper
import org.json.JSONObject
import org.json.JSONArray

class FirewallModule : RouterModule {
    override val id = "firewall"
    override val name = "Firewall"
    override val category = "security"

    override fun isEnabled(): Boolean = enabled

    override fun onStart(): Boolean {
        enabled = true
        applyRules()
        return true
    }

    override fun onStop(): Boolean {
        enabled = false
        ShizukuHelper.executeCommand("iptables -F FORWARD")
        return true
    }

    override fun handleAction(action: String, body: String): JSONObject {
        return try {
            when (action) {
                "add" -> {
                    val b = JSONObject(body)
                    val rule = b.getString("rule")
                    rules.add(rule)
                    val out = ShizukuHelper.executeCommand("iptables $rule")
                    JSONObject().put("success", true).put("output", out)
                }
                "delete" -> {
                    val b = JSONObject(body)
                    val index = b.getInt("index")
                    if (index < 0 || index >= rules.size) {
                        throw IndexOutOfBoundsException("Invalid index")
                    }
                    val rule = rules.removeAt(index)
                    val chain = extractChain(rule)
                    val out = if (chain.isNotEmpty()) {
                        val rest = rule.removePrefix("-A $chain ").removePrefix("-I $chain ").removePrefix("-D $chain ")
                        ShizukuHelper.executeCommand("iptables -D $chain $rest")
                    } else ""
                    JSONObject().put("success", true).put("output", out)
                }
                "list" -> JSONObject().put("success", true).put("rules", JSONArray(rules))
                "flush" -> {
                    rules.clear()
                    val out = ShizukuHelper.executeCommand("iptables -F")
                    JSONObject().put("success", true).put("output", out)
                }
                else -> JSONObject().put("success", false).put("error", "Unknown action")
            }
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message)
        }
    }

    private fun extractChain(rule: String): String {
        val parts = rule.trim().split("\\s+".toRegex())
        if (parts.size >= 2 && (parts[0] == "-A" || parts[0] == "-I" || parts[0] == "-D")) {
            return parts[1]
        }
        return ""
    }

    private fun applyRules() {
        for (rule in rules) {
            ShizukuHelper.executeCommand("iptables $rule")
        }
    }

    companion object {
        private var enabled = false
        private val rules = mutableListOf<String>()

        @JvmStatic
        fun toJson(): JSONObject = JSONObject().put("enabled", enabled).put("rules", JSONArray(rules))

        @JvmStatic
        fun fromJson(json: JSONObject) {
            enabled = json.optBoolean("enabled", false)
            rules.clear()
            val arr = json.optJSONArray("rules") ?: return
            for (i in 0 until arr.length()) {
                rules.add(arr.getString(i))
            }
        }
    }
}
