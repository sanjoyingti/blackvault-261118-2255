package com.aibuilt.blackvault.ui.screens

import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.Tranche
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
            VaultTopBar("TRANSACTION RECEIPT", onBack = onBack) {
                val rec = run
                if (rec != null) {
                    IconButton(onClick = {
                        val text = shareText(rec, tranches)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Share receipt")) }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = VaultGreen)
                    }
                }
            }
        }
    ) { pad ->
        val rec = run
        if (rec == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = VaultGreen)
                Spacer(Modifier.height(10.dp))
                MonoLabel("loading record …", VaultTextDim)
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Banner(rec)

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(VaultCard, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    MonoLabel("AMOUNT TRANSFERRED")
                    Text(
                        money(rec.amountUsd, "$"),
                        style = MaterialTheme.typography.displayMedium,
                        color = VaultGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    MonoLabel(
                        if (rec.localAmount > 0) {
                            "≈ ${money(rec.localAmount)} ${rec.localCurrency} @ ${"%.4f".format(rec.fxRate)}"
                        } else {
                            "USD — no conversion recorded"
                        },
                        VaultTextDim
                    )
                }
                Text(
                    "UNTRACEABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultBlack,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(VaultGreen, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("DETAILS") {
                DetailRow("BANK", rec.bankName)
                DetailRow("RAIL", rec.rail)
                DetailRow("ACCOUNT", rec.accountMasked)
                if (rec.ifsc.isNotBlank()) DetailRow("IFSC", rec.ifsc)
                if (rec.iban.isNotBlank()) DetailRow("IBAN", rec.iban)
                if (rec.aba.isNotBlank()) DetailRow("ROUTING", rec.aba)
                DetailRow("GATEWAY", rec.gateway ?: "—")
                DetailRow("TIMESTAMP", stampOfDate(rec.createdAt))
                Spacer(Modifier.height(8.dp))
                MonoLabel("GATEWAY REFERENCE")
                SelectionContainer {
                    Text(
                        rec.reference ?: "— the gateway returned no reference —",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rec.reference != null) VaultGreen else VaultAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (rec.message != null) {
                    Spacer(Modifier.height(8.dp))
                    MonoLabel("GATEWAY MESSAGE", VaultAmber)
                    Text(
                        rec.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("BANK SERVER FINGERPRINT (gathered live in recon)") {
                DetailRow("HOST", rec.host ?: "no host mapping — generic path")
                DetailRow("CERT ISSUER", rec.certIssuer ?: "not captured")
                DetailRow("CERT EXPIRY", rec.certExpiry ?: "not captured")
                DetailRow("SERVER BANNER", rec.serverBanner ?: "none returned")
                Spacer(Modifier.height(6.dp))
                MonoLabel("CERT SHA-256")
                Text(
                    rec.fingerprint ?: "— handshake failed, no fingerprint —",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultTextDim,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (tranches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard("TRANCHES (${tranches.size})", accent = VaultAmber) {
                    tranches.forEach { t -> TrancheRow(t) }
                }
            }

            Spacer(Modifier.height(12.dp))
            MonoLabel(
                "Recon hit the bank's own servers. The payout was submitted to the gateway with " +
                    "your key. Reference shown above is what the API returned, verbatim.",
                VaultTextDim
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Banner(rec: RunRecord) {
    val (text, color) = when (rec.status) {
        RunStatus.CONFIRMED -> "TRANSFER CONFIRMED" to VaultGreen
        RunStatus.PAYING, RunStatus.RECON -> "TRANSFER IN FLIGHT" to VaultAmber
        RunStatus.ABORTED -> "TRANSFER ABORTED" to VaultAmber
        RunStatus.UPI -> "UPI HAND-OFF ISSUED" to VaultAmber
        else -> "TRANSFER REJECTED" to VaultRed
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, color), RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
    if (rec.status != RunStatus.CONFIRMED) {
        Spacer(Modifier.height(8.dp))
        NoteBanner(
            "Confirmed means the gateway accepted the request. Settlement to the bank ledger follows the rail's own timing.",
            VaultAmber
        )
    }
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonoLabel(key, VaultTextDim)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = VaultText,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun TrancheRow(t: Tranche) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonoLabel("#${t.indexNo} ${money(t.amountLocal)} ${t.status}", VaultTextDim)
        Text(
            t.reference ?: t.message ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = when (t.status) {
                "DONE" -> VaultGreen
                "FAILED" -> VaultRed
                else -> VaultAmber
            },
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun shareText(rec: RunRecord, tranches: List<Tranche>): String {
    val sb = StringBuilder()
    sb.appendLine("BLACKVAULT RECEIPT")
    sb.appendLine("STATUS ${rec.status}")
    sb.appendLine("AMOUNT ${money(rec.amountUsd, "$")}")
    sb.appendLine("BANK ${rec.bankName}")
    sb.appendLine("RAIL ${rec.rail}")
    sb.appendLine("ACCOUNT ${rec.accountMasked}")
    sb.appendLine("REFERENCE ${rec.reference ?: "none"}")
    sb.appendLine("GATEWAY ${rec.gateway ?: "none"}")
    sb.appendLine("AT ${stampOfDate(rec.createdAt)}")
    sb.appendLine("HOST ${rec.host ?: "no mapping"}")
    sb.appendLine("CERT ${rec.certIssuer ?: "n/a"} / ${rec.certExpiry ?: "n/a"}")
    sb.appendLine("BANNER ${rec.serverBanner ?: "n/a"}")
    if (tranches.isNotEmpty()) {
        sb.appendLine("TRANCHES ${tranches.size}")
        tranches.forEach { sb.appendLine(" #${it.indexNo} ${it.status} ${it.reference ?: "-"}") }
    }
    return sb.toString()
}
