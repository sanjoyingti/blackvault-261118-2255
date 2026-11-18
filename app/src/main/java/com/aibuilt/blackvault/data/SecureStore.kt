package com.aibuilt.blackvault.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "blackvault_secure",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("blackvault_secure_plain", Context.MODE_PRIVATE)
    }

    fun get(key: String): String = prefs.getString(key, "").orEmpty()

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    val razorpayKeyId: String get() = get(K_RAZORPAY_ID)
    val razorpaySecret: String get() = get(K_RAZORPAY_SECRET)
    val razorpayAccount: String get() = get(K_RAZORPAY_ACC)
    val cashfreeClientId: String get() = get(K_CASHFREE_ID)
    val cashfreeSecret: String get() = get(K_CASHFREE_SECRET)
    val stripeKey: String get() = get(K_STRIPE)
    val payfastMerchant: String get() = get(K_PAYFAST_MERCHANT)
    val payfastSecured: String get() = get(K_PAYFAST_SECURED)

    fun hasIndiaKey(): Boolean =
        (razorpayKeyId.isNotBlank() && razorpaySecret.isNotBlank() && razorpayAccount.isNotBlank()) ||
            (cashfreeClientId.isNotBlank() && cashfreeSecret.isNotBlank())

    fun hasUsKey(): Boolean = stripeKey.isNotBlank()

    fun hasPakistanKey(): Boolean = payfastMerchant.isNotBlank() && payfastSecured.isNotBlank()

    fun hasKeyFor(railName: String): Boolean = when (railName) {
        "INDIA_IMPS" -> hasIndiaKey()
        "US_ACH" -> hasUsKey()
        "PAKISTAN_IBFT" -> hasPakistanKey()
        else -> false
    }

    companion object {
        const val K_RAZORPAY_ID = "razorpay_key_id"
        const val K_RAZORPAY_SECRET = "razorpay_key_secret"
        const val K_RAZORPAY_ACC = "razorpay_account_number"
        const val K_CASHFREE_ID = "cashfree_client_id"
        const val K_CASHFREE_SECRET = "cashfree_client_secret"
        const val K_STRIPE = "stripe_secret_key"
        const val K_PAYFAST_MERCHANT = "payfast_merchant_id"
        const val K_PAYFAST_SECURED = "payfast_secured_key"
    }
}
