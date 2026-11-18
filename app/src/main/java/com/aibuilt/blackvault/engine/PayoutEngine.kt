package com.aibuilt.blackvault.engine

import com.aibuilt.blackvault.data.SecureStore
import com.aibuilt.blackvault.model.Rail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

data class PayResult(
    val success: Boolean,
    val reference: String?,
    val gateway: String,
    val message: String
)

object RailLimits {
    fun perTxn(rail: Rail): Double = when (rail) {
        Rail.INDIA_IMPS -> 500_000.0
        Rail.US_ACH -> 25_000.0
        Rail.PAKISTAN_IBFT -> 500_000.0
    }
}

class PayoutEngine(
    private val client: OkHttpClient,
    private val secure: SecureStore
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun fxRate(target: String): Double = withContext(Dispatchers.IO) {
        if (target == "USD") return@withContext 1.0
        try {
            val req = Request.Builder().url("https://open.er-api.com/v6/latest/USD").build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext fallback(target)
                val body = r.body?.string().orEmpty()
                val rates = json.parseToJsonElement(body).jsonObject["rates"]?.jsonObject
                rates?.get(target)?.jsonPrimitive?.content?.toDoubleOrNull() ?: fallback(target)
            }
        } catch (e: Exception) {
            fallback(target)
        }
    }

    private fun fallback(target: String): Double = when (target) {
        "INR" -> 83.2
        "PKR" -> 278.5
        else -> 1.0
    }

    suspend fun payTranche(
        rail: Rail,
        account: String,
        ifsc: String,
        iban: String,
        aba: String,
        amountLocal: Double,
        runId: String,
        index: Int
    ): PayResult = when (rail) {
        Rail.INDIA_IMPS -> payIndia(account, ifsc, amountLocal, runId, index)
        Rail.US_ACH -> payUs(account, aba, amountLocal, runId, index)
        Rail.PAKISTAN_IBFT -> payPakistan(iban.ifBlank { account }, amountLocal, runId, index)
    }

    private fun gatewayName(rail: Rail): String = when (rail) {
        Rail.INDIA_IMPS ->
            if (secure.cashfreeClientId.isNotBlank() && secure.cashfreeSecret.isNotBlank()) "CASHFREE" else "RAZORPAY_X"
        Rail.US_ACH -> "STRIPE"
        Rail.PAKISTAN_IBFT -> "PAYFAST"
    }

    private fun reference(prefix: String): String =
        prefix + System.currentTimeMillis().toString(36).uppercase(Locale.US)

    // ---------------- INDIA ----------------

    private suspend fun payIndia(
        account: String,
        ifsc: String,
        amountLocal: Double,
        runId: String,
        index: Int
    ): PayResult = withContext(Dispatchers.IO) {
        val useCashfree = secure.cashfreeClientId.isNotBlank() && secure.cashfreeSecret.isNotBlank()
        if (useCashfree) return@withContext cashfree(account, ifsc, amountLocal, runId, index)
        val keyId = secure.razorpayKeyId
        val secret = secure.razorpaySecret
        val holder = secure.razorpayAccount
        if (keyId.isBlank() || secret.isBlank() || holder.isBlank()) {
            return@withContext PayResult(false, null, "RAZORPAY_X", "Razorpay X key, secret or account number is missing in Settings")
        }
        val ref = reference("brv")
        val body = """
            {"account_number":"$holder","amount":${(amountLocal * 100).roundToLong()},"currency":"INR",
            "mode":"IMPS","purpose":"payout","reference_id":"$ref","queue_if_low_balance":true,
            "narration":"BlackVault payout",
            "contact":{"name":"BlackVault Payee","type":"customer","reference_id":"$ref","email":"payee@blackvault.app","contact":"9999999999"},
            "fund_account":{"account_type":"bank_account","bank_account":{"name":"BlackVault Payee","ifsc":"$ifsc","account_number":"$account"}}}
        """.trimIndent().toRequestBody(jsonMedia)

        val req = Request.Builder()
            .url("https://api.razorpay.com/v1/payouts")
            .header("Authorization", Credentials.basic(keyId, secret))
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        execute(req, "RAZORPAY_X", ref)
    }

    private suspend fun cashfree(
        account: String,
        ifsc: String,
        amountLocal: Double,
        runId: String,
        index: Int
    ): PayResult = withContext(Dispatchers.IO) {
        val cid = secure.cashfreeClientId
        val sec = secure.cashfreeSecret
        val beneId = "BV" + (runId + index).hashCode().toUInt().toString()
        val clean = account.takeLast(12)

        val addBody = """
            {"beneId":"$beneId","name":"BlackVault Payee","email":"payee@blackvault.app","phone":"9999999999",
            "bankAccount":"$clean","ifsc":"$ifsc","address1":"BlackVault"}
        """.trimIndent().toRequestBody(jsonMedia)

        val addReq = Request.Builder()
            .url("https://api.cashfree.com/payout/v1/addBeneficiary")
            .header("x-client-id", cid)
            .header("x-client-secret", sec)
            .header("Content-Type", "application/json")
            .post(addBody)
            .build()
        runCatching { client.newCall(addReq).execute().use { it.body?.string() } }

        val transferId = reference("bvt")
        val payBody = """
            {"beneId":"$beneId","amount":"${"%.2f".format(Locale.US, amountLocal)}","transferId":"$transferId","transferMode":"imps"}
        """.trimIndent().toRequestBody(jsonMedia)

        val payReq = Request.Builder()
            .url("https://api.cashfree.com/payout/v1/directTransfer")
            .header("x-client-id", cid)
            .header("x-client-secret", sec)
            .header("Content-Type", "application/json")
            .post(payBody)
            .build()
        execute(payReq, "CASHFREE", transferId)
    }

    // ---------------- US ----------------

    private suspend fun payUs(
        account: String,
        aba: String,
        amountLocal: Double,
        runId: String,
        index: Int
    ): PayResult = withContext(Dispatchers.IO) {
        val key = secure.stripeKey
        if (key.isBlank()) {
            return@withContext PayResult(false, null, "STRIPE", "Stripe secret key is missing in Settings")
        }
        try {
            val pmForm = FormBody.Builder()
                .add("type", "us_bank_account")
                .add("us_bank_account[account_number]", account)
                .add("us_bank_account[routing_number]", aba)
                .add("us_bank_account[account_holder_type]", "individual")
                .add("billing_details[name]", "BlackVault Payee")
                .build()

            val pmReq = Request.Builder()
                .url("https://api.stripe.com/v1/payment_methods")
                .header("Authorization", "Bearer $key")
                .post(pmForm)
                .build()

            val pmId = client.newCall(pmReq).execute().use { r ->
                val b = r.body?.string().orEmpty()
                if (!r.isSuccessful) return@withContext PayResult(false, null, "STRIPE", errorOf(b, "payment_method"))
                json.parseToJsonElement(b).jsonObject["id"]?.jsonPrimitive?.content
            } ?: return@withContext PayResult(false, null, "STRIPE", "Stripe did not return a payment method id")

            val acceptedAt = (System.currentTimeMillis() / 1000).toString()
            val piForm = FormBody.Builder()
                .add("amount", (amountLocal * 100).roundToLong().toString())
                .add("currency", "usd")
                .add("payment_method_types[]", "us_bank_account")
                .add("payment_method", pmId)
                .add("confirm", "true")
                .add("description", "BlackVault ACH ${runId.take(8)}-$index")
                .add("mandate_data[customer_acceptance][type]", "online")
                .add("mandate_data[customer_acceptance][accepted_at]", acceptedAt)
                .add("payment_method_options[us_bank_account][verification_method]", "automatic")
                .build()

            val piReq = Request.Builder()
                .url("https://api.stripe.com/v1/payment_intents")
                .header("Authorization", "Bearer $key")
                .post(piForm)
                .build()

            client.newCall(piReq).execute().use { r ->
                val b = r.body?.string().orEmpty()
                if (!r.isSuccessful) return@withContext PayResult(false, null, "STRIPE", errorOf(b, "payment_intent"))
                val obj = json.parseToJsonElement(b).jsonObject
                val id = obj["id"]?.jsonPrimitive?.content
                val status = obj["status"]?.jsonPrimitive?.content ?: "unknown"
                PayResult(true, id, "STRIPE", "ACH accepted by Stripe (status: $status)")
            }
        } catch (e: Exception) {
            PayResult(false, null, "STRIPE", e.message ?: "Stripe call failed")
        }
    }

    // ---------------- PAKISTAN ----------------

    private suspend fun payPakistan(
        iban: String,
        amountLocal: Double,
        runId: String,
        index: Int
    ): PayResult = withContext(Dispatchers.IO) {
        val merchant = secure.payfastMerchant
        val secured = secure.payfastSecured
        if (merchant.isBlank() || secured.isBlank()) {
            return@withContext PayResult(false, null, "PAYFAST", "PayFast merchant id or secured key is missing in Settings")
        }
        val ref = reference("bvp")
        val body = """
            {"merchant_id":"$merchant","amount":"${"%.2f".format(Locale.US, amountLocal)}","currency":"PKR",
            "iban":"${iban.uppercase().replace(" ", "")}","order_id":"$ref","description":"BlackVault IBFT",
            "txn_type":"IBFT","customer_email":"payee@blackvault.app","customer_phone":"03001234567"}
        """.trimIndent().toRequestBody(jsonMedia)

        val req = Request.Builder()
            .url("https://api.payfast.pk/v1/payout")
            .header("merchant-id", merchant)
            .header("secured-key", secured)
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        execute(req, "PAYFAST", ref)
    }

    // ---------------- helpers ----------------

    private fun errorOf(body: String, kind: String): String = runCatching {
        val obj = json.parseToJsonElement(body).jsonObject
        val err = obj["error"]?.jsonObject
        val desc = err?.get("description")?.jsonPrimitive?.content
            ?: err?.get("message")?.jsonPrimitive?.content
            ?: obj["message"]?.jsonPrimitive?.content
            ?: "HTTP rejected the $kind"
        desc
    }.getOrElse { "HTTP rejected the $kind" }

    private fun execute(req: Request, gateway: String, localRef: String): PayResult {
        return try {
            client.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    PayResult(false, localRef, gateway, "HTTP ${r.code}: " + errorOf(body, "payout"))
                } else {
                    val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    val remote = obj?.get("id")?.jsonPrimitive?.content
                        ?: obj?.get("transaction_id")?.jsonPrimitive?.content
                        ?: obj?.get("transferId")?.jsonPrimitive?.content
                    PayResult(true, remote ?: localRef, gateway, "Accepted by $gateway")
                }
            }
        } catch (e: Exception) {
            PayResult(false, localRef, gateway, e.message ?: "$gateway unreachable")
        }
    }

    suspend fun webhook(url: String, payload: String) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        runCatching {
            val req = Request.Builder().url(url)
                .post(payload.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().close()
        }
    }

    fun gatewayFor(rail: Rail): String = gatewayName(rail)

    fun stamp(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
}
