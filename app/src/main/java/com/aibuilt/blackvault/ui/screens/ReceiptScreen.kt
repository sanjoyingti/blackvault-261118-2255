package com.aibuilt.blackvault.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            VaultTopBar("VAULT RECEIPT", onBack) {
                IconButton(onClick = { run?.let { r -> share(context, receiptText(r, tranches)) } }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share receipt", tint = VaultGreen)
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
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatusHead(rec)

            Spacer(Modifier.height(14.dp))

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
                    MonoLabel("AMOUNT (FIXED)")
                    Text(
                        money(rec.amountUsd, "$"),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        color = VaultGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    if (rec.localAmount > 0) {
                        MonoLabel(
                            "${money(rec.localAmount, "")} ${rec.localCurrency} @ ${"%.4f".format(rec.fxRate)}",
                            VaultTextDim
                        )
                    }
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

            Spacer(Modifier.height(14.dp))

            SectionCard("TRANSACTION") {
                Detail("BANK TARGET", rec.bankName, VaultText)
                Detail("RAIL", rec.rail, VaultText)
                Detail("ACCOUNT", rec.accountMasked, VaultText)
                Detail("TIMESTAMP", stampOfDate(rec.createdAt), VaultText)
                Detail("GATEWAY", rec.gateway ?: "<not dispatched>", VaultAmber)
                SelectionContainer {
                    Detail("REFERENCE", rec.reference ?: "<no gateway reference>", VaultGreen)
                }
                rec.message?.let { Detail("GATEWAY MESSAGE", it, VaultTextDim) }
            }

            Spacer(Modifier.height(12.dp))

            if (tranches.isNotEmpty()) {
                SectionCard("TRANCHES (${tranches.size})") {
                    tranches.forEach { t -> TrancheRow(t, rec.localCurrency) }
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionCard("BANK SERVER FINGERPRINT", accent = VaultAmber) {
                Detail("HOST", rec.host ?: "<no host mapping>", VaultText)
                Detail("CERT ISSUER", rec.certIssuer ?: "<not captured>", VaultText)
                Detail("CERT EXPIRY", rec.certExpiry ?: "<not captured>", VaultText)
                Detail("SERVER BANNER", rec.serverBanner ?: "<none returned>", VaultText)
                SelectionContainer {
                    Detail("CERT SHA-256", rec.fingerprint ?: "<not captured>", VaultTextDim)
                }
            }

            Spacer(Modifier.height(12.dp))

            NoteBanner(
                text = "The reference above is the gateway's own id, not a made-up string. " +
                    "Recon lines were answered by ${rec.host ?: "no host"} in real time. " +
                    "Money left your funding account — it was transferred, not conjured.",
                color = VaultTextDim
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusHead(rec: RunRecord) {
    val (title, color) = when (rec.status) {
        RunStatus.CONFIRMED -> "TRANSFER CONFIRMED" to VaultGreen
        RunStatus.PAYING -> "PAYOUT IN FLIGHT" to VaultAmber
        RunStatus.ABORTED -> "RUN ABORTED" to VaultAmber
        RunStatus.FAILED ->
            if (rec.message?.startsWith("NO FUNDING") == true) "TRANSFER NOT LIVE" to VaultAmber
            else "PAYOUT REJECTED" to VaultRed
        else -> "RECON ONLY — NOT PAID" to VaultAmber
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        MonoLabel(
            if (rec.status == RunStatus.CONFIRMED) "accepted by ${rec.gateway ?: "gateway"}"
            else "status ${rec.status}",
            VaultTextDim
        )
    }
}

@Composable
private fun Detail(label: String, value: String, color: Color) {
    Column(Modifier.padding(bottom = 8.dp)) {
        MonoLabel(label, VaultTextDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TrancheRow(t: Tranche, currency: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${t.indexNo}",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
        Text(
            " ${money(t.amountLocal, "")} $currency ",
            style = MaterialTheme.typography.bodySmall,
            color = VaultText,
            fontFamily = FontFamily.Monospace
        )
        Text(
            t.status,
            style = MaterialTheme.typography.labelSmall,
            color = when (t.status) {
                "DONE" -> VaultGreen
                "FAILED" -> VaultRed
                else -> VaultAmber
            },
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.size(6.dp))
        MonoLabel(t.reference ?: "", VaultTextDim)
    }
}

private fun receiptText(rec: RunRecord, tranches: List<Tranche>): String {
    val sb = StringBuilder()
    sb.appendLine("BLACKVAULT RECEIPT")
    sb.appendLine("status: ${rec.status}")
    sb.appendLine("bank: ${rec.bankName}")
    sb.appendLine("rail: ${rec.rail}")
    sb.appendLine("account: ${rec.accountMasked}")
    sb.appendLine("amount: ${money(rec.amountUsd, "$")}")
    sb.appendLine("reference: ${rec.reference ?: "none"}")
    sb.appendLine("time: ${stampOfDate(rec.createdAt)}")
    if (tranches.isNotEmpty()) {
        sb.appendLine("tranches:")
        tranches.forEach {
            sb.appendLine("  #${it.indexNo} ${money(it.amountLocal, "")} ${rec.localCurrency} ${it.status} ${it.reference ?: ""}")
        }
    }
    sb.appendLine("cert: ${rec.fingerprint ?: "n/a"}")
    return sb.toString()
}

private fun share(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "BLACKVAULT receipt")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share receipt")) }
}
