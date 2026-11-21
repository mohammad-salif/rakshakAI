package com.example.detection.url

import com.example.detection.rules.BrandRule
import com.example.domain.models.UrlResult
import java.net.URI

class UrlAnalyzer(
    private val brands: List<BrandRule>,
    private val suspiciousTlds: Set<String>,
    private val shorteners: Set<String>,
    private val suspiciousPathKeywords: Set<String>,
    private val twoLevelSuffixes: Set<String>
) {
    private val brandDetector = BrandImpersonationDetector(brands)
    private val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun analyze(
        rawUrl: String,
        claimedBrands: List<BrandRule> = emptyList()
    ): UrlResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return UrlResult(
                rawUrl = rawUrl,
                normalizedHost = "",
                registeredDomain = "",
                score = 0,
                isUncertain = true,
                flags = listOf("Empty or invalid URL"),
                summary = "Unable to parse URL"
            )
        }

        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }

        val parsed = try {
            URI(withScheme)
        } catch (_: Exception) {
            null
        }

        val scheme = parsed?.scheme?.lowercase() ?: if (trimmed.startsWith("https://", true)) "https" else "http"
        val rawAuthority = parsed?.rawAuthority ?: withScheme.substringAfter("://").substringBefore('/')
        val hasAtSign = rawAuthority.contains('@') || trimmed.contains('@')
        val hostWithPort = rawAuthority.substringAfterLast('@')
        val host = (parsed?.host?.lowercase() ?: hostWithPort.substringBefore(':').lowercase()).trim('.')
        val port = parsed?.port ?: -1
        val pathAndQuery = ((parsed?.rawPath ?: "") + (parsed?.rawQuery?.let { "?$it" } ?: "")).lowercase()

        val hasBrokenBrackets = host.contains('[') != host.contains(']')
        val looksMalformed = parsed == null || host.isEmpty() || hasBrokenBrackets ||
            host.any { it in setOf(' ', '?', '#', '\\', '<', '>') }
        if (looksMalformed) {
            return UrlResult(
                rawUrl = rawUrl,
                normalizedHost = host,
                registeredDomain = "",
                score = 35,
                isUncertain = true,
                flags = listOf("Malformed URL could not be safely parsed (+35)"),
                summary = "Malformed URL — treat with caution"
            )
        }

        val registeredDomain = computeRegisteredDomain(host)
        val tld = host.substringAfterLast('.', "")
        val labels = host.split('.').filter { it.isNotEmpty() }

        var score = 0
        val flags = mutableListOf<String>()

        val isIpHost = ipv4Regex.matches(host) || (host.startsWith('[') && host.endsWith(']'))
        if (isIpHost) {
            score += 40
            flags.add("Uses raw IP address instead of a domain name (+40)")
        }

        val hasNonAscii = host.any { it.code > 127 }
        val isPunycode = labels.any { it.startsWith("xn--") } || hasNonAscii
        if (isPunycode) {
            score += 35
            flags.add("Contains Punycode (xn--) or non-ASCII mixed script host (+35)")
        }

        val isShortener = shorteners.any { s -> host == s || host.endsWith(".$s") }
        if (isShortener) {
            score += 25
            flags.add("URL shortener hides final destination (+25)")
        }

        if (!isIpHost && tld in suspiciousTlds) {
            score += 20
            flags.add("Uses high-risk top-level domain .$tld (+20)")
        }

        if (hasAtSign) {
            score += 30
            flags.add("Contains '@' symbol which can disguise actual host (+30)")
        }

        if (!isIpHost && labels.size >= 4) {
            score += 15
            flags.add("Deep subdomain nesting (${labels.size} host labels) (+15)")
        }

        val brandAnalysis = if (!isIpHost && !isShortener) {
            brandDetector.analyzeHostAgainstBrands(
                host = host,
                pathAndQuery = pathAndQuery,
                registeredDomain = registeredDomain,
                claimedBrands = claimedBrands
            )
        } else {
            BrandAnalysis(claimedBrands = claimedBrands, hostBrandMatches = emptyList())
        }

        if (brandAnalysis.isBrandMismatch) {
            score += 45
            val targetBrand = brandAnalysis.hostBrandMatches.firstOrNull()?.name
                ?: claimedBrands.firstOrNull()?.name
                ?: "a known brand"
            flags.add("Brand mismatch: references $targetBrand while registered domain is $registeredDomain (warning signal, not proof) (+45)")
        }

        if (brandAnalysis.isLookalike) {
            score += 45
            flags.add("${brandAnalysis.lookalikeReason ?: "Lookalike domain pattern"} (+45)")
        }

        val matchedKeywords = suspiciousPathKeywords.filter { kw ->
            host.contains(kw) || pathAndQuery.contains(kw)
        }
        if (matchedKeywords.isNotEmpty()) {
            score += 15
            flags.add("Contains sensitive lure keywords (${matchedKeywords.take(3).joinToString(", ")}) (+15)")
        }

        val hyphenCount = host.count { it == '-' }
        if (hyphenCount >= 3) {
            score += 10
            flags.add("Contains $hyphenCount hyphens in hostname (+10)")
        }

        if (trimmed.length > 75) {
            score += 10
            flags.add("Unusually long URL (${trimmed.length} chars) (+10)")
        }

        if (port != -1 && port != 80 && port != 443) {
            score += 10
            flags.add("Uses non-standard port $port (+10)")
        }

        if (pathAndQuery.contains(".apk")) {
            score += 55
            flags.add("Direct Android .apk app download link (+55)")
        }

        // Plain http:// penalty (+10); https gives zero safety credit
        if (scheme == "http") {
            score += 10
            flags.add("Unencrypted http:// link (+10); note HTTPS alone never guarantees safety")
        }

        val isOfficial = brandAnalysis.matchedOfficialBrand != null
        if (isOfficial) {
            score = score.coerceAtMost(10)
            flags.clear()
            flags.add("Matches listed official domain for ${brandAnalysis.matchedOfficialBrand!!.name} (still verify context)")
        } else {
            score = score.coerceIn(0, 100)
        }

        val isUncertain = isShortener || (!isOfficial && score in 20..59 && flags.size <= 2)
        val matchedBrand = brandAnalysis.matchedOfficialBrand ?: brandAnalysis.hostBrandMatches.firstOrNull()

        val summary = when {
            isOfficial -> "Lower risk — matches known official domain (${brandAnalysis.matchedOfficialBrand!!.name}), not a guarantee"
            isShortener -> "Uncertain — shortened link destination not verified; treat with caution"
            score >= 60 -> "High-risk link patterns detected (${flags.size} flags)"
            score >= 30 -> "Suspicious link patterns found — verify before opening"
            else -> "No strong URL red flags found — HTTPS is not proof of safety"
        }

        return UrlResult(
            rawUrl = rawUrl,
            normalizedHost = host,
            registeredDomain = registeredDomain,
            score = score,
            isShortener = isShortener,
            isOfficialMatch = isOfficial,
            matchedBrandId = matchedBrand?.id,
            matchedBrandName = matchedBrand?.name,
            isBrandMismatch = brandAnalysis.isBrandMismatch,
            isUncertain = isUncertain,
            flags = flags,
            summary = summary
        )
    }

    fun computeRegisteredDomain(host: String): String {
        val clean = host.lowercase().trim('.')
        if (ipv4Regex.matches(clean) || clean.startsWith('[')) return clean
        val parts = clean.split('.').filter { it.isNotEmpty() }
        if (parts.size <= 2) return clean
        val lastTwo = "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        return if (lastTwo in twoLevelSuffixes && parts.size >= 3) {
            "${parts[parts.size - 3]}.$lastTwo"
        } else {
            lastTwo
        }
    }
}
