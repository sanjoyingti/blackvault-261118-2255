package com.aibuilt.blackvault.ui.screens

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
        topBar = { VaultTopBar("VAULT LOG", onBack = onBack) }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            if (runs.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "NO RUNS IN THE VAULT",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("Start a payout and the record lands here.", VaultTextDim)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { MonoLabel("SWIPE A CARD LEFT TO DELETE IT", VaultTextDim, Modifier.padding(4.dp)) }
                    items(runs, key = { it.id }) { rec ->
                        SwipeCard(rec = rec, onOpen = { onOpen(rec.id) }, onDelete = { vm.delete(rec.id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeCard(rec: RunRecord, onOpen: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
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
                    .background(VaultRed.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = VaultBlack)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  DELETE",
                    style = MaterialTheme.typography.labelLarge,
                    color = VaultBlack,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(VaultCard, RoundedCornerShape(6.dp))
                .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    rec.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    rec.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(rec.status),
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                "${rec.accountMasked}  //  ${rec.rail}",
                style = MaterialTheme.typography.bodySmall,
                color = VaultText,
                fontFamily = FontFamily.Monospace
            )
            MonoLabel("\$10,000.00  ·  ${stampOfDate(rec.createdAt)}", VaultTextDim)
            Text(
                rec.reference?.let { "REF $it" } ?: (rec.message ?: "no reference"),
                style = MaterialTheme.typography.labelSmall,
                color = VaultTextDim,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "OPEN FULL RECEIPT",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun statusColor(status: String) = when (status) {
    RunStatus.CONFIRMED -> VaultGreen
    RunStatus.FAILED, RunStatus.ABORTED -> VaultRed
    else -> VaultAmber
}
