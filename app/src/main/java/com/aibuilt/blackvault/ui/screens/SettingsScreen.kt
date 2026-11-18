=== ABOUT: Settings — masked secure slots for the payout gateway keys, the UPI fallback address, stealth motion, webhook, custom bank targets and the shred button ===
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

    var confirmShred by remember { mutableStateOf(false) }
    var webhookText by remember(webhook) { mutableStateOf(webhook) }
    var vpaText by remember(vpa) { mutableStateOf(vpa) }

    var targetName by remember { mutableStateOf("") }
    var targetHost by remember { mutableStateOf("") }
    var targetHint by remember { mutableStateOf("") }
    var targetRail by remember { mutableStateOf(Rail.INDIA_IMPS) }
    var targetError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("SETTINGS // SECURE SLOTS", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NoteBanner(
                text = "Keys are held in EncryptedSharedPreferences behind an Android Keystore AES-256 master key.",
                color = VaultTextDim
            )

            Spacer(Modifier.height(14.dp))

            SectionCard("RAIL READINESS", accent = VaultAmber) {
                MonoLabel(if (keys.indiaReady) "INDIA (IMPS/NEFT) — KEY PRESENT" else "INDIA (IMPS/NEFT) — NO KEY", if (keys.indiaReady) VaultGreen else VaultRed)
                MonoLabel(if (keys.usReady) "US (ACH) — KEY PRESENT" else "US (ACH) — NO KEY", if (keys.usReady) VaultGreen else VaultRed)
                MonoLabel(if (keys.pkReady) "PAKISTAN (IBFT) — KEY PRESENT" else "PAKISTAN (IBFT) — NO KEY", if (keys.pkReady) VaultGreen else VaultRed)
            }

            Spacer(Modifier.height(14.dp))

            SectionCard("INDIA — RAZORPAY X") {
                KeyField(
                    label = "Razorpay key id",
                    saved = keys.razorpayId,
                    hint = "rzp_live_…"
                ) { vm.save(SecureStore.K_RAZORPAY_ID, it) }
                Spacer(Modifier.height(10.dp))
                KeyField(
                    label = "Razorpay key secret",
                    saved = keys.razorpaySecret
                ) { vm.save(SecureStore.K_RAZORPAY_SECRET, it) }
                Spacer(Modifier.height(10.dp))
                KeyField(
                    label = "Razorpay X current account no.",
                    saved = keys.razorpayAccount,
                    hint = "the funding account the payout is debited from",
                    keyboard = KeyboardType.Number
                ) { vm.save(SecureStore.K_RAZORPAY_ACC, it) }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("INDIA — CASHFREE (ALTERNATIVE)") {
                KeyField(
                    label = "Cashfree client id",
                    saved = keys.cashfreeId
                ) { vm.save(SecureStore.K_CASHFREE_ID, it) }
                Spacer(Modifier.height(10.dp))
                KeyField(
                    label = "Cashfree client secret",
                    saved = keys.cashfreeSecret
                ) { vm.save(SecureStore.K_CASHFREE_SECRET, it) }
                Spacer(Modifier.height(6.dp))
                MonoLabel("If Cashfree is filled it wins over Razorpay X.", VaultTextDim)
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("US — STRIPE ACH") {
                KeyField(
                    label = "Stripe secret key",
                    saved = keys.stripeKey,
                    hint = "sk_live_…"
                ) { vm.save(SecureStore.K_STRIPE, it) }
                Spacer(Modifier.height(6.dp))
                MonoLabel("Uses PaymentIntent with us_bank_account + mandate_data.", VaultTextDim)
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("PAKISTAN — PAYFAST IBFT") {
                KeyField(
                    label = "PayFast merchant id",
                    saved = keys.payfastMerchant
                ) { vm.save(SecureStore.K_PAYFAST_MERCHANT, it) }
                Spacer(Modifier.height(10.dp))
                KeyField(
                    label = "PayFast secured key",
                    saved = keys.payfastSecured
                ) { vm.save(SecureStore.K_PAYFAST_SECURED, it) }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("UPI DEEP-LINK FALLBACK") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalField(
                        label = "Your UPI VPA",
                        value = vpaText,
                        onChange = { vpaText = it },
                        hint = "name@bank — fires the real UPI sheet with upi://",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.saveVpa(vpaText) },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                    ) { Text("SAVE", fontFamily = FontFamily.Monospace) }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("RUN BEHAVIOUR") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        MonoLabel("STEALTH PANEL MOTION", VaultText)
                        MonoLabel("Animate the live spoof IP block during a run", VaultTextDim)
                    }
                    Switch(
                        checked = motion,
                        onCheckedChange = { vm.setMotion(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultBlack,
                            checkedTrackColor = VaultGreen,
                            uncheckedThumbColor = VaultTextDim,
                            uncheckedTrackColor = VaultGrey
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalField(
                        label = "Webhook URL (optional)",
                        value = webhookText,
                        onChange = { webhookText = it },
                        hint = "POSTed when a run settles",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.setWebhook(webhookText) },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                    ) { Text("SAVE", fontFamily = FontFamily.Monospace) }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("CUSTOM BANK TARGETS") {
                MonoLabel("Unknown institutions can be mapped here so recon still knows the host.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rail.values().forEach { r ->
                        FilterChip(
                            selected = targetRail == r,
                            onClick = { targetRail = r },
                            label = {
                                Text(
                                    r.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VaultGreen.copy(alpha = 0.15f),
                                selectedLabelColor = VaultGreen,
                                labelColor = VaultTextDim
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = targetRail == r,
                                borderColor = VaultGrey,
                                selectedBorderColor = VaultGreen
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TerminalField(
                    label = "Bank name",
                    value = targetName,
                    onChange = { targetName = it }
                )
                Spacer(Modifier.height(8.dp))
                TerminalField(
                    label = "Host domain",
                    value = targetHost,
                    onChange = { targetHost = it },
                    hint = "e.g. mybank.example.com"
                )
                Spacer(Modifier.height(8.dp))
                TerminalField(
                    label = "Match hint",
                    value = targetHint,
                    onChange = { targetHint = it.uppercase() },
                    error = targetError,
                    hint = "IFSC prefix (IN), IBAN bank code (PK) or 9-digit ABA (US)"
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (targetName.isBlank() || targetHost.isBlank() || targetHint.isBlank()) {
                            targetError = "Name, host and match hint are all required"
                        } else {
                            targetError = null
                            vm.addTarget(targetName, targetHost, targetRail, targetHint)
                            targetName = ""
                            targetHost = ""
                            targetHint = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                ) {
                    Text("ADD TARGET", fontFamily = FontFamily.Monospace)
                }

                if (targets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    targets.forEach { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultText,
                                    fontFamily = FontFamily.Monospace
                                )
                                MonoLabel("${t.rail} // ${t.host} // ${t.keyHint}", VaultTextDim)
                            }
                            IconButton(onClick = { vm.removeTarget(t.host) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Remove target",
                                    tint = VaultRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(VaultCard, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, VaultRed.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                MonoLabel("DESTRUCTIVE", VaultRed)
                Spacer(Modifier.height(6.dp))
                MonoLabel("Wipes every run, tranche, custom target and stored key.", VaultTextDim)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { confirmShred = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack)
                ) {
                    Text("SHRED ALL LOGS", fontFamily = FontFamily.Monospace)
                }
                if (shredded) {
                    Spacer(Modifier.height(8.dp))
                    MonoLabel("ALL LOGS SHREDDED — vault empty", VaultGreen)
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmShred) {
        AlertDialog(
            onDismissRequest = { confirmShred = false },
            containerColor = VaultCard,
            title = {
                Text("SHRED ALL LOGS?", color = VaultRed, fontFamily = FontFamily.Monospace)
            },
            text = {
                Text(
                    "Every stored run, tranche, custom target and gateway key is deleted from this device. Cannot be undone.",
                    color = VaultText,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmShred = false
                    vm.shred()
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
private fun KeyField(
    label: String,
    saved: String,
    hint: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    onSave: (String) -> Unit
) {
    var text by remember(saved) { mutableStateOf(saved) }
    var reveal by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TerminalField(
            label = label,
            value = text,
            onChange = { text = it },
            hint = if (saved.isNotBlank()) "SAVED — ${hint ?: "…"}" else hint,
            keyboard = keyboard,
            transformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (reveal) "Hide" else "Reveal",
                        tint = VaultGreen
                    )
                }
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { onSave(text) },
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
        ) { Text("SAVE", fontFamily = FontFamily.Monospace) }
    }
}
