package com.aibuilt.blackvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val keys by vm.keys.collectAsState()
    val targets by vm.targets.collectAsState()
    val motion by vm.motion.collectAsState()
    val webhook by vm.webhook.collectAsState()
    val shredded by vm.shredded.collectAsState()

    var confirmShred by remember { mutableStateOf(false) }
    var vpa by remember(vm.vpa.collectAsState().value) { mutableStateOf(vm.vpa.value) }

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("VAULT SETTINGS", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            if (shredded) {
                NoteBanner("ALL LOGS SHREDDED — history, targets and keys wiped", VaultGreen)
                Spacer(Modifier.height(10.dp))
            }

            NoteBanner("Keys live only in encrypted storage on this device (AES-256).", VaultTextDim)
            Spacer(Modifier.height(10.dp))

            SectionCard("INDIA RAIL — RAZORPAY X (IMPS/NEFT)") {
                KeySlot("Razorpay X key id", keys.razorpayId) { vm.save(SecureStore.K_RAZORPAY_ID, it) }
                Spacer(Modifier.height(8.dp))
                KeySlot("Razorpay X key secret", keys.razorpaySecret) { vm.save(SecureStore.K_RAZORPAY_SECRET, it) }
                Spacer(Modifier.height(8.dp))
                KeySlot("Razorpay X source account number", keys.razorpayAccount) { vm.save(SecureStore.K_RAZORPAY_ACC, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.indiaReady) "INDIA RAIL READY" else "INDIA RAIL NOT READY", if (keys.indiaReady) VaultGreen else VaultAmber)
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("INDIA RAIL — CASHFREE (IMPS)") {
                KeySlot("Cashfree client id", keys.cashfreeId) { vm.save(SecureStore.K_CASHFREE_ID, it) }
                Spacer(Modifier.height(8.dp))
                KeySlot("Cashfree client secret", keys.cashfreeSecret) { vm.save(SecureStore.K_CASHFREE_SECRET, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel("Cashfree is used in preference to Razorpay X when both are set.", VaultTextDim)
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("US RAIL — STRIPE (ACH)") {
                KeySlot("Stripe secret key", keys.stripeKey) { vm.save(SecureStore.K_STRIPE, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.usReady) "US RAIL READY" else "US RAIL NOT READY", if (keys.usReady) VaultGreen else VaultAmber)
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("PAKISTAN RAIL — PAYFAST (IBFT / RAAST)") {
                KeySlot("PayFast merchant id", keys.payfastMerchant) { vm.save(SecureStore.K_PAYFAST_MERCHANT, it) }
                Spacer(Modifier.height(8.dp))
                KeySlot("PayFast secured key", keys.payfastSecured) { vm.save(SecureStore.K_PAYFAST_SECURED, it) }
                Spacer(Modifier.height(8.dp))
                MonoLabel(if (keys.pkReady) "PAKISTAN RAIL READY" else "PAKISTAN RAIL NOT READY", if (keys.pkReady) VaultGreen else VaultAmber)
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("UPI FALLBACK") {
                TerminalField(
                    label = "Your UPI VPA (funding source)",
                    value = vpa,
                    onChange = { vpa = it },
                    hint = "e.g. name@okhdfcbank — used for the upi:// deep link sheet"
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.saveVpa(vpa) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
                ) { Text("SAVE VPA", fontFamily = FontFamily.Monospace) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("STEALTH + CALLBACK") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        MonoLabel("STEALTH PANEL MOTION", VaultText)
                        MonoLabel("Animate the live panel during a run", VaultTextDim)
                    }
                    Switch(
                        checked = motion,
                        onCheckedChange = { vm.setMotion(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultBlack,
                            checkedTrackColor = VaultGreen,
                            uncheckedThumbColor = VaultTextDim,
                            uncheckedTrackColor = VaultCard
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                WebhookSlot(webhook) { vm.setWebhook(it) }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("CUSTOM TARGETS") {
                CustomTargetForm { name, host, rail, hint -> vm.addTarget(name, host, rail, hint) }
                if (targets.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    MonoLabel("No custom targets yet.", VaultTextDim)
                } else {
                    Spacer(Modifier.height(8.dp))
                    targets.forEach { t ->
                        TargetRow(t) { vm.removeTarget(t.host) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("DESTRUCTION", accent = VaultRed) {
                MonoLabel("Wipes Room history, tranches, custom targets, all gateway keys and the VPA.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmShred = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(0.dp))
                    Text("  SHRED ALL LOGS", fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmShred) {
        AlertDialog(
            onDismissRequest = { confirmShred = false },
            containerColor = VaultCard,
            title = { Text("SHRED EVERYTHING?", color = VaultGreen, fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "History, receipts, targets and gateway keys are removed from this device.",
                    color = VaultText,
                    fontFamily = FontFamily.Monospace
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
                    Text("KEEP", color = VaultTextDim, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun KeySlot(label: String, saved: String, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val hint = if (saved.isBlank()) {
        "not set"
    } else {
        "stored ****" + saved.takeLast(4)
    }
    TerminalField(
        label = label,
        value = value,
        onChange = { value = it },
        hint = hint,
        keyboard = KeyboardType.Text,
        transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle visibility",
                    tint = VaultTextDim
                )
            }
        }
    )
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = {
            onSave(value)
            value = ""
        },
        enabled = value.isNotBlank()
    ) {
        Text("SAVE", color = if (value.isNotBlank()) VaultGreen else VaultTextDim, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun WebhookSlot(current: String, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    TerminalField(
        label = "Webhook URL (optional)",
        value = value,
        onChange = { value = it },
        hint = if (current.isBlank()) "no webhook set" else "armed: $current",
        keyboard = KeyboardType.Uri
    )
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { onSave(value) }) {
        Text("SAVE WEBHOOK", color = VaultGreen, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CustomTargetForm(onAdd: (String, String, Rail, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var rail by remember { mutableStateOf(Rail.INDIA_IMPS) }
    var error by remember { mutableStateOf<String?>(null) }

    TerminalField(
        label = "Target name",
        value = name,
        onChange = { name = it },
        error = error
    )
    Spacer(Modifier.height(8.dp))
    TerminalField(
        label = "Domain",
        value = host,
        onChange = { host = it },
        hint = "e.g. mybank.example — probed exactly as typed",
        keyboard = KeyboardType.Uri
    )
    Spacer(Modifier.height(8.dp))
    TerminalField(
        label = "Key hint (IFSC prefix / IBAN bank code / ABA)",
        value = hint,
        onChange = { hint = it.uppercase() },
        hint = "e.g. SBIN, HABB or 021000021"
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Rail.entries.forEach { r ->
            FilterChip(
                selected = rail == r,
                onClick = { rail = r },
                label = {
                    Text(
                        r.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = VaultTextDim,
                    selectedLabelColor = VaultBlack,
                    selectedContainerColor = VaultGreen
                )
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            if (name.isBlank() || host.isBlank() || !host.contains(".")) {
                error = "Name and a dotted domain are required"
            } else {
                error = null
                onAdd(name, host, rail, hint)
                name = ""
                host = ""
                hint = ""
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VaultGreen, contentColor = VaultBlack)
    ) { Text("ADD TARGET", fontFamily = FontFamily.Monospace) }
}

@Composable
private fun TargetRow(target: CustomTarget, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VaultBlack, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                target.name,
                style = MaterialTheme.typography.bodyMedium,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("${target.host} // ${target.rail}", VaultTextDim)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove",
                tint = VaultRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
