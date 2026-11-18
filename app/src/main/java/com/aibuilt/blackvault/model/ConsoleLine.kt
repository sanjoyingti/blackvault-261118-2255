package com.aibuilt.blackvault.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LineLevel { OK, WARN, ERR, INFO }

data class ConsoleLine(
    val id: Long,
    val time: String,
    val text: String,
    val level: LineLevel
)

private val STAMP = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

fun nowStamp(): String = STAMP.format(Date())

fun maskAccount(account: String): String =
    if (account.length <= 4) account else "****" + account.takeLast(4)

fun money(value: Double, symbol: String = ""): String =
    symbol + String.format(Locale.US, "%,.2f", value)

fun stampOfDate(epoch: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US).format(Date(epoch))
