package com.aibuilt.blackvault.work

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aibuilt.blackvault.BlackVaultApp
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.RunStatus
import com.aibuilt.blackvault.model.nowStamp
import kotlinx.coroutines.flow.first

class TrancheWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BlackVaultApp ?: return Result.failure()
        val c = app.container
        val dao = c.db.dao()
        val runId = inputData.getString(KEY_RUN) ?: return Result.failure()
        val trancheId = inputData.getString(KEY_TRANCHE) ?: return Result.failure()

        val run = dao.runById(runId) ?: return Result.failure()
        val tranche = dao.trancheById(trancheId) ?: return Result.failure()

        val rail = runCatching { Rail.valueOf(run.rail) }.getOrDefault(Rail.INDIA_IMPS)
        val res = c.payout.payTranche(
            rail = rail,
            account = com.aibuilt.blackvault.data.Crypto.decrypt(run.accountEnc).ifBlank { run.accountMasked },
            ifsc = run.ifsc,
            iban = run.iban,
            aba = run.aba,
            amountLocal = tranche.amountLocal,
            runId = runId,
            index = tranche.indexNo
        )

        dao.finishTranche(
            trancheId,
            if (res.success) STATUS_DONE else STATUS_FAILED,
            res.reference,
            res.message
        )

        val all = dao.tranchesNow(runId)
        val pending = all.count { it.status == STATUS_PENDING }
        if (pending > 0) return Result.success()

        val done = all.filter { it.status == STATUS_DONE }
        val failed = all.filter { it.status == STATUS_FAILED }
        val refs = done.mapNotNull { it.reference }.joinToString(" | ")

        if (done.isEmpty()) {
            dao.updateRun(
                run.copy(
                    status = RunStatus.FAILED,
                    reference = null,
                    gateway = c.payout.gatewayFor(rail),
                    message = failed.firstOrNull()?.message ?: "All tranches rejected by the gateway"
                )
            )
        } else {
            dao.updateRun(
                run.copy(
                    status = RunStatus.CONFIRMED,
                    reference = refs,
                    gateway = c.payout.gatewayFor(rail),
                    message = if (failed.isEmpty()) {
                        "All ${done.size} tranche(s) accepted by the gateway"
                    } else {
                        "${done.size} of ${all.size} tranches accepted, ${failed.size} rejected"
                    }
                )
            )
        }

        notify(run.bankName, done.isNotEmpty(), refs)

        val hook = c.settings.webhookUrl.first()
        if (hook.isNotBlank()) {
            c.payout.webhook(
                hook,
                """{"run":"$runId","status":"${if (done.isEmpty()) "FAILED" else "CONFIRMED"}","refs":"$refs","at":"${nowStamp()}"}"""
            )
        }
        return Result.success()
    }

    private fun notify(bank: String, ok: Boolean, refs: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val mgr = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val n = NotificationCompat.Builder(applicationContext, BlackVaultApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(if (ok) "PAYOUT ACCEPTED — $bank" else "PAYOUT REJECTED — $bank")
            .setContentText(refs.ifBlank { "Open BLACKVAULT for the receipt" })
            .setAutoCancel(true)
            .build()
        runCatching { mgr.notify(System.currentTimeMillis().toInt(), n) }
    }

    companion object {
        const val KEY_RUN = "runId"
        const val KEY_TRANCHE = "trancheId"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_DONE = "DONE"
        const val STATUS_FAILED = "FAILED"
        fun unique(runId: String) = "vault_run_$runId"
    }
}
