package com.aisoul.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.aisoul.assistant.service.notification.ChatNotificationListenerService
import com.aisoul.assistant.ui.theme.MyApplicationTheme
import com.aisoul.assistant.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStartService()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayPermission()
        }
    }

    private var isFloatingServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isFloatingServiceBound = true
            viewModel.setFloatingServiceRunning(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isFloatingServiceBound = false
            viewModel.setFloatingServiceRunning(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(
                    mainViewModel = viewModel,
                    onStartFloatingService = { startFloatingService() },
                    onStopFloatingService = { stopFloatingService() }
                )
            }
        }

        // Bind to floating service to check if running
        bindFloatingServiceIfRunning()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        checkNotificationListenerStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFloatingServiceBound) {
            unbindService(serviceConnection)
            isFloatingServiceBound = false
        }
    }

    private fun bindFloatingServiceIfRunning() {
        val intent = Intent(this, FloatingService::class.java)
        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            // Service not running
        }
    }

    private fun checkNotificationListenerStatus() {
        val componentName = ComponentName(this, ChatNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val isEnabled = enabledListeners?.contains(componentName.flattenToString()) == true
        viewModel.refreshState()
    }

    private fun requestAllPermissions() {
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                startFloatingService()
            }
        } else {
            startFloatingService()
        }
    }

    private fun checkPermissionsAndStartService() {
        checkOverlayPermission()
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindFloatingServiceIfRunning()
        Toast.makeText(this, "悬浮球服务已开启", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        stopService(intent)
        if (isFloatingServiceBound) {
            unbindService(serviceConnection)
            isFloatingServiceBound = false
        }
        viewModel.setFloatingServiceRunning(false)
        Toast.makeText(this, "悬浮球服务已停止", Toast.LENGTH_SHORT).show()
    }
}
