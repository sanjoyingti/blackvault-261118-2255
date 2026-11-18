=== ABOUT: Loads one finished run so the receipt screen can show the real gateway reference ===
package com.aibuilt.blackvault.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.Tranche
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReceiptViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val dao = (app as BlackVaultApp).container.db.dao()
    private val id: String = handle.get<String>("id").orEmpty()

    val run: StateFlow<RunRecord?> = dao.runFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tranches: StateFlow<List<Tranche>> = dao.tranches(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
