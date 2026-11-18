=== ABOUT: The opening screen — green terminal boot lines about this real phone and network, then it moves on ===
package com.aibuilt.blackvault.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.SplashViewModel

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val vm: SplashViewModel = viewModel()
    val lines by vm.lines.collectAsState()

    LaunchedEffect(Unit) { vm.boot() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2600)
        onDone()
    }

    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "cursorAlpha"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .padding(20.dp)
    ) {
        Text(
            "BLACKVAULT",
            style = MaterialTheme.typography.displayMedium,
            color = VaultGreen,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "v1.0 // SERVER RECON + LIVE PAYOUT",
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
        LazyColumn(
            modifier = Modifier.padding(top = 24.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lines) { line ->
                Text(
                    "> $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
            item {
                Text(
                    "_",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.graphicsLayer { alpha = blink }
                )
            }
        }
        Text(
            "device ${android.os.Build.MODEL} / checked at ${context.packageName}",
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
    }
}
