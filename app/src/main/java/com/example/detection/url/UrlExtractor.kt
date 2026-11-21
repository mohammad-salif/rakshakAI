package com.example.detection.url

data class ExtractedEntities(
    val urls: List<String>,
    val upiUris: List<String>,
    val upiIds: List<String>,
    val phoneNumbers: List<String>,
    val amounts: List<String>
)

object UrlExtractor {
    private val upiUriRegex = Regex("""upi://pay[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val explicitUrlRegex = Regex("""https?://[^\s"'<>()]+""", RegexOption.IGNORE_CASE)
    private val wwwUrlRegex = Regex("""\bwww\.[a-z0-9.-]+\.[a-z]{2,}[^\s"'<>()]*""", RegexOption.IGNORE_CASE)
    private val bareDomainRegex = Regex("""\b([a-z0-9-]+(?:\.[a-z0-9-]+)+)(/[^\s"'<>()]*)?""", RegexOption.IGNORE_CASE)
    private val upiIdRegex = Regex("""\b[a-zA-Z0-9._-]{2,}@[a-zA-Z0-9]{2,}\b""")
    private val phoneRegex = Regex("""(?<!\d)(?:\+91[\s-]?|0)?[6-9]\d{9}(?!\d)""")
    private val amountRegex = Regex("""(?:₹|rs\.?|inr)\s*\d[\d,]*(?:\.\d{1,2})?(?:\s*(?:lakh|lac|crore|k))?""", RegexOption.IGNORE_CASE)

    fun extract(
        text: String,
        suspiciousTlds: Set<String> = emptySet(),
        shorteners: Set<String> = emptySet(),
        officialDomains: Set<String> = emptySet()
    ): ExtractedEntities {
        val urls = LinkedHashSet<String>()
        val upiUris = LinkedHashSet<String>()

        upiUriRegex.findAll(text).forEach { match ->
            upiUris.add(trimTrailingPunctuation(match.value))
        }

        explicitUrlRegex.findAll(text).forEach { match ->
            urls.add(trimTrailingPunctuation(match.value))
        }

        wwwUrlRegex.findAll(text).forEach { match ->
            val candidate = trimTrailingPunctuation(match.value)
            if (urls.none { it.contains(candidate, ignoreCase = true) }) {
                urls.add("http://$candidate")
            }
        }

        bareDomainRegex.findAll(text).forEach { match ->
            val startIdx = match.range.first
            val precededByAt = startIdx > 0 && text[startIdx - 1] == '@'
            if (precededByAt) return@forEach

            val host = match.groupValues[1].lowercase()
            val tld = host.substringAfterLast('.', "")
            val isKnownUrlDomain = tld in suspiciousTlds ||
                host in shorteners ||
                officialDomains.any { host == it || host.endsWith(".$it") } ||
                tld in setOf("com", "in", "org", "net", "gov", "sbi", "apk")

            // Avoid matching version numbers like "v2.5" or currency like "1.240"
            val hasLetters = host.any { it in 'a'..'z' }
            if (hasLetters && isKnownUrlDomain) {
                val full = trimTrailingPunctuation(match.value)
                if (urls.none { it.contains(full, ignoreCase = true) }) {
                    urls.add("http://$full")
                }
            }
        }

        val upiIds = LinkedHashSet<String>()
        upiIdRegex.findAll(text).forEach { match ->
            val value = match.value
            val domain = value.substringAfter('@').lowercase()
            // Skip standard public email providers unless clearly a UPI handle
            if (!domain.contains('.')) {
                upiIds.add(value)
            }
        }

        val phones = phoneRegex.findAll(text).map { it.value.trim() }.distinct().toList()
        val amounts = amountRegex.findAll(text).map { it.value.trim() }.distinct().toList()

        return ExtractedEntities(
            urls = urls.toList(),
            upiUris = upiUris.toList(),
            upiIds = upiIds.toList(),
            phoneNumbers = phones,
            amounts = amounts
        )
    }

    private fun trimTrailingPunctuation(raw: String): String {
        return raw.trimEnd('.', ',', ';', ':', ')', ']', '}', '!', '?', '"', '\'')
    }
}
