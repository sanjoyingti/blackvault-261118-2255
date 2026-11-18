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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.aibuilt.blackvault.ui.theme.VaultCard
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultGrey
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val keys by vm.keys.collectAsState()
    val motion by vm.motion.collectAsState()
    val webhook by vm.webhook.collectAsState()
    val targets by vm.targets.collectAsState()
    val vpa by vm.vpa.collectAsState()
    val shredded by vm.shredded.collectAsState()

    var webhookText by remember(webhook) { mutableStateOf(webhook) }
    var vpaText by remember(vpa) { mutableStateOf(vpa) }
    var shredAsk by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newHost by remember { mutableStateOf("") }
    var newHint by remember { mutableStateOf("") }
    var newRail by remember { mutableStateOf(Rail.INDIA_IMPS) }

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("SETTINGS // CONFIG", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (shredded) {
                NoteBanner("ALL LOGS SHREDDED — runs, tranches, targets and keys wiped", VaultGreen)
                Spacer(Modifier.height(10.dp))
            }

            NoteBanner(
                "Keys are stored in EncryptedSharedPreferences and are never shown in full. " +
                    "Without a key for a rail the payout stays offline.",
                VaultAmber
            )

            Spacer(Modifier.height(14.dp))

            SectionCard("INDIA — RAZORPAY X (IMPS/NEFT)", accent = if (keys.indiaReady) VaultGreen else VaultAmber) {
                KeyField("Key ID", keys.razorpayId) { vm.save(SecureStore.K_RAZORPAY_ID, it) }
                Spacer(Modifier.height(8.dp))
                KeyField("Key secret", keys.razorpaySecret) { vm.save(SecureStore.K_RAZORPAY_SECRET, it) }
                Spacer(Modifier.height(8.dp))
                KeyField("Source account number", keys.razorpayAccount, numeric = true) {
                    vm.save(SecureStore.K_RAZORPAY_ACC, it)
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("INDIA — CASHFREE (alternative)") {
                KeyField("Client ID", keys.cashfreeId) { vm.save(SecureStore.K_CASHFREE_ID, it) }
                Spacer(Modifier.height(8.dp))
                KeyField("Client secret", keys.cashfreeSecret) { vm.save(SecureStore.K_CASHFREE_SECRET, it) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("US — STRIPE ACH", accent = if (keys.usReady) VaultGreen else VaultAmber) {
                KeyField("Secret key (sk_live_…)", keys.stripeKey) { vm.save(SecureStore.K_STRIPE, it) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("PAKISTAN — PAYFAST", accent = if (keys.pkReady) VaultGreen else VaultAmber) {
                KeyField("Merchant ID", keys.payfastMerchant) { vm.save(SecureStore.K_PAYFAST_MERCHANT, it) }
                Spacer(Modifier.height(8.dp))
                KeyField("Secured key", keys.payfastSecured) { vm.save(SecureStore.K_PAYFAST_SECURED, it) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("UPI DEEP-LINK FALLBACK") {
                TerminalField(
                    label = "Your VPA",
                    value = vpaText,
                    onChange = { vpaText = it },
                    hint = "e.g. name@okhdfcbank — used by the UPI hand-off button"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.saveVpa(vpaText) },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                ) { Text("SAVE VPA", fontFamily = FontFamily.Monospace) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("STEALTH PANEL") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "PANEL MOTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultText,
                            fontFamily = FontFamily.Monospace
                        )
                        MonoLabel("animated pulse on the run screen", VaultTextDim)
                    }
                    Switch(
                        checked = motion,
                        onCheckedChange = vm::setMotion,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultBlack,
                            checkedTrackColor = VaultGreen,
                            uncheckedThumbColor = VaultTextDim,
                            uncheckedTrackColor = VaultGrey
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("WEBHOOK (optional)") {
                TerminalField(
                    label = "POST URL",
                    value = webhookText,
                    onChange = { webhookText = it },
                    keyboard = KeyboardType.Uri,
                    hint = "Fired on tranche completion with the reference"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.setWebhook(webhookText) },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                ) { Text("SAVE WEBHOOK", fontFamily = FontFamily.Monospace) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("CUSTOM TARGETS") {
                if (targets.isEmpty()) {
                    MonoLabel("No custom targets yet", VaultTextDim)
                    Spacer(Modifier.height(8.dp))
                } else {
                    targets.forEach { t -> TargetRow(t) { vm.removeTarget(t.host) } }
                    Spacer(Modifier.height(8.dp))
                }
                TerminalField(
                    label = "Bank name",
                    value = newName,
                    onChange = { newName = it }
                )
                Spacer(Modifier.height(8.dp))
                TerminalField(
                    label = "Domain",
                    value = newHost,
                    onChange = { newHost = it },
                    keyboard = KeyboardType.Uri,
                    hint = "e.g. bank.example"
                )
                Spacer(Modifier.height(8.dp))
                TerminalField(
                    label = "Prefix / IBAN code / ABA",
                    value = newHint,
                    onChange = { newHint = it },
                    hint = "4-letter IFSC prefix, IBAN bank code or routing number"
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Rail.entries.forEach { rail ->
                        FilterChip(
                            selected = newRail == rail,
                            onClick = { newRail = rail },
                            label = {
                                Text(
                                    rail.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VaultGreen.copy(alpha = 0.20f),
                                selectedLabelColor = VaultGreen,
                                labelColor = VaultTextDim
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        vm.addTarget(newName, newHost, newRail, newHint)
                        newName = ""
                        newHost = ""
                        newHint = ""
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                ) { Text("ADD TARGET", fontFamily = FontFamily.Monospace) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("DANGER ZONE", accent = VaultRed) {
                Text(
                    "SHRED ALL LOGS",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultRed,
                    fontFamily = FontFamily.Monospace
                )
                MonoLabel("deletes every run, tranche, custom target, stored key and the UPI VPA", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { shredAsk = true },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = VaultBlack)
                    Spacer(Modifier.height(0.dp))
                    Text("  SHRED", fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (shredAsk) {
        AlertDialog(
            onDismissRequest = { shredAsk = false },
            containerColor = VaultCard,
            titleContentColor = VaultRed,
            textContentColor = VaultText,
            title = { Text("SHRED ALL LOGS?", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "Every stored run and key is wiped. Gateways stay configured on their side.",
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.shred()
                    webhookText = ""
                    vpaText = ""
                    shredAsk = false
                }) { Text("SHRED", color = VaultRed, fontFamily = FontFamily.Monospace) }
            },
            dismissButton = {
                TextButton(onClick = { shredAsk = false }) {
                    Text("CANCEL", color = VaultTextDim, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun KeyField(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    TerminalField(
        label = label,
        value = value,
        onChange = onChange,
        keyboard = if (numeric) KeyboardType.Number else KeyboardType.Text,
        transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = VaultGreen
                )
            }
        }
    )
}

@Composable
private fun TargetRow(target: CustomTarget, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VaultBlack, RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                target.name,
                style = MaterialTheme.typography.bodySmall,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("${target.host} // ${target.rail} // ${target.keyHint}", VaultTextDim)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = VaultRed)
        }
    }
}
