package com.example.detection.upi

import com.example.domain.models.UpiAnalysis
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object UpiQrAnalyzer {

    private val trapKeywords = listOf(
        "refund", "prize", "lottery", "win", "winner", "claim", "receive",
        "cashback", "reward", "kyc", "verify", "release", "customs", "kbc",
        "रिफंड", "इनाम", "लॉटरी"
    )

    private val officialPayeeHandleHints = listOf(
        "helpdesk", "refund", "support", "customercare", "care", "lottery", "prize", "verify", "kyc"
    )

    fun analyze(rawPayload: String, claimedBrandName: String? = null): UpiAnalysis {
        val queryPart = rawPayload.substringAfter('?', "")
        val params = parseQueryParams(queryPart)

        val pa = params["pa"].orEmpty()
        val pn = params["pn"].orEmpty()
        val am = params["am"].orEmpty()
        val tn = params["tn"].orEmpty()
        val cu = params["cu"].orEmpty().ifBlank { "INR" }

        val warnings = mutableListOf<String>()
        var score = 20 // Base caution for any scanned payment QR

        // Core Indian safety rule
        warnings.add("You NEVER need to scan a QR code or enter your UPI PIN to RECEIVE money. Scanning a payment QR always sends money out.")

        val combinedText = "$pa $pn $tn".lowercase()
        if (trapKeywords.any { combinedText.contains(it) }) {
            score += 45
            warnings.add("Payee name or note uses a 'refund/prize/receive' lure — classic QR collect trap.")
        }

        if (am.isNotBlank()) {
            score += 20
            warnings.add("Pre-filled payment amount (₹$am) is locked inside this QR code.")
        }

        if (officialPayeeHandleHints.any { pa.lowercase().contains(it) }) {
            score += 20
            warnings.add("Payee VPA ($pa) uses generic 'helpdesk/refund/support' wording often seen in impersonation scams.")
        }

        if (!claimedBrandName.isNullOrBlank()) {
            val brandKeyword = claimedBrandName.lowercase().substringBefore(' ')
            if (brandKeyword.length >= 3 &&
                !pn.lowercase().contains(brandKeyword) &&
                !pa.lowercase().contains(brandKeyword)
            ) {
                score += 25
                warnings.add("Payee details ('$pn' / $pa) do not match claimed brand '$claimedBrandName'.")
            }
        }

        if (pa.isBlank()) {
            score += 15
            warnings.add("Missing payee VPA ('pa') parameter — malformed UPI QR.")
        }

        return UpiAnalysis(
            rawUri = rawPayload,
            payeeAddress = pa,
            payeeName = pn,
            amount = am,
            note = tn,
            currency = cu,
            riskScore = score.coerceIn(0, 100),
            warnings = warnings
        )
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (pair in query.split('&')) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = decode(pair.substring(0, idx)).lowercase()
                val value = decode(pair.substring(idx + 1))
                map[key] = value
            }
        }
        return map
    }

    private fun decode(raw: String): String {
        return try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            raw
        }
    }
}
