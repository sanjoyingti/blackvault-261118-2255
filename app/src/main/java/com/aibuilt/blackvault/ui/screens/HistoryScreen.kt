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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
        topBar = { VaultTopBar("VAULT LOG", onBack) }
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
                    Text(
                        "NO RUNS STORED",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("Every run you start is logged here, encrypted at rest.", VaultTextDim)
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            MonoLabel(
                "${runs.size} RUN(S) — tap to open receipt, swipe left to delete",
                VaultTextDim,
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(runs, key = { it.id }) { rec ->
                    val state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                vm.delete(rec.id)
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .background(VaultRed.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = VaultRed)
                            }
                        }
                    ) {
                        RunCard(rec) { onOpen(rec.id) }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun RunCard(rec: RunRecord, onClick: () -> Unit) {
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
                color = VaultGreen,
                fontFamily = FontFamily.Monospace
            )
            Text(
                money(rec.amountUsd, "$"),
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        MonoLabel("${rec.accountMasked} // ${rec.rail}", VaultTextDim)
        Spacer(Modifier.height(2.dp))
        Text(
            rec.reference ?: "no gateway reference",
            style = MaterialTheme.typography.labelSmall,
            color = if (rec.reference != null) VaultGreen else VaultAmber,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MonoLabel(stampOfDate(rec.createdAt), VaultTextDim)
            Text(
                rec.status,
                style = MaterialTheme.typography.labelSmall,
                color = when (rec.status) {
                    RunStatus.CONFIRMED -> VaultGreen
                    RunStatus.FAILED -> VaultRed
                    else -> VaultAmber
                },
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
