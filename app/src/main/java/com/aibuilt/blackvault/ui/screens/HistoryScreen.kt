package com.aibuilt.blackvault.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim
import com.aibuilt.blackvault.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val runs by vm.runs.collectAsState()

    Scaffold(
        containerColor = VaultBlack,
        topBar = { VaultTopBar("RUN LOG // ${runs.size}", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp)
        ) {
            MonoLabel("Swipe a row left to shred it. Tap to re-open the full receipt.", VaultTextDim)
            Spacer(Modifier.height(8.dp))

            if (runs.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO RUNS RECORDED",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextDim,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(6.dp))
                        MonoLabel("Every recon + payout attempt lands here.", VaultTextDim)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(runs, key = { it.id }) { rec ->
                        val state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    vm.delete(rec.id)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = state,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(VaultRed, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = VaultBlack
                                    )
                                }
                            }
                        ) {
                            HistoryRow(
                                rec = rec,
                                onOpen = { onOpen(rec.id) },
                                onDelete = { vm.delete(rec.id) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(rec: RunRecord, onOpen: () -> Unit, onDelete: () -> Unit) {
    val statusColor = when (rec.status) {
        RunStatus.CONFIRMED -> VaultGreen
        RunStatus.FAILED, RunStatus.ABORTED -> VaultRed
        RunStatus.PAYING, RunStatus.RECON -> VaultAmber
        else -> VaultTextDim
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(6.dp))
            .clickable { onOpen() }
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    rec.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultText,
                    fontFamily = FontFamily.Monospace
                )
                MonoLabel("${rec.rail} // ${rec.accountMasked}", VaultTextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    money(rec.amountUsd, "$"),
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    rec.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = VaultAmber.copy(alpha = 0.15f))
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                MonoLabel(stampOfDate(rec.createdAt), VaultTextDim)
                MonoLabel(
                    "REF ${rec.reference ?: "— no gateway reference —"}",
                    if (rec.reference != null) VaultGreen else VaultAmber
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Shred",
                    tint = VaultTextDim,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
