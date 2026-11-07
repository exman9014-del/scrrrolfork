package com.example

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.ScrollSearchStateBus
import com.example.service.FloatingOverlayService
import com.example.service.InstagramAccessibilityService
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.ScrollSearchTheme
import com.example.util.ImageDownloadHelper

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)
    private var hasStoragePermission by mutableStateOf(false)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            Toast.makeText(this, "Permissão de armazenamento concedida! Downloads liberados.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollSearchTheme {
                DashboardScreen(
                    isOverlayPermissionGranted = hasOverlayPermission,
                    isAccessibilityServiceEnabled = hasAccessibilityPermission,
                    isStoragePermissionGranted = hasStoragePermission,
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                    onRequestStoragePermission = { requestStoragePermission() },
                    onLaunchInstagram = { launchInstagram() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        if (hasOverlayPermission && ScrollSearchStateBus.isOverlayActive.value) {
            FloatingOverlayService.startService(this)
        }
    }

    private fun checkPermissions() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        hasAccessibilityPermission = isAccessibilityServiceRunning()
        hasStoragePermission = ImageDownloadHelper.hasStoragePermission(this)
    }

    private fun isAccessibilityServiceRunning(): Boolean {
        // 1. Direct active instance check
        if (com.example.service.TikTokAccessibilityService.isServiceRunning) {
            return true
        }

        // 2. Canonical system setting check (preserves true status across app close/open)
        try {
            val accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (!settingValue.isNullOrBlank()) {
                    val splitter = android.text.TextUtils.SimpleStringSplitter(':')
                    splitter.setString(settingValue)
                    while (splitter.hasNext()) {
                        val accessService = splitter.next()
                        if (accessService.contains(packageName, ignoreCase = true) &&
                            (accessService.contains("TikTokAccessibilityService", ignoreCase = true) ||
                             accessService.contains("InstagramAccessibilityService", ignoreCase = true))
                        ) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 3. AccessibilityManager enabled services list check
        try {
            val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?: am.getEnabledAccessibilityServiceList(-1)
            if (enabledServices != null) {
                return enabledServices.any {
                    it.id.contains(packageName, ignoreCase = true) ||
                    it.resolveInfo?.serviceInfo?.name?.contains("TikTokAccessibilityService") == true ||
                    it.resolveInfo?.serviceInfo?.name?.contains("InstagramAccessibilityService") == true
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Ative a permissão de sobreposição para o TikTokLens", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permissão de sobreposição já concedida!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Localize 'TikTokLens Detector de Vídeos' e ative nas Configurações de Acessibilidade",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            hasStoragePermission = true
            Toast.makeText(this, "No Android 10+, fotos são salvas diretamente na galeria!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchInstagram() {
        launchTikTok()
    }

    private fun launchTikTok() {
        val pm = packageManager
        val tiktokPackages = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme"
        )
        for (pkg in tiktokPackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/"))
        startActivity(webIntent)
        Toast.makeText(this, "TikTok não instalado. Abrindo no navegador.", Toast.LENGTH_SHORT).show()
    }
}
