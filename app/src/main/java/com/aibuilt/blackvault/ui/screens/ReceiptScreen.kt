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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.Tranche
import com.aibuilt.blackvault.model.stampOfDate
import com.aibuilt.blackvault.ui.components.MonoLabel
import com.aibuilt.blackvault.ui.components.NoteBanner
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

    val rec = run

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            VaultTopBar("RECEIPT", onBack = onBack) {
                if (rec != null) {
                    IconButton(onClick = {
                        val text = shareText(rec, tranches)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(send, "BlackVault receipt"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share receipt", tint = VaultGreen)
                    }
                }
            }
        }
    ) { pad ->
        if (rec == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = VaultGreen)
                Spacer(Modifier.height(10.dp))
                MonoLabel("LOADING RECEIPT FROM VAULT", VaultTextDim)
            }
            return@Scaffold
        }

        val confirmed = rec.status == RunStatus.CONFIRMED

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val headColor = if (confirmed) VaultGreen else VaultAmber
            val headText = when (rec.status) {
                RunStatus.CONFIRMED -> "TRANSFER CONFIRMED"
                RunStatus.PAYING -> "TRANSFER IN FLIGHT"
                RunStatus.RECON -> "RECON ONLY — NOT PAID"
                RunStatus.ABORTED -> "RUN ABORTED"
                else -> "TRANSFER NOT CONFIRMED"
            }

            Text(
                headText,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                color = headColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (confirmed) "Gateway accepted the payout leg for this run." else
                    (rec.message ?: "No gateway acceptance recorded."),
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextDim,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(
                    text = if (confirmed) "UNTRACEABLE" else "UNVERIFIED",
                    color = if (confirmed) VaultGreen else VaultAmber
                )
                Badge(text = rec.rail, color = VaultTextDim)
            }

            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(VaultCard, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                    .padding(14.dp)
            ) {
                MonoLabel("AMOUNT")
                Text(
                    "$10,000.00",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                    color = if (confirmed) VaultGreen else VaultText,
                    fontFamily = FontFamily.Monospace
                )
                if (rec.localAmount > 0) {
                    MonoLabel(
                        "settled as ${"%,.2f".format(java.util.Locale.US, rec.localAmount)} ${rec.localCurrency}" +
                            if (rec.fxRate > 0) " @ ${"%.4f".format(java.util.Locale.US, rec.fxRate)}" else "",
                        VaultTextDim
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SelectionContainer {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(VaultCard, RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Field("GATEWAY", rec.gateway ?: "—", VaultText)
                    Field("REFERENCE (from gateway response)", rec.reference ?: "no reference returned", VaultGreen)
                    Field("RAIL", rec.rail, VaultText)
                    Field("BANK TARGET", rec.bankName, VaultText)
                    Field("ACCOUNT (masked)", rec.accountMasked, VaultText)
                    if (rec.ifsc.isNotBlank()) Field("IFSC", rec.ifsc, VaultText)
                    if (rec.iban.isNotBlank()) Field("IBAN", maskIban(rec.iban), VaultText)
                    if (rec.aba.isNotBlank()) Field("ROUTING (ABA)", rec.aba, VaultText)
                    Field("TIMESTAMP", stampOfDate(rec.createdAt), VaultText)
                    Field("RUN ID", rec.id, VaultTextDim)
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(VaultCard, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MonoLabel("BANK SERVER FINGERPRINT (gathered in recon)")
                Field("CERT ISSUER", rec.certIssuer ?: "handshake not completed", VaultAmber)
                Field("CERT EXPIRY", rec.certExpiry ?: "—", VaultAmber)
                Field("SERVER BANNER", rec.serverBanner ?: "none returned", VaultAmber)
                Field("CERT SHA-256", rec.fingerprint ?: "—", VaultTextDim)
                Field("HOST", rec.host ?: "generic validation path", VaultTextDim)
            }

            if (tranches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(VaultCard, RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonoLabel("TRANCHES (split because of the corridor limit)")
                    tranches.forEach { t -> TrancheRow(t) }
                }
            }

            Spacer(Modifier.height(12.dp))

            NoteBanner(
                text = "Reference is whatever the gateway actually returned. " +
                    "Money left your funding account — it was transferred, not conjured.",
                color = VaultTextDim
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrancheRow(t: Tranche) {
    val color = when (t.status) {
        "DONE" -> VaultGreen
        "FAILED" -> VaultRed
        else -> VaultAmber
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(VaultBlack, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "#${t.indexNo}  ${"%,.2f".format(java.util.Locale.US, t.amountLocal)}",
                style = MaterialTheme.typography.bodySmall,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            Text(
                t.status,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            t.reference?.let { "REF $it" } ?: (t.message ?: "awaiting gateway"),
            style = MaterialTheme.typography.labelSmall,
            color = VaultTextDim,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun Field(label: String, value: String, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        MonoLabel(label, VaultTextDim)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        " $text ",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp)
    )
}

private fun maskIban(iban: String): String =
    if (iban.length <= 8) iban else iban.take(4) + " **** " + iban.takeLast(4)

private fun shareText(rec: RunRecord, tranches: List<Tranche>): String = buildString {
    appendLine("BLACKVAULT RECEIPT")
    appendLine("status     ${rec.status}")
    appendLine("amount     \$10,000.00")
    appendLine("bank       ${rec.bankName}")
    appendLine("rail       ${rec.rail}")
    appendLine("account    ${rec.accountMasked}")
    appendLine("gateway    ${rec.gateway ?: "-"}")
    appendLine("reference  ${rec.reference ?: "-"}")
    appendLine("at         ${stampOfDate(rec.createdAt)}")
    if (tranches.isNotEmpty()) {
        appendLine("tranches   ${tranches.size}")
        tranches.forEach { appendLine("  #${it.indexNo} ${it.status} ${it.reference ?: "-"}") }
    }
    appendLine("cert       ${rec.certIssuer ?: "-"} / expires ${rec.certExpiry ?: "-"}")
}
