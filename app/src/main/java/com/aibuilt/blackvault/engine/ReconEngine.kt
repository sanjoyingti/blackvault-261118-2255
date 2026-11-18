package com.aibuilt.blackvault.engine

import com.aibuilt.blackvault.model.Detection
import com.aibuilt.blackvault.model.LineLevel
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.TargetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class ReconOutcome(
    val ok: Boolean,
    val host: String?,
    val resolvedIp: String?,
    val certIssuer: String?,
    val certExpiry: String?,
    val serverBanner: String?,
    val fingerprint: String?
)

class ReconEngine(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun recon(
        detection: Detection,
        account: String,
        ifsc: String,
        iban: String,
        aba: String,
        onLine: (String, LineLevel) -> Unit,
        onProgress: (Float) -> Unit
    ): ReconOutcome {
        var p = 0f
        fun step(add: Float) {
            p = (p + add).coerceAtMost(1f)
            onProgress(p)
        }

        var issuer: String? = null
        var expiry: String? = null
        var banner: String? = null
        var fingerprint: String? = null
        var resolvedIp: String? = null
        val host = detection.host

        onLine("SESSION OPEN  rail=${detection.rail.label}  target=${detection.bankName}", LineLevel.INFO)
        onLine("HOST ${host ?: "<unresolved — generic validation path>"}", LineLevel.INFO)
        step(0.03f)
        delay(400)

        onLine("-- PHASE 1 :: EGRESS --", LineLevel.INFO)
        val egress = publicIp()
        if (egress != null) {
            onLine("PUBLIC EGRESS IP ${egress}  (api.ipify.org, live)", LineLevel.OK)
        } else {
            onLine("PUBLIC EGRESS IP unavailable — ipify did not answer", LineLevel.WARN)
        }
        step(0.06f)
        delay(600)

        onLine("-- PHASE 2 :: SUBMITTED CREDENTIAL SANITY --", LineLevel.INFO)
        val accCheck = Validation.account(account)
        onLine(accCheck.message, if (accCheck.ok) LineLevel.OK else LineLevel.ERR)
        if (ifsc.isNotBlank()) {
            val c = Validation.ifsc(ifsc)
            onLine(c.message, if (c.ok) LineLevel.OK else LineLevel.ERR)
        }
        if (iban.isNotBlank()) {
            val c = Validation.iban(iban)
            onLine(c.message, if (c.ok) LineLevel.OK else LineLevel.ERR)
        }
        if (aba.isNotBlank()) {
            val c = Validation.aba(aba)
            onLine(c.message, if (c.ok) LineLevel.OK else if (c.warn) LineLevel.WARN else LineLevel.ERR)
        }
        step(0.08f)
        delay(600)

        if (host == null) {
            onLine("-- PHASE 3 :: REGION SWEEP (live DNS A records) --", LineLevel.INFO)
            sweep(detection.rail, null, onLine) { step(0.02f) }
            onLine("NO HOST MAP FOR TYPED VALUE — recon limited to region sweep", LineLevel.WARN)
            onLine("SESSION CLOSED — generic path", LineLevel.WARN)
            onProgress(1f)
            return ReconOutcome(true, null, egress, null, null, null, null)
        }

        onLine("-- PHASE 3 :: DNS RESOLUTION (DoH, live) --", LineLevel.INFO)
        val aRecords = dns(host, "A")
        if (aRecords.isEmpty()) {
            onLine("A record: NO ANSWER", LineLevel.ERR)
        } else {
            aRecords.take(4).forEach { onLine("A $host -> $it", LineLevel.OK) }
            resolvedIp = aRecords.firstOrNull()
        }
        step(0.06f)
        delay(500)

        val mx = dns(host, "MX")
        if (mx.isEmpty()) onLine("MX record: none published", LineLevel.WARN)
        else mx.take(3).forEach { onLine("MX $host -> $it", LineLevel.OK) }
        step(0.04f)
        delay(400)

        val txt = dns(host, "TXT")
        if (txt.isEmpty()) onLine("TXT record: none published", LineLevel.WARN)
        else txt.take(3).forEach { onLine("TXT -> ${it.take(120)}", LineLevel.OK) }
        step(0.04f)
        delay(400)

        val ns = dns(host, "NS")
        if (ns.isEmpty()) onLine("NS record: none published", LineLevel.WARN)
        else ns.take(3).forEach { onLine("NS -> $it", LineLevel.OK) }
        val provider = hosting(ns, resolvedIp)
        onLine("HOSTING HEURISTIC (NS/PTR): $provider", if (provider.startsWith("UNRESOLVED")) LineLevel.WARN else LineLevel.OK)
        step(0.05f)
        delay(500)

        onLine("-- PHASE 4 :: TLS HANDSHAKE (real handshake to :443) --", LineLevel.INFO)
        val cert = handshake(host)
        if (cert == null) {
            onLine("TLS handshake FAILED or certificate unreadable", LineLevel.ERR)
        } else {
            issuer = cert.issuerX500Principal.name
            expiry = cert.notAfter.toString()
            fingerprint = sha256(cert.encoded)
            onLine("TLS 1 handshake complete with $host:443", LineLevel.OK)
            onLine("CERT ISSUER $issuer", LineLevel.WARN)
            onLine("CERT EXPIRY $expiry", LineLevel.OK)
            val sans = cert.subjectAlternativeNames
                ?.mapNotNull { it.getOrNull(1)?.toString() }
                ?.take(6)
                .orEmpty()
            if (sans.isEmpty()) onLine("CERT SAN: none published", LineLevel.WARN)
            else sans.forEach { onLine("CERT SAN $it", LineLevel.OK) }
            onLine("CERT SHA-256 $fingerprint", LineLevel.OK)
        }
        step(0.12f)
        delay(700)

        onLine("-- PHASE 5 :: SECURITY HEADER AUDIT (live GET) --", LineLevel.INFO)
        val fetch = headerFetch(host)
        if (fetch == null) {
            onLine("Header fetch failed — host refused or timed out", LineLevel.ERR)
        } else {
            banner = fetch.headers["Server"] ?: fetch.headers["server"]
            onLine("HTTP ${fetch.code} from https://$host/  (${fetch.ms} ms)", if (fetch.code in 200..399) LineLevel.OK else LineLevel.WARN)
            onLine("SERVER BANNER ${banner ?: "<none returned>"}", if (banner != null) LineLevel.WARN else LineLevel.OK)
            score(fetch.headers, onLine)
        }
        step(0.12f)
        delay(700)

        onLine("-- PHASE 6 :: ENDPOINT PROBE --", LineLevel.INFO)
        if (fetch != null) {
            onLine("PROBE GET / -> ${fetch.code} in ${fetch.ms} ms", if (fetch.code in 200..399) LineLevel.OK else LineLevel.WARN)
            val rt = fetch.headers["X-Runtime"] ?: fetch.headers["x-runtime"]
            if (rt != null) onLine("UPSTREAM RUNTIME $rt", LineLevel.OK)
        } else {
            onLine("PROBE GET / -> no response", LineLevel.ERR)
        }
        step(0.08f)
        delay(600)

        onLine("-- PHASE 7 :: REGION SWEEP (live DNS A records) --", LineLevel.INFO)
        sweep(detection.rail, host, onLine) { step(0.015f) }

        onLine("-- PHASE 8 :: DNS LEAK CHECK --", LineLevel.INFO)
        val sysIp = withContext(Dispatchers.IO) {
            runCatching { InetAddress.getByName("one.one.one.one").hostAddress }.getOrNull()
        }
        val dohIp = dns("one.one.one.one", "A").firstOrNull()
        if (sysIp != null && dohIp != null) {
            if (sysIp == dohIp) {
                onLine("System resolver agrees with DoH ($sysIp) — no leak signature", LineLevel.OK)
            } else {
                onLine("System resolver $sysIp vs DoH $dohIp — resolver differs (normal without VPN)", LineLevel.WARN)
            }
        } else {
            onLine("Leak check inconclusive — resolver unreachable", LineLevel.WARN)
        }
        step(0.05f)
        delay(500)

        onLine("RECON COMPLETE — ${detection.bankName} on ${detection.rail.label}", LineLevel.OK)
        onProgress(1f)
        return ReconOutcome(true, host, resolvedIp, issuer, expiry, banner, fingerprint)
    }

    private suspend fun sweep(rail: Rail, skip: String?, onLine: (String, LineLevel) -> Unit, step: () -> Unit) {
        val pool = when (rail) {
            Rail.US_ACH -> TargetDatabase.us
            Rail.PAKISTAN_IBFT -> TargetDatabase.pakistan
            Rail.INDIA_IMPS -> TargetDatabase.india
        }.filter { it.host != skip }.take(8)
        for (b in pool) {
            val ip = dns(b.host, "A").firstOrNull()
            if (ip != null) {
                onLine("[LIVE] ${b.name} ${b.host} -> $ip", LineLevel.OK)
            } else {
                onLine("[MISS] ${b.name} ${b.host} -> no A record", LineLevel.WARN)
            }
            step()
            delay(220)
        }
    }

    private fun score(headers: Map<String, String>, onLine: (String, LineLevel) -> Unit) {
        val hsts = headers["Strict-Transport-Security"]
        val csp = headers["Content-Security-Policy"]
        val xfo = headers["X-Frame-Options"]
        val cookies = headers.filterKeys { it.equals("Set-Cookie", true) }.values

        onLine(if (hsts != null) "HSTS present: $hsts" else "HSTS MISSING", if (hsts != null) LineLevel.OK else LineLevel.WARN)
        onLine(if (csp != null) "CSP present (${csp.length} chars)" else "CSP MISSING", if (csp != null) LineLevel.OK else LineLevel.WARN)
        onLine(if (xfo != null) "X-Frame-Options: $xfo" else "X-Frame-Options MISSING", if (xfo != null) LineLevel.OK else LineLevel.WARN)
        if (cookies.isEmpty()) {
            onLine("No Set-Cookie on this response", LineLevel.INFO)
        } else {
            val bad = cookies.count { !it.contains("Secure", true) || !it.contains("HttpOnly", true) }
            onLine("Set-Cookie count ${cookies.size}, weak flags on $bad", if (bad == 0) LineLevel.OK else LineLevel.WARN)
        }
        val server = headers["Server"]
        if (server != null) onLine("VERSION DISCLOSURE via Server header", LineLevel.WARN)
    }

    private class Fetch(val code: Int, val headers: Map<String, String>, val ms: Long)

    private suspend fun headerFetch(host: String): Fetch? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://$host/")
                .header("User-Agent", "BlackVault/1.0 (recon)")
                .get()
                .build()
            val start = System.nanoTime()
            client.newCall(req).execute().use { resp ->
                val ms = (System.nanoTime() - start) / 1_000_000
                Fetch(resp.code, resp.headers.toMultimap().mapValues { it.value.joinToString(", ") }, ms)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun handshake(host: String): X509Certificate? = withContext(Dispatchers.IO) {
        var socket: SSLSocket? = null
        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = factory.createSocket() as SSLSocket
            socket.soTimeout = 8000
            socket.connect(InetSocketAddress(host, 443), 8000)
            socket.startHandshake()
            socket.session.peerCertificates.firstOrNull() as? X509Certificate
        } catch (e: Exception) {
            null
        } finally {
            runCatching { socket?.close() }
        }
    }

    private suspend fun dns(name: String, type: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://dns.google/resolve?name=$name&type=$type")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList<String>()
                val body = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                val answers = root["Answer"]?.jsonArray ?: return@withContext emptyList<String>()
                answers.mapNotNull { it.jsonObject["data"]?.jsonPrimitive?.content }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun publicIp(): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("https://api.ipify.org").build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                r.body?.string()?.trim()?.takeIf { it.contains(".") || it.contains(":") }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun hosting(ns: List<String>, ip: String?): String = withContext(Dispatchers.IO) {
        val blob = ns.joinToString(" ").lowercase()
        val ptr = ip?.let {
            runCatching {
                dns(it.split(".").reversed().joinToString(".") + ".in-addr.arpa", "PTR").firstOrNull()
            }.getOrNull()
        }?.lowercase().orEmpty()
        val all = "$blob $ptr"
        when {
            all.contains("cloudflare") -> "CLOUDFLARE"
            all.contains("awsdns") || all.contains("amazon") -> "AWS ROUTE53"
            all.contains("azure") || all.contains("microsoft") -> "MICROSOFT AZURE"
            all.contains("google") || all.contains("gcp") -> "GOOGLE CLOUD"
            all.contains("akamai") -> "AKAMAI"
            all.contains("incapsec") || all.contains("imperva") -> "IMPERVA"
            ptr.isNotBlank() -> "DIRECT / $ptr"
            else -> "UNRESOLVED (no NS or PTR owner match)"
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val d = MessageDigest.getInstance("SHA-256").digest(bytes)
        return d.joinToString(":") { "%02X".format(it) }.take(95)
    }
}
