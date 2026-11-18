=== ABOUT: The live console — real probes against the bank, plus the stealth panel, progress bar and ABORT ===
package com.aibuilt.blackvault.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.ConsoleLine
import com.aibuilt.blackvault.model.LineLevel
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.ui.components.MonoLabel
import com.aibuilt.blackvault.ui.components.NoteBanner
import com.aibuilt.blackvault.ui.components.VaultTopBar
import com.aibuilt.blackvault.ui.theme.VaultAmber
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultCard
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultGrey
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.RunViewModel

@Composable
fun RunScreen(onReceipt: (String) -> Unit, onBack: () -> Unit) {
    val vm: RunViewModel = viewModel()
    val run by vm.run.collectAsState()
    val lines by vm.lines.collectAsState()
    val progress by vm.progress.collectAsState()
    val blocked by vm.blocked.collectAsState()
    val fakeIp by vm.fakeIp.collectAsState()
    val hops by vm.hops.collectAsState()
    val done by vm.done.collectAsState()
    val motion by vm.motion.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.start() }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem((lines.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            VaultTopBar(
                title = run?.let { "RUN ${it.id.take(6).uppercase()}" } ?: "RUN",
                onBack = onBack
            ) {
                if (!done) {
                    IconButton(onClick = vm::abort) {
                        Icon(Icons.Filled.Close, contentDescription = "Abort", tint = VaultRed)
                    }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp)
        ) {
            run?.let { rec -> TargetHead(rec) }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = VaultGreen,
                trackColor = VaultGrey
            )

            Spacer(Modifier.height(8.dp))

            StealthPanel(
                ip = fakeIp,
                hops = hops,
                blocked = blocked,
                motion = motion
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "-- LIVE CONSOLE (real returned values) --",
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextDim,
                fontFamily = FontFamily.Monospace
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(VaultBlack)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(lines, key = { it.id }) { l -> ConsoleRow(l) }
            }

            if (run?.status == RunStatus.FAILED && run?.message?.startsWith("NO FUNDING") == true) {
                NoteBanner(
                    text = "NO FUNDING KEY — transfer not live",
                    trailing = {
                        TextButton(onClick = onBack) {
                            Text("BACK", color = VaultAmber, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!done) {
                    Button(
                        onClick = vm::abort,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRed,
                            contentColor = VaultBlack
                        )
                    ) { Text("ABORT", fontFamily = FontFamily.Monospace) }
                } else {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("BACK", color = VaultGreen, fontFamily = FontFamily.Monospace)
                    }
                    val rec = run
                    if (rec != null && rmUpiAvailable(vm)) {
                        TextButton(
                            onClick = {
                                val link = vm.upiLink()
                                if (link != null) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                    }
                                }
                            }
                        ) {
                            Text("UPI PAY", color = VaultAmber, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Button(
                        onClick = { rec?.let { onReceipt(it.id) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultGreen,
                            contentColor = VaultBlack
                        )
                    ) { Text("RECEIPT", fontFamily = FontFamily.Monospace) }
                }
            }
        }
    }
}

private fun rmUpiAvailable(vm: RunViewModel): Boolean = vm.upiLink() != null

@Composable
private fun TargetHead(rec: RunRecord) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                rec.bankName,
                style = MaterialTheme.typography.titleMedium,
                color = VaultGreen,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("${rec.rail} // ${rec.accountMasked} // ${rec.host ?: "no host"}")
        }
    }
}

@Composable
private fun ConsoleRow(line: ConsoleLine) {
    val color = when (line.level) {
        LineLevel.OK -> VaultGreen
        LineLevel.WARN -> VaultAmber
        LineLevel.ERR -> VaultRed
        LineLevel.INFO -> VaultTextDim
    }
    Row(Modifier.fillMaxWidth()) {
        Text(
            "[${line.time}] ",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
        Text(
            line.text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StealthPanel(
    ip: String,
    hops: List<String>,
    blocked: Int,
    motion: Boolean
) {
    val transition = rememberInfiniteTransition(label = "stealth")
    val pulse by transition.animateFloat(
        initialValue = if (motion) 0.55f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoLabel("STEALTH LAYER", VaultGreen)
            Text(
                "TRACE ATTEMPTS BLOCKED $blocked",
                style = MaterialTheme.typography.labelSmall,
                color = VaultRed,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "SPOOF IP $ip",
            style = MaterialTheme.typography.bodySmall,
            color = VaultGreen,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.graphicsLayer { alpha = pulse }
        )
        if (hops.isNotEmpty()) {
            Text(
                "7-HOP CHAIN " + hops.take(3).joinToString(" > ") { it.split(" ").first() },
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextDim,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        StealthRow("MAC RANDOMISER", "rotating")
        StealthRow("DNS LEAK BLOCKER", "armed")
        StealthRow("TRACE SCRUBBER", "purging logs")
        StealthRow("ANTI-FORENSICS", "active")
    }
}

@Composable
private fun StealthRow(name: String, state: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "> $name",
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
        Text(
            state.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = VaultGreen,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun Unused() {
    CircularProgressIndicator(color = VaultGreen, modifier = Modifier.size(12.dp))
    Icon(Icons.Filled.Close, contentDescription = null)
}
