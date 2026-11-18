package com.aibuilt.blackvault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.aibuilt.blackvault.ui.theme.VaultGrey
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
        topBar = { VaultTopBar("VAULT LOG // HISTORY", onBack = onBack) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp)
        ) {
            MonoLabel("${runs.size} RUN(S) ON DEVICE — SWIPE A ROW LEFT TO DELETE", VaultTextDim)
            Spacer(Modifier.height(8.dp))
            if (runs.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "NO RUNS YET",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("Start a payout from the pay screen", VaultTextDim)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(runs, key = { it.id }) { run ->
                        HistoryRow(
                            run = run,
                            onOpen = { onOpen(run.id) },
                            onDelete = { vm.delete(run.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(run: RunRecord, onOpen: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart || v == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(VaultRed.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = VaultRed)
            }
        }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(VaultCard, RoundedCornerShape(6.dp))
                .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                .clickable { onOpen() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    run.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultText,
                    fontFamily = FontFamily.Monospace
                )
                MonoLabel("${run.accountMasked} // ${run.rail} // \$${money(run.amountUsd)}")
                MonoLabel(stampOfDate(run.createdAt), VaultTextDim)
                Text(
                    run.reference ?: "no reference",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                when (run.status) {
                    RunStatus.CONFIRMED -> "OK"
                    RunStatus.FAILED -> "REJ"
                    RunStatus.ABORTED -> "ABT"
                    else -> "PEND"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (run.status) {
                    RunStatus.CONFIRMED -> VaultGreen
                    RunStatus.FAILED -> VaultRed
                    else -> VaultAmber
                },
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(width = 34.dp, height = 14.dp)
            )
        }
    }
}
