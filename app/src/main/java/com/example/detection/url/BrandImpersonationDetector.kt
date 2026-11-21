package com.example.detection.url

import com.example.detection.rules.BrandRule
import kotlin.math.min

data class BrandAnalysis(
    val claimedBrands: List<BrandRule>,
    val hostBrandMatches: List<BrandRule>,
    val matchedOfficialBrand: BrandRule? = null,
    val isBrandMismatch: Boolean = false,
    val isLookalike: Boolean = false,
    val lookalikeReason: String? = null
)

class BrandImpersonationDetector(
    private val brands: List<BrandRule>
) {
    fun detectClaimedBrands(messageTextLower: String): List<BrandRule> {
        if (messageTextLower.isBlank()) return emptyList()
        return brands.filter { brand ->
            brand.aliases.any { alias ->
                val aliasLower = alias.lowercase()
                if (aliasLower.all { it in 'a'..'z' || it == ' ' || it == '-' }) {
                    Regex("""\b${Regex.escape(aliasLower)}\b""").containsMatchIn(messageTextLower)
                } else {
                    messageTextLower.contains(aliasLower)
                }
            }
        }
    }

    fun analyzeHostAgainstBrands(
        host: String,
        pathAndQuery: String,
        registeredDomain: String,
        claimedBrands: List<BrandRule>
    ): BrandAnalysis {
        val hostLower = host.lowercase()
        val regLower = registeredDomain.lowercase()
        val fullTarget = "$hostLower$pathAndQuery".lowercase()

        // Check exact official match
        val exactOfficialBrand = brands.firstOrNull { brand ->
            brand.officialDomains.any { official ->
                regLower == official.lowercase()
            }
        }

        if (exactOfficialBrand != null) {
            return BrandAnalysis(
                claimedBrands = claimedBrands,
                hostBrandMatches = listOf(exactOfficialBrand),
                matchedOfficialBrand = exactOfficialBrand,
                isBrandMismatch = false,
                isLookalike = false
            )
        }

        // Check if brand name or official domain appears in subdomain/path while registered domain is different
        val embeddedBrands = brands.filter { brand ->
            brand.officialDomains.any { official ->
                fullTarget.contains(official.lowercase())
            } || brand.aliases.any { alias ->
                val cleaned = alias.lowercase().replace(" ", "")
                cleaned.length >= 3 && cleaned.all { it in 'a'..'z' } && fullTarget.contains(cleaned)
            }
        }

        val mismatchWithClaimed = claimedBrands.isNotEmpty() && claimedBrands.none { brand ->
            brand.officialDomains.any { regLower == it.lowercase() }
        }

        val isMismatch = embeddedBrands.isNotEmpty() || mismatchWithClaimed

        // Lookalike check on registered domain label (e.g., sbi-kyc-update, hdfcbank -> hdfcbanlk, 0/o, 1/l/i, rn/m, vv/w)
        val regBase = normalizeLookalike(regLower.substringBefore('.'))
        var lookalikeBrand: BrandRule? = null
        var lookalikeReason: String? = null

        for (brand in brands) {
            for (official in brand.officialDomains) {
                val officialLower = official.lowercase()
                val officialBase = normalizeLookalike(officialLower.substringBefore('.'))
                val rawRegBase = regLower.substringBefore('.')
                val rawOfficialBase = officialLower.substringBefore('.')

                // Hyphen-padded or prefix/suffix padded brand domain (e.g. sbi-kyc-update.xyz)
                val hyphenParts = rawRegBase.split('-', '_')
                val padded = hyphenParts.size >= 2 && hyphenParts.any { part ->
                    part == rawOfficialBase || brand.aliases.any {
                        val cleanAlias = it.lowercase().replace(" ", "")
                        cleanAlias.length >= 3 && part == cleanAlias
                    }
                }
                if (padded) {
                    lookalikeBrand = brand
                    lookalikeReason = "Hyphen-padded domain impersonating ${brand.name} ($officialLower)"
                    break
                }

                // Homoglyph substitution exact match after normalization (0->o, 1->l, rn->m, vv->w)
                if (regBase == officialBase && rawRegBase != rawOfficialBase) {
                    lookalikeBrand = brand
                    lookalikeReason = "Character substitution lookalike of ${brand.name} ($officialLower)"
                    break
                }

                // Levenshtein distance <= 2 for brands with base length >= 4
                if (officialBase.length >= 4 && regBase != officialBase) {
                    val dist = levenshtein(regBase, officialBase)
                    if (dist in 1..2) {
                        lookalikeBrand = brand
                        lookalikeReason = "Typosquatting lookalike (edit distance $dist) of ${brand.name} ($officialLower)"
                        break
                    }
                }
            }
            if (lookalikeBrand != null) break
        }

        val matchedBrands = (embeddedBrands + listOfNotNull(lookalikeBrand)).distinctBy { it.id }
        return BrandAnalysis(
            claimedBrands = claimedBrands,
            hostBrandMatches = matchedBrands,
            matchedOfficialBrand = null,
            isBrandMismatch = isMismatch,
            isLookalike = lookalikeBrand != null,
            lookalikeReason = lookalikeReason
        )
    }

    private fun normalizeLookalike(input: String): String {
        return input.lowercase()
            .replace('0', 'o')
            .replace('1', 'l')
            .replace("rn", "m")
            .replace("vv", "w")
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}
