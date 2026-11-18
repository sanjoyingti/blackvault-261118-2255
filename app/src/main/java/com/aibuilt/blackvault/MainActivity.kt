=== ABOUT: The app's front door — starts the screen flow and asks for notification permission once ===
package com.aibuilt.blackvault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.aibuilt.blackvault.ui.VaultNavHost
import com.aibuilt.blackvault.ui.theme.BlackVaultTheme
import com.aibuilt.blackvault.ui.theme.VaultBlack

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlackVaultTheme {
                val context = LocalContext.current
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(VaultBlack)
                ) {
                    VaultNavHost()
                }
            }
        }
    }
}
