=== ABOUT: The pay screen's brain — now with a readable snapshot of the typed fields and the right app handle ===
package com.aibuilt.blackvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.data.Crypto
import com.aibuilt.blackvault.data.CustomTarget
import com.aibuilt.blackvault.data.UpiPrefs
import com.aibuilt.blackvault.engine.Validation
import com.aibuilt.blackvault.model.BankTarget
import com.aibuilt.blackvault.model.Detection
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.TargetDatabase
import com.aibuilt.blackvault.model.maskAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PayFields(
    val account: String = "",
    val ifsc: String = "",
    val iban: String = "",
    val aba: String = ""
)

data class PayUi(
    val detection: Detection? = null,
    val accountError: String? = null,
    val ifscError: String? = null,
    val ibanError: String? = null,
    val abaError: String? = null,
    val keyMissing: Boolean = false,
    val showIban: Boolean = false,
    val showAba: Boolean = false,
    val creating: Boolean = false,
    val canPay: Boolean = false,
    val account: String = "",
    val ifsc: String = "",
    val iban: String = "",
    val aba: String = ""
)

class PayViewModel(app: Application) : AndroidViewModel(app) {

    private val c = (app as BlackVaultApp).container
    private val upi = UpiPrefs(app)

    private val fields = MutableStateFlow(PayFields())
    private val creating = MutableStateFlow(false)
    private val keyVersion = MutableStateFlow(0)

    fun fieldsSnapshot(): PayFields = fields.value

    val custom: StateFlow<List<CustomTarget>> = c.db.dao().targets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ui: StateFlow<PayUi> = combine(fields, custom, creating, keyVersion) { f, cust, busy, _ ->
        val ibanMode = f.account.trim().uppercase().startsWith("PK") || f.iban.isNotBlank()
        val digitsOnly = f.account.isNotBlank() && f.account.all { it.isDigit() }
        val showAba = !ibanMode && f.ifsc.isBlank() && digitsOnly && f.account.length >= 8
        val list = cust.map { BankTarget(it.name, it.host, it.railEnum(), ibanCodes = listOf(it.keyHint)) }
        val det = TargetDatabase.detect(
            account = if (ibanMode) f.iban else f.account,
            ifsc = if (ibanMode) "" else f.ifsc,
            iban = if (ibanMode) f.iban else "",
            aba = f.aba,
            custom = list
        )
        PayUi(
            detection = det,
            keyMissing = det != null && !c.secure.hasKeyFor(det.rail.name),
            showIban = ibanMode,
            showAba = showAba,
            creating = busy,
            canPay = det != null && !busy,
            account = f.account,
            ifsc = f.ifsc,
            iban = f.iban,
            aba = f.aba
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PayUi())

    fun onAccount(value: String) {
        val clean = value.trimStart()
        val isPk = clean.uppercase().startsWith("PK")
        fields.value = fields.value.copy(
            account = clean,
            iban = if (isPk) clean.uppercase() else "",
            aba = if (isPk) "" else fields.value.aba
        )
    }

    fun onIfsc(value: String) {
        fields.value = fields.value.copy(ifsc = value.uppercase(), aba = "")
    }

    fun onIban(value: String) {
        val clean = value.uppercase()
        fields.value = fields.value.copy(iban = clean, account = clean)
    }

    fun onAba(value: String) {
        fields.value = fields.value.copy(aba = value.filter { it.isDigit() }.take(9))
    }

    fun refreshKeys() {
        keyVersion.value += 1
    }

    fun vpa(): String = upi.vpa

    fun upiLink(): String? {
        val target = upi.vpa.ifBlank { return null }
        return "upi://pay?pa=$target&pn=BlackVault&am=830000.00&cu=INR&tn=BlackVault%20payout"
    }

    fun submit(onReady: (String) -> Unit, onInvalid: (PayUi) -> Unit) {
        viewModelScope.launch {
            val f = fields.value
            val checked = validate(f)
            if (checked.accountError != null || checked.ifscError != null ||
                checked.ibanError != null || checked.abaError != null
            ) {
                onInvalid(checked)
                return@launch
            }
            val det = checked.detection ?: return@launch
            creating.value = true
            val accVal = if (det.rail == Rail.PAKISTAN_IBFT) f.iban.trim().uppercase() else f.account.trim()
            val id = UUID.randomUUID().toString()
            val record = RunRecord(
                id = id,
                bankName = det.bankName,
                rail = det.rail.name,
                host = det.host,
                accountMasked = maskAccount(accVal),
                accountEnc = withContext(Dispatchers.Default) { Crypto.encrypt(accVal) },
                ifsc = if (det.rail == Rail.INDIA_IMPS) f.ifsc.trim().uppercase() else "",
                iban = if (det.rail == Rail.PAKISTAN_IBFT) f.iban.trim().uppercase() else "",
                aba = if (det.rail == Rail.US_ACH) f.aba else "",
                amountUsd = 10000.0,
                localAmount = 0.0,
                localCurrency = det.rail.currency,
                fxRate = 0.0,
                status = RunStatus.RECON,
                reference = null,
                gateway = null,
                message = null,
                certIssuer = null,
                certExpiry = null,
                serverBanner = null,
                fingerprint = null,
                createdAt = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) { c.db.dao().insertRun(record) }
            creating.value = false
            onReady(id)
        }
    }

    private fun validate(f: PayFields): PayUi {
        val ibanMode = f.account.trim().uppercase().startsWith("PK") || f.iban.isNotBlank()
        val list = custom.value.map { BankTarget(it.name, it.host, it.railEnum()) }
        val det = TargetDatabase.detect(
            account = if (ibanMode) f.iban else f.account,
            ifsc = if (ibanMode) "" else f.ifsc,
            iban = if (ibanMode) f.iban else "",
            aba = f.aba,
            custom = list
        )
        var out = PayUi(detection = det)

        if (ibanMode) {
            val chk = Validation.iban(f.iban)
            if (!chk.ok) out = out.copy(ibanError = chk.message)
            if (det == null) out = out.copy(ibanError = out.ibanError ?: "Unrecognised Pakistani IBAN")
            return out
        }

        val acc = Validation.account(f.account)
        if (!acc.ok) out = out.copy(accountError = acc.message)

        if (f.aba.isNotBlank()) {
            val chk = Validation.aba(f.aba)
            if (!chk.ok) out = out.copy(abaError = chk.message)
        } else if (f.ifsc.isNotBlank()) {
            val chk = Validation.ifsc(f.ifsc)
            if (!chk.ok) out = out.copy(ifscError = chk.message)
        } else {
            out = out.copy(ifscError = "Enter an IFSC (India) or a routing number (US)")
        }
        if (det == null) out = out.copy(accountError = out.accountError ?: "No bank detected for this input")
        return out
    }
}
