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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
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

    var showShred by remember { mutableStateOf(false) }
    var targetName by remember { mutableStateOf("") }
    var targetHost by remember { mutableStateOf("") }
    var targetHint by remember { mutableStateOf("") }
    var targetRail by remember { mutableStateOf(Rail.INDIA_IMPS) }
    var hookText by remember { mutableStateOf(webhook) }
    var vpaText by remember { mutableStateOf(vpa) }

    androidx.compose.runtime.LaunchedEffect(webhook) { hookText = webhook }
    androidx.compose.runtime.LaunchedEffect(vpa) { vpaText = vpa }

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("SETTINGS // KEYS", onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            NoteBanner(
                text = "Keys are held in EncryptedSharedPreferences. Without a key for a rail the payout is not live — the app says so instead of pretending.",
                color = VaultAmber
            )

            Spacer(Modifier.height(14.dp))

            SectionCard("RAZORPAY X (INDIA IMPS)", accent = if (keys.indiaReady) VaultGreen else VaultAmber) {
                MonoLabel("Key id, key secret and your Razorpay X account number are all required.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                SecretField("RAZORPAY KEY ID", keys.razorpayId, { vm.save(SecureStore.K_RAZORPAY_ID, it) })
                SecretField("RAZORPAY KEY SECRET", keys.razorpaySecret, { vm.save(SecureStore.K_RAZORPAY_SECRET, it) })
                SecretField("FUNDING ACCOUNT NUMBER", keys.razorpayAccount, { vm.save(SecureStore.K_RAZORPAY_ACC, it) })
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("CASHFREE (INDIA — ALTERNATIVE)", accent = VaultTextDim) {
                MonoLabel("Used instead of Razorpay when both fields are filled.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                SecretField("CASHFREE CLIENT ID", keys.cashfreeId, { vm.save(SecureStore.K_CASHFREE_ID, it) })
                SecretField("CASHFREE CLIENT SECRET", keys.cashfreeSecret, { vm.save(SecureStore.K_CASHFREE_SECRET, it) })
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("STRIPE (US ACH)", accent = if (keys.usReady) VaultGreen else VaultAmber) {
                SecretField("STRIPE SECRET KEY", keys.stripeKey, { vm.save(SecureStore.K_STRIPE, it) })
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("PAYFAST (PAKISTAN IBFT)", accent = if (keys.pkReady) VaultGreen else VaultAmber) {
                SecretField("PAYFAST MERCHANT ID", keys.payfastMerchant, { vm.save(SecureStore.K_PAYFAST_MERCHANT, it) })
                SecretField("PAYFAST SECURED KEY", keys.payfastSecured, { vm.save(SecureStore.K_PAYFAST_SECURED, it) })
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("UPI DEEP-LINK FALLBACK") {
                MonoLabel("Your receiving VPA. Opens the payment sheet with the amount prefilled.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = vpaText,
                        onValueChange = { vpaText = it },
                        label = { MonoLabel("VPA  (name@bank)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { vm.saveVpa(vpaText) }) { Text("SAVE", color = VaultGreen) }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("STEALTH PANEL MOTION") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MonoLabel("Animate the spoof panel during a run", VaultTextDim)
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
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("WEBHOOK (OPTIONAL)") {
                MonoLabel("POSTed a small JSON summary when a chain finishes.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hookText,
                        onValueChange = { hookText = it },
                        label = { MonoLabel("https://…") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { vm.setWebhook(hookText) }) { Text("SAVE", color = VaultGreen) }
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("CUSTOM TARGETS") {
                MonoLabel("Unknown input still runs the generic path. Add a target to map it to a real host.", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = { MonoLabel("BANK NAME") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    label = { MonoLabel("HOST  (e.g. bank.example)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = targetHint,
                    onValueChange = { targetHint = it.uppercase() },
                    label = { MonoLabel("KEY HINT  (IFSC prefix / PK bank code / ABA)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Rail.entries.forEach { r ->
                        TextButton(onClick = { targetRail = r }) {
                            Text(
                                r.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetRail == r) VaultGreen else VaultTextDim,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        vm.addTarget(targetName, targetHost, targetRail, targetHint)
                        targetName = ""
                        targetHost = ""
                        targetHint = ""
                    },
                    enabled = targetName.isNotBlank() && targetHost.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaultGreen,
                        contentColor = VaultBlack,
                        disabledContainerColor = VaultGrey,
                        disabledContentColor = VaultTextDim
                    )
                ) { Text("ADD TARGET", fontFamily = FontFamily.Monospace) }

                if (targets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    targets.forEach { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultText,
                                    fontFamily = FontFamily.Monospace
                                )
                                MonoLabel("${t.host} // ${t.rail} // ${t.keyHint}", VaultTextDim)
                            }
                            IconButton(onClick = { vm.removeTarget(t.host) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = VaultRed)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = { showShred = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultRed, contentColor = VaultBlack)
            ) {
                Text("SHRED ALL LOGS", style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
            }

            if (shredded) {
                Spacer(Modifier.height(8.dp))
                NoteBanner("ROOM + KEYS WIPED — every run, tranche, target and stored key is gone.", color = VaultGreen)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showShred) {
        AlertDialog(
            onDismissRequest = { showShred = false },
            containerColor = VaultCard,
            title = { Text("SHRED ALL LOGS", color = VaultRed, fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "Deletes every stored run, tranche, custom target and key from Room and the encrypted store. It cannot be undone.",
                    color = VaultText,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showShred = false
                    vm.shred()
                }) { Text("SHRED", color = VaultRed, fontFamily = FontFamily.Monospace) }
            },
            dismissButton = {
                TextButton(onClick = { showShred = false }) {
                    Text("CANCEL", color = VaultTextDim, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VaultGreen,
    unfocusedBorderColor = VaultGrey,
    focusedLabelColor = VaultGreen,
    unfocusedLabelColor = VaultTextDim,
    cursorColor = VaultGreen,
    focusedTextColor = VaultText,
    unfocusedTextColor = VaultText
)

@Composable
private fun SecretField(label: String, stored: String, onSave: (String) -> Unit) {
    var text by remember(stored) { mutableStateOf(stored) }
    var reveal by remember { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { MonoLabel(label) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = { reveal = !reveal }) {
                        Icon(
                            if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = VaultTextDim
                        )
                    }
                },
                colors = fieldColors(),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onSave(text) }) { Text("SAVE", color = VaultGreen, fontSize = MaterialTheme.typography.labelSmall.fontSize) }
        }
        MonoLabel(
            if (stored.isBlank()) "empty" else "stored • ${mask(stored)}",
            if (stored.isBlank()) VaultAmber else VaultGreen
        )
    }
}

private fun mask(value: String): String =
    if (value.length <= 4) "****" else value.take(4) + "••••" + value.takeLast(2)
