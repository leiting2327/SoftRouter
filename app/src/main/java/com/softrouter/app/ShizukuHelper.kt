package com.softrouter.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {
    private var binderReceived = false
    private var permissionGranted = false

    val onBinderReceivedListener = Shizuku.OnBinderReceivedListener { binderReceived = true }
    val onBinderDeadListener = Shizuku.OnBinderDeadListener { binderReceived = false; permissionGranted = false }
    val onRequestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
    }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener)
        Shizuku.addBinderDeadListener(onBinderDeadListener)
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener)
        Shizuku.removeBinderDeadListener(onBinderDeadListener)
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener)
    }

    fun isShizukuAvailable(): Boolean = binderReceived && Shizuku.pingBinder()

    fun checkOrRequestPermission(): Boolean {
        if (!binderReceived) return false
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true; return true
        }
        Shizuku.requestPermission(0)
        return false
    }

    fun hasPermission(): Boolean = permissionGranted ||
        (binderReceived && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)

    fun executeCommand(cmd: String): CommandResult {
        return try {
            if (isShizukuAvailable() && hasPermission()) executeViaShizuku(cmd)
            else executeViaSu(cmd)
        } catch (e: Exception) { CommandResult(-1, "", "Exception: ${e.message}") }
    }

    private fun executeViaShizuku(cmd: String): CommandResult {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
        return CommandResult(process.waitFor(), stdout.trim(), stderr.trim())
    }

    private fun executeViaSu(cmd: String): CommandResult {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
        return CommandResult(process.waitFor(), stdout.trim(), stderr.trim())
    }

    data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val success get() = exitCode == 0
    }
}

</content>