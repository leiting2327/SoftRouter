package com.softrouter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.softrouter.app.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var logBuilder = StringBuilder()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) appendLog("通知权限已授予")
        else appendLog("通知权限被拒绝，前台服务可能受限")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
        initShizuku()
        setupUI()
        updateStatus()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initShizuku() {
        ShizukuHelper.init()
        scope.launch(Dispatchers.IO) {
            Thread.sleep(1000)
            withContext(Dispatchers.Main) {
                val available = ShizukuHelper.isShizukuAvailable()
                binding.tvShizukuStatus.text = if (available) "✅ Shizuku 已连接" else "❌ Shizuku 未连接"
                if (available) {
                    ShizukuHelper.checkOrRequestPermission()
                    appendLog("Shizuku 服务可用，正在请求权限...")
                } else {
                    appendLog("Shizuku 不可用，将尝试 Root 模式")
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnStart.setOnClickListener { startRouter() }
        binding.btnStop.setOnClickListener { stopRouter() }
        binding.btnOpenDashboard.setOnClickListener { openDashboard() }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.switchRouter.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startRouter() else stopRouter()
        }
    }

    private fun startRouter() {
        scope.launch(Dispatchers.IO) {
            appendLog("正在启动软路由服务...")

            val hasPerm = ShizukuHelper.hasPermission() || ShizukuHelper.isShizukuAvailable().also {
                if (it) ShizukuHelper.checkOrRequestPermission()
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, RouterService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, intent)

                binding.switchRouter.isChecked = true
                binding.tvRouterStatus.text = "🟢 运行中"
                binding.tvDashboardUrl.text = "http://${getLocalIp()}:8080"
                appendLog("软路由已启动，管理后台: http://${getLocalIp()}:8080")
                Toast.makeText(this@MainActivity, "路由已启动", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRouter() {
        scope.launch(Dispatchers.IO) {
            appendLog("正在停止软路由...")
            ShizukuHelper.executeCommand("killall dnsmasq 2>/dev/null; iptables -t nat -F POSTROUTING; iptables -F FORWARD; sysctl -w net.ipv4.ip_forward=0")

            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, RouterService::class.java)
                stopService(intent)

                binding.switchRouter.isChecked = false
                binding.tvRouterStatus.text = "🔴 已停止"
                binding.tvDashboardUrl.text = "未启动"
                RouterState.isRunning = false
                appendLog("软路由已停止")
                Toast.makeText(this@MainActivity, "路由已停止", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDashboard() {
        val ip = getLocalIp()
        val url = "http://$ip:8080"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus() {
        val running = RouterService.isServiceRunning
        binding.switchRouter.isChecked = running
        binding.tvRouterStatus.text = if (running) "🟢 运行中" else "🔴 已停止"
        binding.tvDashboardUrl.text = if (running) "http://${getLocalIp()}:8080" else "未启动"
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            logBuilder.appendLine("[$time] $msg")
            binding.tvLog.text = logBuilder.toString()
        }
    }

    private fun getLocalIp(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    override fun onDestroy() {
        ShizukuHelper.destroy()
        scope.cancel()
        super.onDestroy()
    }
}

</content>