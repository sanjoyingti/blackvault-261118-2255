=== ABOUT: Fixed the run brain so it can reach the app's shared services and never crashes on startup ===
package com.aibuilt.blackvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.data.Crypto
import com.aibuilt.blackvault.data.UpiPrefs
import com.aibuilt.blackvault.engine.RailLimits
import com.aibuilt.blackvault.model.ConsoleLine
import com.aibuilt.blackvault.model.Detection
import com.aibuilt.blackvault.model.LineLevel
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.TargetDatabase
import com.aibuilt.blackvault.model.Tranche
import com.aibuilt.blackvault.model.nowStamp
import com.aibuilt.blackvault.work.TrancheWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.random.Random

class RunViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val c = (app as BlackVaultApp).container
    private val dao = c.db.dao()
    private val runId: String = handle.get<String>("id").orEmpty()
    private val upi = UpiPrefs(app)

    val run: StateFlow<RunRecord?> = dao.runFlow(runId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val progress = MutableStateFlow(0f)
    val blocked = MutableStateFlow(0)
    val fakeIp = MutableStateFlow("0.0.0.0")
    val hops = MutableStateFlow<List<String>>(emptyList())
    val done = MutableStateFlow(false)
    val motion = c.settings.stealthMotion.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var job: Job? = null
    private var started = false
    private var seq = 0L

    fun start() {
        if (started) return
        started = true
        job = viewModelScope.launch { execute() }
        viewModelScope.launch { ticker() }
        viewModelScope.launch {
            dao.runFlow(runId).collect { r ->
                if (r != null && (r.status == RunStatus.CONFIRMED || r.status == RunStatus.FAILED || r.status == RunStatus.ABORTED)) {
                    done.value = true
                }
            }
        }
    }

    private fun line(text: String, level: LineLevel) {
        seq += 1
        lines.value = lines.value + ConsoleLine(seq, nowStamp(), text, level)
    }

    private suspend fun ticker() {
        var hop = 0
        while (viewModelScope.isActive) {
            if (job?.isActive != true) break
            fakeIp.value = (1..4).joinToString(".") { Random.nextInt(11, 250).toString() }
            hop += 1
            hops.value = (0 until 7).map {
                "#$it ${hopName((hop + it) % HOP_NAMES.size)} ${Random.nextInt(11, 250)}.${Random.nextInt(1, 250)}.x.x"
            }
            blocked.value += Random.nextInt(1, 4)
            delay(650)
        }
    }

    private suspend fun execute() {
        val record = withContext(Dispatchers.IO) { dao.runById(runId) } ?: return
        val detection = Detection(
            runCatching { Rail.valueOf(record.rail) }.getOrDefault(Rail.INDIA_IMPS),
            record.bankName,
            record.host
        )
        val account = Crypto.decrypt(record.accountEnc)

        line("VAULT PROCESS ${runId.take(8).uppercase(Locale.US)} — OPERATOR SESSION", LineLevel.INFO)
        line("TARGET ${record.bankName} / ${detection.rail.label}", LineLevel.INFO)
        line("ACCOUNT ${record.accountMasked}  (masked, full number encrypted at rest)", LineLevel.INFO)

        val orbot = orbotInstalled()
        if (orbot) line("TOR DAEMON: Orbot installed — Tor routing available", LineLevel.OK)
        else line("TOR: Orbot not installed — stealth layer SIMULATED, recon/payout REAL", LineLevel.WARN)

        val outcome = c.recon.recon(
            detection = detection,
            account = account,
            ifsc = record.ifsc,
            iban = record.iban,
            aba = record.aba,
            onLine = { text, level -> line(text, level) },
            onProgress = { progress.value = it * 0.7f }
        )

        withContext(Dispatchers.IO) {
            dao.updateRun(
                record.copy(
                    certIssuer = outcome.certIssuer,
                    certExpiry = outcome.certExpiry,
                    serverBanner = outcome.serverBanner,
                    fingerprint = outcome.fingerprint
                )
            )
        }

        line("-- PHASE 9 :: PAYOUT LEG --", LineLevel.INFO)
        val rail = detection.rail
        if (!c.secure.hasKeyFor(rail.name)) {
            line("NO FUNDING KEY FOR ${rail.label} — transfer NOT live", LineLevel.WARN)
            val vpa = upi.vpa
            if (rail == Rail.INDIA_IMPS && vpa.isNotBlank()) {
                line("UPI hand-off available for VPA $vpa — use the button below", LineLevel.WARN)
            }
            withContext(Dispatchers.IO) {
                dao.updateRun(record.copy(status = RunStatus.FAILED, message = "NO FUNDING KEY — transfer not live"))
            }
            progress.value = 1f
            return
        }

        line("VALUING \$10,000.00 into ${rail.currency} at live mid-rate …", LineLevel.INFO)
        val rate = c.payout.fxRate(rail.currency)
        val local = 10000.0 * rate
        line("FX 1 USD = ${"%.4f".format(Locale.US, rate)} ${rail.currency} — total ${"%,.2f".format(Locale.US, local)} ${rail.currency}", LineLevel.OK)

        val limit = RailLimits.perTxn(rail)
        val count = ceil(local / limit).toInt().coerceAtLeast(1)
        line("PER-TXN LIMIT ${"%,.2f".format(Locale.US, limit)} ${rail.currency} — splitting into $count tranche(s)", if (count > 1) LineLevel.WARN else LineLevel.INFO)

        withContext(Dispatchers.IO) {
            dao.updateRun(record.copy(status = RunStatus.PAYING, localAmount = local, fxRate = rate, localCurrency = rail.currency))
        }

        val slice = local / count
        val builders = mutableListOf<OneTimeWorkRequestBuilder<TrancheWorker>>()
        for (i in 0 until count) {
            val tId = UUID.randomUUID().toString()
            webInsert(
                Tranche(
                    id = tId,
                    runId = runId,
                    indexNo = i + 1,
                    amountLocal = if (i == count - 1) local - slice * (count - 1) else slice,
                    status = TrancheWorker.STATUS_PENDING,
                    reference = null,
                    message = null,
                    createdAt = System.currentTimeMillis()
                )
            )
            builders += OneTimeWorkRequestBuilder<TrancheWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(TrancheWorker.KEY_RUN, runId)
                        .putString(TrancheWorker.KEY_TRANCHE, tId)
                        .build()
                )
            line("TRANCH ${i + 1}/$count queued — ${"%,.2f".format(Locale.US, slice)} ${rail.currency}", LineLevel.INFO)
        }

        progress.value = 0.85f
        val wm = WorkManager.getInstance(getApplication())
        var chain = wm.beginUniqueWork(TrancheWorker.unique(runId), ExistingWorkPolicy.REPLACE, builders.first().build())
        for (i in 1 until builders.size) chain = chain.then(builders[i].build())
        withContext(Dispatchers.IO) { chain.enqueue() }

        line("GATEWAY ${c.payout.gatewayFor(rail)} — tranche chain dispatched to WorkManager", LineLevel.OK)

        var ticks = 0
        while (viewModelScope.isActive && !done.value && ticks < 300) {
            delay(500)
            ticks++
            progress.value = (0.85f + ticks * 0.0005f).coerceAtMost(0.99f)
        }
        if (done.value) {
            val latest = withContext(Dispatchers.IO) { dao.runById(runId) }
            if (latest?.status == RunStatus.CONFIRMED) {
                line("GATEWAY ACCEPTED — reference ${latest.reference}", LineLevel.OK)
            } else {
                line("GATEWAY REJECTED — ${latest?.message ?: "see receipt"}", LineLevel.ERR)
            }
            progress.value = 1f
        } else {
            line("TIMEOUT waiting for gateway — check the receipt for late settlement", LineLevel.WARN)
            done.value = true
        }
    }

    private suspend fun webInsert(t: Tranche) {
        withContext(Dispatchers.IO) { dao.insertTranche(t) }
    }

    fun abort() {
        viewModelScope.launch {
            job?.cancel()
            WorkManager.getInstance(getApplication()).cancelUniqueWork(TrancheWorker.unique(runId))
            val rec = withContext(Dispatchers.IO) { dao.runById(runId) }
            if (rec != null && (rec.status == RunStatus.RECON || rec.status == RunStatus.PAYING)) {
                withContext(Dispatchers.IO) {
                    dao.updateRun(rec.copy(status = RunStatus.ABORTED, message = "Aborted by operator"))
                }
                line("ABORT SIGNAL SENT — chain cancelled", LineLevel.ERR)
            }
            done.value = true
            progress.value = 1f
        }
    }

    fun upiLink(): String? {
        val vpa = upi.vpa.ifBlank { return null }
        val r = run.value ?: return null
        val amount = if (r.localAmount > 0) r.localAmount else 10000.0 * 83.2
        return "upi://pay?pa=$vpa&pn=BlackVault&am=${"%.2f".format(Locale.US, amount)}&cu=INR&tn=BlackVault ${r.accountMasked}"
    }

    private fun orbotInstalled(): Boolean = runCatching {
        getApplication<Application>().packageManager.getPackageInfo("org.torproject.android", 0)
        true
    }.getOrDefault(false)

    private fun hopName(i: Int): String = HOP_NAMES[i]

    companion object {
        private val HOP_NAMES = TargetDatabase.all.take(24).map { it.name }
    }
}
