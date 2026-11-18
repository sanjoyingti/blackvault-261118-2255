package com.aibuilt.blackvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.data.Crypto
import com.aibuilt.blackvault.data.CustomTarget
import com.aibuilt.blackvault.data.SecureStore
import com.aibuilt.blackvault.data.UpiPrefs
import com.aibuilt.blackvault.model.Rail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KeyState(
    val razorpayId: String = "",
    val razorpaySecret: String = "",
    val razorpayAccount: String = "",
    val cashfreeId: String = "",
    val cashfreeSecret: String = "",
    val stripeKey: String = "",
    val payfastMerchant: String = "",
    val payfastSecured: String = ""
) {
    val indiaReady: Boolean
        get() = (razorpayId.isNotBlank() && razorpaySecret.isNotBlank() && razorpayAccount.isNotBlank()) ||
            (cashfreeId.isNotBlank() && cashfreeSecret.isNotBlank())
    val usReady: Boolean get() = stripeKey.isNotBlank()
    val pkReady: Boolean get() = payfastMerchant.isNotBlank() && payfastSecured.isNotBlank()
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val c = (app as BlackVaultApp).container
    private val upi = UpiPrefs(app)

    private val _keys = MutableStateFlow(load())
    val keys: StateFlow<KeyState> = _keys

    val vpa = MutableStateFlow(upi.vpa)
    val targets: StateFlow<List<CustomTarget>> = c.db.dao().targets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val motion = c.settings.stealthMotion.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val webhook = c.settings.webhookUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val shredded = MutableStateFlow(false)

    private fun load(): KeyState = KeyState(
        razorpayId = c.secure.razorpayKeyId,
        razorpaySecret = c.secure.razorpaySecret,
        razorpayAccount = c.secure.razorpayAccount,
        cashfreeId = c.secure.cashfreeClientId,
        cashfreeSecret = c.secure.cashfreeSecret,
        stripeKey = c.secure.stripeKey,
        payfastMerchant = c.secure.payfastMerchant,
        payfastSecured = c.secure.payfastSecured
    )

    fun save(key: String, value: String) {
        c.secure.put(key, value.trim())
        _keys.value = load()
    }

    fun saveVpa(value: String) {
        upi.vpa = value
        vpa.value = value.trim()
    }

    fun setMotion(on: Boolean) {
        viewModelScope.launch { c.settings.setStealthMotion(on) }
    }

    fun setWebhook(url: String) {
        viewModelScope.launch { c.settings.setWebhookUrl(url) }
    }

    fun addTarget(name: String, host: String, rail: Rail, hint: String) {
        if (name.isBlank() || host.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            c.db.dao().insertTarget(CustomTarget(host.trim(), name.trim().uppercase(), rail.name, hint.trim().uppercase()))
        }
    }

    fun removeTarget(host: String) {
        viewModelScope.launch(Dispatchers.IO) { c.db.dao().deleteTarget(host) }
    }

    fun shred() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                c.db.dao().clearTranchesAll()
                c.db.dao().clearRuns()
                c.db.dao().clearTargets()
                listOf(
                    SecureStore.K_RAZORPAY_ID, SecureStore.K_RAZORPAY_SECRET, SecureStore.K_RAZORPAY_ACC,
                    SecureStore.K_CASHFREE_ID, SecureStore.K_CASHFREE_SECRET, SecureStore.K_STRIPE,
                    SecureStore.K_PAYFAST_MERCHANT, SecureStore.K_PAYFAST_SECURED
                ).forEach { c.secure.put(it, "") }
                c.settings.setWebhookUrl("")
                upi.vpa = ""
                getApplication<Application>().getSharedPreferences("blackvault_upi", 0).edit().clear().apply()
            }
            _keys.value = KeyState()
            vpa.value = ""
            shredded.value = true
        }
    }

    suspend fun currentWebhook(): String = c.settings.webhookUrl.first()

    fun encryptPreview(value: String): String =
        if (value.isBlank()) "" else Crypto.encrypt(value).take(12) + "…"

    fun railLabel(rail: Rail): String = rail.label
}
