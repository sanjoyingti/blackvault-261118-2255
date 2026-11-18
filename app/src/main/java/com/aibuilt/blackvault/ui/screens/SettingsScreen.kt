package com.aibuilt.blackvault.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.data.CustomTarget
import com.aibuilt.blackvault.data.SecureStore
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.ui.components.MonoLabel
import com.aibuilt.blackvault.ui.components.NoteBanner
import com.aibuilt.blackvault.ui.components.SectionCard
import com.aibuilt.blackvault.ui.components.TerminalField
import com.aibuilt.blackvault.ui.components.VaultTopBar
import com.aibuilt.blackvault.ui.theme.VaultAmber
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val keys by vm.keys.collectAsState()
    val motion by vm.motion.collectAsState()
    val webhook by vm.webhook.collectAsState()
    val vpa by vm.vpa.collectAsState()
    val targets by vm.targets.collectAsState()
    val shredded by vm.shredded.collectAsState()

    var confirmShred by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("SETTINGS", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NoteBanner(
                text = "Keys are stored in EncryptedSharedPreferences. Nothing sensitive is kept in plain text.",
                color = VaultTextDim
            )

            if (shredded) {
                NoteBanner(text = "ALL LOGS SHREDDED — Room wiped, encrypted records cleared", color = VaultGreen)
            }

            SectionCard(title = "INDIA RAIL — RAZORPAY X", accent = if (keys.indiaReady) VaultGreen else VaultAmber) {
                SecretField("Razorpay key id", keys.razorpayId) { vm.save(SecureStore.K_RAZORPAY_ID, it) }
                Spacer(Modifier.height(8.dp))
                SecretField("Razorpay key secret", keys.razorpaySecret) { vm.save(SecureStore.K_RAZORPAY_SECRET, it) }
                Spacer(Modifier.height(8.dp))
                SecretField("Razorpay X source account number", keys.razorpayAccount) {
                    vm.save(SecureStore.K_RAZORPAY_ACC, it)
                }
                Spacer(Modifier.height(8.dp))
                MonoLabel("— or use Cashfree instead —", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                SecretField("Cashfree client id", keys.cashfreeId) { vm.save(SecureStore.K_CASHFREE_ID, it) }
                Spacer(Modifier.height(8.dp))
                SecretField("Cashfree client secret", keys.cashfreeSecret) { vm.save(SecureStore.K_CASHFREE_SECRET, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.indiaReady) "INDIA CORRIDOR LIVE" else "INDIA CORRIDOR NOT LIVE", VaultTextDim)
            }

            SectionCard(title = "US RAIL — STRIPE ACH", accent = if (keys.usReady) VaultGreen else VaultAmber) {
                SecretField("Stripe secret key", keys.stripeKey) { vm.save(SecureStore.K_STRIPE, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.usReady) "US CORRIDOR LIVE" else "US CORRIDOR NOT LIVE", VaultTextDim)
            }

            SectionCard(title = "PAKISTAN RAIL — PAYFAST", accent = if (keys.pkReady) VaultGreen else VaultAmber) {
                SecretField("PayFast merchant id", keys.payfastMerchant) { vm.save(SecureStore.K_PAYFAST_MERCHANT, it) }
                Spacer(Modifier.height(8.dp))
                SecretField("PayFast secured key", keys.payfastSecured) { vm.save(SecureStore.K_PAYFAST_SECURED, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.pkReady) "PAKISTAN CORRIDOR LIVE" else "PAKISTAN CORRIDOR NOT LIVE", VaultTextDim)
            }

            SectionCard(title = "UPI DEEP-LINK FALLBACK") {
                LocalText(
                    label = "Your VPA",
                    initial = vpa,
                    hint = "e.g. name@okhdfcbank — fires upi:// with the sheet",
                    onValue = { vm.saveVpa(it) }
                )
            }

            SectionCard(title = "STEALTH PANEL") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MonoLabel("PANEL MOTION", VaultTextDim)
                    Switch(
                        checked = motion,
                        onCheckedChange = { vm.setMotion(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultBlack,
                            checkedTrackColor = VaultGreen,
                            uncheckedThumbColor = VaultTextDim,
                            uncheckedTrackColor = VaultBlack
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                LocalText(
                    label = "Webhook URL",
                    initial = webhook,
                    hint = "optional — POSTed with each finished run",
                    onValue = { vm.setWebhook(it) }
                )
            }

            SectionCard(title = "CUSTOM TARGETS") {
                var name by remember { mutableStateOf("") }
                var host by remember { mutableStateOf("") }
                var hint by remember { mutableStateOf("") }
                var rail by remember { mutableStateOf(Rail.INDIA_IMPS) }

                TerminalField("Bank name", name, { name = it })
                Spacer(Modifier.height(8.dp))
                TerminalField("Host / domain", host, { host = it.trim() }, hint = "recon runs against this real host")
                Spacer(Modifier.height(8.dp))
                TerminalField("Match hint", hint, { hint = it.uppercase() }, hint = "IFSC prefix, PK IBAN code or ABA")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Rail.entries.forEach { r ->
                        OutlinedButton(
                            onClick = { rail = r },
                            border = BorderStroke(1.dp, if (rail == r) VaultGreen else VaultTextDim),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                r.currency,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rail == r) VaultGreen else VaultTextDim,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        vm.addTarget(name, host, rail, hint)
                        name = ""
                        host = ""
                        hint = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ADD TARGET", fontFamily = FontFamily.Monospace)
                }

                if (targets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    targets.forEach { t -> TargetRow(t) { vm.removeTarget(t.host) } }
                }
            }

            SectionCard(title = "DANGER ZONE", accent = VaultRed) {
                MonoLabel("Wipes Room, tranches, custom targets and every stored key.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmShred = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SHRED ALL LOGS", fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmShred) {
        AlertDialog(
            onDismissRequest = { confirmShred = false },
            containerColor = VaultBlack,
            title = { Text("SHRED EVERYTHING?", fontFamily = FontFamily.Monospace, color = VaultGreen) },
            text = {
                Text(
                    "History, tranches, custom targets and saved gateway keys are removed.",
                    fontFamily = FontFamily.Monospace,
                    color = VaultTextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.shred()
                    confirmShred = false
                }) { Text("SHRED", color = VaultRed, fontFamily = FontFamily.Monospace) }
            },
            dismissButton = {
                TextButton(onClick = { confirmShred = false }) {
                    Text("CANCEL", color = VaultTextDim, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun SecretField(label: String, initial: String, onValue: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    TerminalField(
        label = label,
        value = value,
        onChange = {
            value = it
            onValue(it)
        },
        transformation = PasswordVisualTransformation(),
        hint = if (initial.isBlank()) "not set" else "stored encrypted"
    )
}

@Composable
private fun LocalText(label: String, initial: String, hint: String, onValue: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    TerminalField(
        label = label,
        value = value,
        onChange = {
            value = it
            onValue(it)
        },
        hint = hint
    )
}

@Composable
private fun TargetRow(target: CustomTarget, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VaultBlack, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                target.name,
                style = MaterialTheme.typography.bodySmall,
                color = VaultGreen,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("${target.host} // ${target.rail} // ${target.keyHint}", VaultTextDim)
        }
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack),
            shape = RoundedCornerShape(3.dp)
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove target",
                tint = VaultBlack
            )
        }
    }
}
