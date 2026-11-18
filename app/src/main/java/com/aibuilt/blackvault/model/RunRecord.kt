package com.aibuilt.blackvault.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object RunStatus {
    const val RECON = "RECON"
    const val PAYING = "PAYING"
    const val CONFIRMED = "CONFIRMED"
    const val FAILED = "FAILED"
    const val UPI = "UPI_HANDOFF"
    const val ABORTED = "ABORTED"
}

@Entity(tableName = "runs")
data class RunRecord(
    @PrimaryKey val id: String,
    val bankName: String,
    val rail: String,
    val host: String?,
    val accountMasked: String,
    val accountEnc: String,
    val ifsc: String,
    val iban: String,
    val aba: String,
    val amountUsd: Double,
    val localAmount: Double,
    val localCurrency: String,
    val fxRate: Double,
    val status: String,
    val reference: String?,
    val gateway: String?,
    val message: String?,
    val certIssuer: String?,
    val certExpiry: String?,
    val serverBanner: String?,
    val fingerprint: String?,
    val createdAt: Long
)

@Entity(tableName = "tranches")
data class Tranche(
    @PrimaryKey val id: String,
    val runId: String,
    val indexNo: Int,
    val amountLocal: Double,
    val status: String,
    val reference: String?,
    val message: String?,
    val createdAt: Long
)
