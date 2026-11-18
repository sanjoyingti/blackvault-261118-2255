package com.aibuilt.blackvault.model

enum class Rail(val label: String, val currency: String) {
    INDIA_IMPS("IMPS / NEFT", "INR"),
    US_ACH("ACH", "USD"),
    PAKISTAN_IBFT("IBFT / Raast", "PKR")
}

data class BankTarget(
    val name: String,
    val host: String,
    val rail: Rail,
    val ifscPrefixes: List<String> = emptyList(),
    val ibanCodes: List<String> = emptyList(),
    val routingNumbers: List<String> = emptyList()
)

data class Detection(val rail: Rail, val bankName: String, val host: String?)

object TargetDatabase {

    val us: List<BankTarget> = listOf(
        BankTarget("JPMORGAN CHASE", "chase.com", Rail.US_ACH, routingNumbers = listOf("021000021")),
        BankTarget("BANK OF AMERICA", "bankofamerica.com", Rail.US_ACH, routingNumbers = listOf("026009593")),
        BankTarget("WELLS FARGO", "wellsfargo.com", Rail.US_ACH, routingNumbers = listOf("121000248")),
        BankTarget("CITI", "citi.com", Rail.US_ACH, routingNumbers = listOf("031100089")),
        BankTarget("US BANK", "usbank.com", Rail.US_ACH, routingNumbers = listOf("091000022")),
        BankTarget("PNC", "pnc.com", Rail.US_ACH, routingNumbers = listOf("043000096")),
        BankTarget("CAPITAL ONE", "capitalone.com", Rail.US_ACH, routingNumbers = listOf("031176110")),
        BankTarget("TD BANK", "tdbank.com", Rail.US_ACH, routingNumbers = listOf("031101266")),
        BankTarget("TRUIST", "truist.com", Rail.US_ACH, routingNumbers = listOf("053101121")),
        BankTarget("ALLY BANK", "ally.com", Rail.US_ACH, routingNumbers = listOf("124003116")),
        BankTarget("HSBC USA", "us.hsbc.com", Rail.US_ACH, routingNumbers = listOf("021001088")),
        BankTarget("CHARLES SCHWAB", "schwab.com", Rail.US_ACH, routingNumbers = listOf("121202211")),
        BankTarget("AMERICAN EXPRESS", "americanexpress.com", Rail.US_ACH, routingNumbers = listOf("124085136"))
    )

    val pakistan: List<BankTarget> = listOf(
        BankTarget("HABIB BANK LIMITED", "hbl.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("HABB")),
        BankTarget("UNITED BANK LIMITED", "ubldirect.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("UNIL")),
        BankTarget("MCB BANK", "mcb.com.pk", Rail.PAKISTAN_IBFT, ibanCodes = listOf("MUCB")),
        BankTarget("MEEZAN BANK", "meezanbank.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("MEZN")),
        BankTarget("ALLIED BANK", "ablp.com.pk", Rail.PAKISTAN_IBFT, ibanCodes = listOf("ALLI")),
        BankTarget("BANK ALFALAH", "bankalfalah.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("ALFH")),
        BankTarget("STANDARD CHARTERED PK", "sc.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("SCBL")),
        BankTarget("FAYSAL BANK", "faysalbank.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("FAYS")),
        BankTarget("ASKARI BANK", "askaribank.com.pk", Rail.PAKISTAN_IBFT, ibanCodes = listOf("ASKC", "ASCM")),
        BankTarget("JS BANK", "jsbl.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("JSBL")),
        BankTarget("BANK AL HABIB", "bankalhabib.com", Rail.PAKISTAN_IBFT, ibanCodes = listOf("BAHL"))
    )

    val india: List<BankTarget> = listOf(
        BankTarget("STATE BANK OF INDIA", "sbi.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("SBIN")),
        BankTarget("HDFC BANK", "hdfcbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("HDFC")),
        BankTarget("ICICI BANK", "icicibank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("ICIC")),
        BankTarget("AXIS BANK", "axisbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("UTIB", "AXIS")),
        BankTarget("KOTAK MAHINDRA BANK", "kotak.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("KKBK")),
        BankTarget("PUNJAB NATIONAL BANK", "pnbindia.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("PUNB", "PNB")),
        BankTarget("BANK OF BARODA", "bankofbaroda.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("BARB")),
        BankTarget("CANARA BANK", "canarabank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("CNRB")),
        BankTarget("INDIAN BANK", "indianbank.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("IDIB")),
        BankTarget("BANK OF INDIA", "bankofindia.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("BKID")),
        BankTarget("INDIAN OVERSEAS BANK", "iob.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("IOBA")),
        BankTarget("UCO BANK", "uco.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("UCBA")),
        BankTarget("YES BANK", "yesbank.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("YESB")),
        BankTarget("IDFC FIRST BANK", "idfcfirstbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("IDFB")),
        BankTarget("CENTRAL BANK OF INDIA", "centralbankofindia.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("CBIN")),
        BankTarget("BANK OF MAHARASHTRA", "bankofmaharashtra.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("MAHB")),
        BankTarget("PUNJAB & SIND BANK", "psbindia.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("PSIB")),
        BankTarget("SOUTH INDIAN BANK", "southindianbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("SIBL")),
        BankTarget("FEDERAL BANK", "federalbank.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("FDRL")),
        BankTarget("KARNATAKA BANK", "karnatakabank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("KARB")),
        BankTarget("ANDHRA BANK", "andhrabank.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("ANDB")),
        BankTarget("BANDHAN BANK", "bandhanbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("BDBL")),
        BankTarget("DCB BANK", "dcbbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("DCBL")),
        BankTarget("RATNAKAR BANK", "ratnakarbank.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("RATN")),
        BankTarget("JAMMU & KASHMIR BANK", "jkbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("JAKA")),
        BankTarget("INDUSIND BANK", "indusind.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("INDB")),
        BankTarget("CSB BANK", "csb.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("CSBK")),
        BankTarget("KARUR VYSYA BANK", "kvb.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("KVBL")),
        BankTarget("CITY UNION BANK", "cityunionbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("CIUB")),
        BankTarget("TAMILNAD MERCANTILE BANK", "tmb.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("TMBL")),
        BankTarget("NAINITAL BANK", "nainitalbank.co.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("NTBL")),
        BankTarget("DHANLAXMI BANK", "dhanbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("DLXB")),
        BankTarget("JK LAKSHMI CEMENT FINANCE", "jklakshmi.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("JSFB")),
        BankTarget("AU SMALL FINANCE BANK", "aubank.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("AUBL")),
        BankTarget("UJJIVAN SMALL FINANCE BANK", "ujjivansfb.in", Rail.INDIA_IMPS, ifscPrefixes = listOf("UJVN")),
        BankTarget("EQUITAS SMALL FINANCE BANK", "equitasbank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("ESFB")),
        BankTarget("SURYODAY SMALL FINANCE BANK", "suryodaybank.com", Rail.INDIA_IMPS, ifscPrefixes = listOf("SURY"))
    )

    val all: List<BankTarget> = us + pakistan + india

    fun byIfsc(ifsc: String): BankTarget? {
        if (ifsc.length < 4) return null
        val prefix = ifsc.take(4).uppercase()
        return india.firstOrNull { it.ifscPrefixes.any { p -> p == prefix } }
    }

    fun byIban(iban: String): BankTarget? {
        val clean = iban.uppercase().filter { !it.isWhitespace() }
        if (clean.length < 8) return null
        val code = clean.substring(4, 8)
        return pakistan.firstOrNull { it.ibanCodes.contains(code) }
    }

    fun byRouting(aba: String): BankTarget? {
        val clean = aba.filter { it.isDigit() }
        return us.firstOrNull { it.routingNumbers.contains(clean) }
    }

    /** Works out the rail and the bank purely from what the user typed. */
    fun detect(account: String, ifsc: String, iban: String, aba: String, custom: List<BankTarget>): Detection? {
        val cleanIban = iban.uppercase().filter { !it.isWhitespace() }
        if (cleanIban.isNotEmpty()) {
            if (!cleanIban.startsWith("PK")) return null
            val known = byIban(cleanIban) ?: custom.firstOrNull { it.rail == Rail.PAKISTAN_IBFT && it.ibanCodes.any { c -> cleanIban.contains(c) } }
            return Detection(Rail.PAKISTAN_IBFT, known?.name ?: "UNIDENTIFIED PK INSTITUTION", known?.host)
        }
        if (ifsc.isNotBlank()) {
            val known = byIfsc(ifsc) ?: custom.firstOrNull { it.rail == Rail.INDIA_IMPS && it.ifscPrefixes.any { p -> ifsc.uppercase().startsWith(p) } }
            return Detection(Rail.INDIA_IMPS, known?.name ?: "UNIDENTIFIED IN INSTITUTION", known?.host)
        }
        val digits = account.filter { it.isDigit() }
        if (digits.length == account.trim().length && digits.length in 8..17 && aba.isNotBlank()) {
            val known = byRouting(aba) ?: custom.firstOrNull { it.rail == Rail.US_ACH && it.routingNumbers.contains(aba.filter { c -> c.isDigit() }) }
            return Detection(Rail.US_ACH, known?.name ?: "UNIDENTIFIED US INSTITUTION", known?.host)
        }
        return null
    }
}
