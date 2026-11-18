=== ABOUT: The history list — every run with bank, masked account, amount and reference; tap to reopen the receipt, swipe left to delete ===
package com.aibuilt.blackvault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.money
import com.aibuilt.blackvault.model.stampOfDate
import com.aibuilt.blackvault.ui.components.MonoLabel
import com.aibuilt.blackvault.ui.components.VaultTopBar
import com.aibuilt.blackvault.ui.theme.VaultAmber
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultCard
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultGrey
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val runs by vm.runs.collectAsState()

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("RUN HISTORY", onBack = onBack) }
    ) { pad ->
        if (runs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MonoLabel("NO RUNS STORED YET", VaultTextDim)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Every payout attempt is written here, encrypted at rest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text("BACK TO PAY", color = VaultGreen, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                MonoLabel(
                    "SWIPE A ROW LEFT TO DELETE — TAP TO REOPEN THE RECEIPT",
                    VaultTextDim,
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(runs, key = { it.id }) { rec ->
                        val state = rememberSwipeToDismissBoxState()
                        LaunchedEffect(state.currentValue) {
                            if (state.currentValue == SwipeToDismissBoxValue.EndToStart ||
                                state.currentValue == SwipeToDismissBoxValue.StartToEnd
                            ) {
                                vm.delete(rec.id)
                            }
                        }
                        SwipeToDismissBox(
                            state = state,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(VaultRed.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete run",
                                            tint = VaultBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            "DELETE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VaultBlack,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        ) {
                            HistoryRow(rec) { onOpen(rec.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(rec: RunRecord, onClick: () -> Unit) {
    val statusColor = when (rec.status) {
        RunStatus.CONFIRMED -> VaultGreen
        RunStatus.FAILED -> VaultRed
        RunStatus.ABORTED -> VaultAmber
        else -> VaultTextDim
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                rec.bankName,
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel(rec.status, statusColor)
        }
        Spacer(Modifier.height(4.dp))
        MonoLabel("${rec.accountMasked} // ${rec.rail}")
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                money(rec.amountUsd, "$"),
                style = MaterialTheme.typography.bodyMedium,
                color = VaultGreen,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel(stampOfDate(rec.createdAt))
        }
        if (!rec.reference.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            MonoLabel("REF ${rec.reference}", VaultAmber)
        }
    }
}
