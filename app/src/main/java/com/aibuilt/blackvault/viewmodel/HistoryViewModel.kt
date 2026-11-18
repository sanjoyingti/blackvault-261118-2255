package com.aibuilt.blackvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.model.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as BlackVaultApp).container.db.dao()

    val runs: StateFlow<List<RunRecord>> = dao.allRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.clearTranches(id)
                dao.deleteRun(id)
            }
        }
    }
}
