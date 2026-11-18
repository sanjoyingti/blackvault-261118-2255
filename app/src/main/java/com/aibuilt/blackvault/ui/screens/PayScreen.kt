=== ABOUT: The main screen — account, IFSC or IBAN fields, locked amount, detected bank banner and the big PAY button ===
package com.aibuilt.blackvault.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner as ComposeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.ui.components.MonoLabel
import com.aibuilt.blackvault.ui.components.NoteBanner
import com.aibuilt.blackvault.ui.components.TerminalField
import com.aibuilt.blackvault.ui.components.VaultTopBar
import com.aibuilt.blackvault.ui.theme.VaultAmber
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultCard
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultGrey
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.PayUi
import com.aibuilt.blackvault.viewmodel.PayViewModel

@Composable
fun PayScreen(
    onRun: (String) -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit
) {
    val vm: PayViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val lifecycle = ComposeLifecycleOwner.current
    var inline by remember { mutableStateOf<PayUi?>(null) }

    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshKeys()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            VaultTopBar("BLACKVAULT // PAYOUT") {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.History, contentDescription = "History", tint = VaultGreen)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = VaultGreen)
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AmountLockCard()

            Spacer(Modifier.height(16.dp))

            TerminalField(
                label = "Account number",
                value = ui.account,
                onChange = vm::onAccount,
                error = inline?.accountError,
                hint = "Digits only. A PK IBAN may also be typed here.",
                keyboard = KeyboardType.Number
            )

            if (ui.showIban) {
                Spacer(Modifier.height(12.dp))
                TerminalField(
                    label = "IBAN (Pakistan)",
                    value = ui.iban,
                    onChange = vm::onIban,
                    error = inline?.ibanError,
                    hint = "PK + 2 check digits + 4 bank letters + 16 digits"
                )
            } else {
                Spacer(Modifier.height(12.dp))
                if (ui.showAba) {
                    TerminalField(
                        label = "Routing number (ABA)",
                        value = ui.aba,
                        onChange = vm::onAba,
                        error = inline?.abaError,
                        hint = "9 digits — US ACH rail detected",
                        keyboard = KeyboardType.Number
                    )
                } else {
                    TerminalField(
                        label = "IFSC code",
                        value = ui.ifsc,
                        onChange = vm::onIfsc,
                        error = inline?.ifscError,
                        hint = "Indian IFSC, e.g. HDFC0000123"
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val det = ui.detection
            if (det != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(VaultGreen.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                        .border(BorderStroke(1.dp, VaultGreen.copy(alpha = 0.6f)), RoundedCornerShape(4.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        MonoLabel("DETECTED TARGET", VaultGreen)
                        Text(
                            det.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultText,
                            fontFamily = FontFamily.Monospace
                        )
                        MonoLabel(
                            "RAIL ${det.rail.label} // ${det.host ?: "no host mapping — generic path"}",
                            VaultTextDim
                        )
                    }
                }
            } else {
                NoteBanner(
                    text = "NO TARGET YET — type an account number with an IFSC or routing number",
                    color = VaultTextDim
                )
            }

            if (ui.keyMissing) {
                Spacer(Modifier.height(10.dp))
                NoteBanner(
                    text = "NO FUNDING KEY — transfer not live",
                    trailing = {
                        TextButton(onClick = onSettings) {
                            Text(
                                "SETTINGS",
                                color = VaultAmber,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = { vm.submit(onRun) { invalid -> inline = invalid } },
                enabled = ui.canPay && !ui.creating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultGreen,
                    contentColor = VaultBlack,
                    disabledContainerColor = VaultGrey,
                    disabledContentColor = VaultTextDim
                )
            ) {
                if (ui.creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VaultBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    "PAY $10,000.00",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            val upi = vm.upiLink()
            if (upi != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upi))) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "UPI DEEP-LINK FALLBACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                MonoLabel("UPI fallback off — add a VPA in Settings", VaultTextDim)
            }

            Spacer(Modifier.height(20.dp))
            MonoLabel(
                "Recon hits the detected bank's own servers. Payout uses your gateway key. " +
                    "Money leaves your funding account — it is transferred, not conjured.",
                VaultTextDim
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AmountLockCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            MonoLabel("AMOUNT (LOCKED)")
            Text(
                "$10,000.00",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                color = VaultGreen,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("USD — converted at live mid-rate per rail", VaultTextDim)
        }
        Icon(
            Icons.Filled.Lock,
            contentDescription = "Locked amount",
            tint = VaultAmber,
            modifier = Modifier.size(28.dp)
        )
    }
}
