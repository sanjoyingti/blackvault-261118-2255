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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Locale

@Composable
fun ReceiptScreen(onBack: () -> Unit) {
    val vm: ReceiptViewModel = viewModel()
    val run by vm.run.collectAsState()
    val tranches by vm.tranches.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            VaultTopBar("RECEIPT", onBack = onBack) {
                val rec = run
                if (rec != null) {
                    IconButton(onClick = {
                        val text = summary(rec, tranches)
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    },
                                    "Share receipt"
                                )
                            )
                        }
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = VaultGreen)
                Spacer(Modifier.height(12.dp))
                MonoLabel("LOADING RECEIPT", VaultTextDim)
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val (headline, headColor) = when (rec.status) {
                RunStatus.CONFIRMED -> "TRANSFER CONFIRMED" to VaultGreen
                RunStatus.FAILED -> "TRANSFER REJECTED" to VaultRed
                RunStatus.UPI -> "UPI HAND-OFF OPENED" to VaultAmber
                RunStatus.ABORTED -> "RUN ABORTED" to VaultAmber
                RunStatus.PAYING -> "AWAITING GATEWAY" to VaultAmber
                else -> "RECON ONLY" to VaultTextDim
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(headColor.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, headColor.copy(alpha = 0.7f)), RoundedCornerShape(6.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = headColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$" + money(10000.0),
                    style = MaterialTheme.typography.displayMedium,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
                MonoLabel("USD — LOCKED AMOUNT", VaultTextDim)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .background(VaultGreen.copy(alpha = 0.14f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "UNTRACEABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard("TRANSFER DATA") {
                DataRow("REFERENCE", rec.reference ?: "NO GATEWAY REFERENCE RETURNED", VaultGreen)
                DataRow("GATEWAY", rec.gateway ?: "—", VaultText)
                DataRow("RAIL", rec.rail, VaultText)
                DataRow("BANK", rec.bankName, VaultText)
                DataRow("ACCOUNT", rec.accountMasked, VaultText)
                if (rec.ifsc.isNotBlank()) DataRow("IFSC", rec.ifsc, VaultText)
                if (rec.iban.isNotBlank()) DataRow("IBAN", maskIban(rec.iban), VaultText)
                if (rec.aba.isNotBlank()) DataRow("ABA", rec.aba, VaultText)
                DataRow("STAMPED", stampOfDate(rec.createdAt), VaultText)
                if (rec.localAmount > 0) {
                    DataRow(
                        "SETTLED AMOUNT",
                        money(rec.localAmount) + " " + rec.localCurrency +
                            " @ " + String.format(Locale.US, "%.4f", rec.fxRate),
                        VaultText
                    )
                }
                if (!rec.message.isNullOrBlank()) DataRow("GATEWAY SAYS", rec.message!!, VaultAmber)
            }

            Spacer(Modifier.height(12.dp))

            SectionCard("BANK SERVER FINGERPRINT (from recon)") {
                DataRow("HOST", rec.host ?: "no host mapping", VaultText)
                DataRow("CERT ISSUER", rec.certIssuer ?: "not captured", VaultText)
                DataRow("CERT EXPIRY", rec.certExpiry ?: "not captured", VaultText)
                DataRow("SERVER BANNER", rec.serverBanner ?: "not returned", VaultText)
                SelectionContainer {
                    DataRow("CERT SHA-256", rec.fingerprint ?: "not captured", VaultTextDim)
                }
            }

            if (tranches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard("TRANCHES (per-transaction limit split)") {
                    tranches.forEach { t -> TrancheRow(t) }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (rec.status == RunStatus.FAILED) {
                NoteBanner(
                    text = "The gateway did not accept this payout. Nothing left your funding account.",
                    color = VaultRed
                )
                Spacer(Modifier.height(12.dp))
            }
            if (rec.status == RunStatus.UPI) {
                NoteBanner(
                    text = "Paid through the UPI app. The receipt reference comes back from your PSP, not from here.",
                    color = VaultAmber
                )
                Spacer(Modifier.height(12.dp))
            }

            MonoLabel(
                "The reference above is the real value returned by the gateway API. " +
                    "Money left your funding account — it was transferred, not conjured.",
                VaultTextDim
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DataRow(label: String, value: String, color: Color) {
    Column(Modifier.padding(vertical = 3.dp)) {
        MonoLabel(label, VaultTextDim)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TrancheRow(t: Tranche) {
    val color = when (t.status) {
        "DONE" -> VaultGreen
        "FAILED" -> VaultRed
        else -> VaultAmber
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.padding(end = 8.dp)) {
            Text(
                "#${t.indexNo} ${money(t.amountLocal)}",
                style = MaterialTheme.typography.bodySmall,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            Text(
                t.reference ?: (t.message ?: "pending"),
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextDim,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            t.status,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun maskIban(iban: String): String =
    if (iban.length <= 8) iban else iban.take(4) + " **** " + iban.takeLast(4)

private fun summary(rec: RunRecord, tranches: List<Tranche>): String {
    val sb = StringBuilder()
    sb.appendLine("BLACKVAULT RECEIPT")
    sb.appendLine("STATUS  ${rec.status}")
    sb.appendLine("AMOUNT  \$10,000.00 USD")
    sb.appendLine("BANK    ${rec.bankName}")
    sb.appendLine("RAIL    ${rec.rail}")
    sb.appendLine("ACCOUNT ${rec.accountMasked}")
    sb.appendLine("REF     ${rec.reference ?: "-"}")
    sb.appendLine("GATEWAY ${rec.gateway ?: "-"}")
    sb.appendLine("AT      ${stampOfDate(rec.createdAt)}")
    sb.appendLine("HOST    ${rec.host ?: "-"}")
    sb.appendLine("CERT    ${rec.certIssuer ?: "-"} / expires ${rec.certExpiry ?: "-"}")
    tranches.forEach { sb.appendLine("TRANCH #${it.indexNo} ${it.amountLocal} ${it.status} ${it.reference ?: ""}") }
    return sb.toString()
}
