package com.softrouter.app

import android.content.Context
import com.softrouter.app.engine.RouterEngine
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class WebDashboardServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val headers = mutableMapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type"
        )
        if (method == Method.OPTIONS) {
            return respond(Response.Status.OK, MIME_PLAINTEXT, "").also { addHeaders(it, headers) }
        }
        val response = try {
            when {
                uri == "/" || uri == "/index.html" -> serveAsset("dashboard/index.html", "text/html; charset=utf-8")
                uri == "/api/status" -> jsonResponse(RouterEngine.getStatus())
                uri == "/api/start" && method == Method.POST -> jsonResponse(RouterEngine.startAll())
                uri == "/api/stop" && method == Method.POST -> jsonResponse(RouterEngine.stopAll())
                uri.startsWith("/api/module/") -> handleModuleApi(uri, method, session)
                else -> respond(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        } catch (e: Exception) {
            jsonResponse(JSONObject().put("error", e.message ?: "Unknown error"))
        }
        addHeaders(response, headers)
        return response
    }

    private fun handleModuleApi(uri: String, method: Method, session: IHTTPSession): Response {
        val parts = uri.removePrefix("/api/module/").split("/", limit = 2)
        val moduleId = parts[0]
        val action = if (parts.size > 1) parts[1] else "status"
        val body = if (method == Method.POST) readBody(session) else "{}"
        val result = RouterEngine.handleApi(moduleId, action, body)
        return jsonResponse(result)
    }

    private fun serveAsset(path: String, mime: String): Response {
        return try {
            val content = context.assets.open(path).bufferedReader().use { it.readText() }
            respond(Response.Status.OK, mime, content)
        } catch (e: Exception) {
            respond(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Asset not found: $path")
        }
    }

    private fun readBody(session: IHTTPSession): String {
        val len = session.headers["content-length"]?.toIntOrNull() ?: return "{}"
        val buf = ByteArray(len)
        session.inputStream.read(buf, 0, len)
        return String(buf, Charsets.UTF_8)
    }

    private fun jsonResponse(obj: JSONObject): Response {
        return respond(Response.Status.OK, "application/json; charset=utf-8", obj.toString())
    }

    private fun respond(status: Response.Status, mime: String, body: String): Response {
        return newFixedLengthResponse(status, mime, body)
    }

    private fun addHeaders(response: Response, headers: Map<String, String>) {
        headers.forEach { (k, v) -> response.addHeader(k, v) }
    }
}
