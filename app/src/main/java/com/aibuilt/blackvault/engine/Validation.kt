package com.aibuilt.blackvault.engine

import java.math.BigInteger

data class Check(val ok: Boolean, val message: String, val warn: Boolean = false)

object Validation {

    private val IFSC = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    private val IBAN_PK = Regex("^PK[0-9]{2}[A-Z]{4}[0-9]{16}$")

    fun account(value: String): Check {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> Check(false, "Account number required")
            digits.length != value.trim().length -> Check(false, "Digits only")
            digits.length < 6 -> Check(false, "Too short (min 6)")
            digits.length > 20 -> Check(false, "Too long (max 20)")
            else -> Check(true, "Account number shape OK (${digits.length} digits)")
        }
    }

    fun ifsc(value: String): Check {
        val v = value.uppercase().trim()
        return when {
            v.isBlank() -> Check(false, "IFSC required for India")
            !IFSC.matches(v) -> Check(false, "IFSC must be 4 letters, 0, then 6 alphanumerics")
            v[4] != '0' -> Check(false, "5th IFSC character must be 0")
            else -> Check(true, "IFSC format valid")
        }
    }

    fun iban(value: String): Check {
        val v = value.uppercase().replace(" ", "")
        return when {
            v.isBlank() -> Check(false, "IBAN required for Pakistan")
            !v.startsWith("PK") -> Check(false, "Pakistani IBAN must start with PK")
            v.length != 24 -> Check(false, "PK IBAN must be exactly 24 characters")
            !IBAN_PK.matches(v) -> Check(false, "IBAN shape invalid (PK + 2 digits + 4 letters + 16 digits)")
            else -> if (mod97(v)) Check(true, "IBAN mod-97 checksum valid") else Check(false, "IBAN checksum failed")
        }
    }

    fun aba(value: String): Check {
        val v = value.filter { it.isDigit() }
        return when {
            v.isBlank() -> Check(false, "Routing (ABA) required for ACH")
            v.length != 9 -> Check(false, "ABA must be 9 digits")
            !abaChecksum(v) -> Check(false, "ABA checksum failed — verify routing number", warn = true)
            else -> Check(true, "ABA checksum valid")
        }
    }

    private fun abaChecksum(v: String): Boolean {
        val w = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7, 1)
        var sum = 0
        for (i in 0 until 9) sum += (v[i] - '0') * w[i]
        return sum != 0 && sum % 10 == 0
    }

    private fun mod97(iban: String): Boolean {
        val rearranged = iban.substring(4) + iban.substring(0, 4)
        val sb = StringBuilder()
        for (c in rearranged) {
            if (c.isDigit()) sb.append(c) else sb.append(c - 'A' + 10)
        }
        return runCatching { BigInteger(sb.toString()).mod(BigInteger.valueOf(97)) == BigInteger.ONE }
            .getOrDefault(false)
    }
}
