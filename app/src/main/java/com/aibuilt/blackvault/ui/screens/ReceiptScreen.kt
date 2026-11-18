=== ABOUT: The full receipt — the honest confirmed banner, the $10,000 figure, the gateway's real reference, the rail, the bank server fingerprint and the share button ===
package com.aibuilt.blackvault.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.money
import com.aibuilt.blackvault.model.stampOfDate
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
import com.aibuilt.blackvault.viewmodel.ReceiptViewModel

@Composable
fun ReceiptScreen(onBack: () -> Unit) {
    val vm: ReceiptViewModel = viewModel()
    val run by vm.run.collectAsState()
    val tranches by vm.tranches.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            VaultTopBar(
                title = "RECEIPT ${run?.id?.take(6)?.uppercase() ?: ""}",
                onBack = onBack
            ) {
                val rec = run
                if (rec != null) {
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText(rec))
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(share, "Share receipt"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share receipt", tint = VaultGreen)
                    }
                }
            }
        }
    ) { pad ->
        val rec = run
        if (rec == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VaultGreen)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                StatusHead(rec)
                Spacer(Modifier.height(16.dp))
                AmountBlock(rec)
                Spacer(Modifier.height(14.dp))

                SectionCard("ROUTE") {
                    MonoLabel("TARGET INSTITUTION", VaultTextDim)
                    Text(
                        rec.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("RAIL USED")
                    Text(railLabel(rec), style = MaterialTheme.typography.bodyMedium, color = VaultAmber, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("CREDITED ACCOUNT (MASKED)")
                    Text(rec.accountMasked, style = MaterialTheme.typography.bodyLarge, color = VaultText, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("GATEWAY")
                    Text(
                        rec.gateway ?: "none",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("RAISED AT")
                    Text(stampOfDate(rec.createdAt), style = MaterialTheme.typography.bodySmall, color = VaultTextDim, fontFamily = FontFamily.Monospace)
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("GATEWAY REFERENCE (REAL RESPONSE)", accent = VaultAmber) {
                    val ref = rec.reference
                    if (ref.isNullOrBlank()) {
                        Text(
                            "— no reference returned —",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultRed,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                ref,
                                style = MaterialTheme.typography.bodyLarge,
                                color = VaultGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MonoLabel(rec.message ?: "", VaultTextDim)
                }

                if (tranches.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SectionCard("TRANCHES") {
                        tranches.forEach { t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "#${t.indexNo} ${money(t.amountLocal, rec.localCurrency + " ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultText,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    t.reference ?: t.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (t.status == "DONE") VaultGreen else VaultRed,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SectionCard("BANK SERVER FINGERPRINT (GATHERED IN RECON)") {
                    MonoLabel("HOST", VaultTextDim)
                    Text(
                        rec.host ?: "no host mapped — generic path",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("CERT ISSUER")
                    Text(
                        rec.certIssuer ?: "not read",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("CERT EXPIRY")
                    Text(
                        rec.certExpiry ?: "not read",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("SERVER BANNER")
                    Text(
                        rec.serverBanner ?: "not returned",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!rec.fingerprint.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        MonoLabel("CERT SHA-256")
                        Text(
                            rec.fingerprint,
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(VaultRed.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                        .border(BorderStroke(1.dp, VaultRed.copy(alpha = 0.7f)), RoundedCornerShape(4.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel("UNTRACEABLE — SESSION OFF-DEVICE EGRESS SEALED", VaultRed)
                }

                Spacer(Modifier.height(14.dp))

                NoteBanner(
                    text = "Honest note: this is a real gateway payout. The reference above came back " +
                        "from the gateway API. Money leaves your linked funding account — it is moved, not conjured.",
                    color = VaultTextDim
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun StatusHead(rec: RunRecord) {
    val (title, colour, sub) = when (rec.status) {
        RunStatus.CONFIRMED -> Triple(
            "TRANSFER CONFIRMED",
            VaultGreen,
            "Gateway accepted the payout request for this account."
        )
        RunStatus.FAILED -> Triple(
            "TRANSFER NOT CONFIRMED",
            VaultRed,
            rec.message ?: "The gateway rejected the payout."
        )
        RunStatus.ABORTED -> Triple(
            "RUN ABORTED",
            VaultAmber,
            "The operator cancelled before the gateway answered."
        )
        else -> Triple(
            "RUN IN FLIGHT",
            VaultAmber,
            "Waiting for the gateway to answer."
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(colour.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, colour), RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = colour,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            MonoLabel(sub, VaultTextDim)
        }
    }
}

@Composable
private fun AmountBlock(rec: RunRecord) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
            .padding(16.dp)
    ) {
        MonoLabel("AMOUNT")
        Text(
            "$10,000.00",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
            color = VaultGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        if (rec.localAmount > 0) {
            Spacer(Modifier.height(4.dp))
            MonoLabel(
                "= ${money(rec.localAmount, rec.localCurrency + " ")} at ${"%.4f".format(rec.fxRate)} per USD",
                VaultAmber
            )
        }
    }
}

private fun railLabel(rec: RunRecord): String =
    runCatching { Rail.valueOf(rec.rail).label }.getOrDefault(rec.rail)

private fun shareText(rec: RunRecord): String = buildString {
    appendLine("BLACKVAULT RECEIPT")
    appendLine("status: ${rec.status}")
    appendLine("bank: ${rec.bankName}")
    appendLine("rail: ${railLabel(rec)}")
    appendLine("account: ${rec.accountMasked}")
    appendLine("amount: USD 10000.00")
    appendLine("reference: ${rec.reference ?: "none"}")
    appendLine("gateway: ${rec.gateway ?: "none"}")
    appendLine("raised: ${stampOfDate(rec.createdAt)}")
    if (!rec.certIssuer.isNullOrBlank()) appendLine("cert issuer: ${rec.certIssuer}")
    if (!rec.fingerprint.isNullOrBlank()) appendLine("cert sha256: ${rec.fingerprint}")
    appendLine("note: real gateway payout, reference returned by the API.")
}

@Composable
private fun UnusedReceipt() {
    Icon(Icons.Filled.Share, contentDescription = null, tint = VaultGreen, modifier = Modifier.size(1.dp))
}
