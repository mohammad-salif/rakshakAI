package com.example.detection.rules

import com.example.detection.engine.PreprocessedText
import kotlin.math.abs
import kotlin.math.min

data class PhraseHit(
    val group: String,
    val phrase: String,
    val matchedText: String,
    val start: Int?,
    val end: Int?,
    val weight: Int,
    val fuzzyMatched: Boolean = false
)

object PhraseMatcher {

    fun matchGroups(
        preprocessed: PreprocessedText,
        lexicons: List<LexiconFile>,
        groupNames: Set<String>,
        enableNegationFilterForOtp: Boolean = false
    ): List<PhraseHit> {
        val textNorm = preprocessed.normalizedLower
        val textMatch = preprocessed.matchingText
        if (textNorm.isBlank()) return emptyList()

        val results = mutableListOf<PhraseHit>()
        val seenKeys = HashSet<String>()

        // Pass 1: exact boundary-aware matching on normalizedLower and deobfuscated matchingText
        for (lexicon in lexicons) {
            for (groupName in groupNames) {
                val entries = lexicon.groups[groupName].orEmpty()
                for (entry in entries) {
                    val needle = entry.phrase.lowercase().trim()
                    if (needle.isEmpty()) continue

                    val dedupeKey = "$groupName|$needle"
                    if (dedupeKey in seenKeys) continue

                    val normIdx = findBoundaryIndex(textNorm, needle)
                    if (normIdx >= 0) {
                        if (enableNegationFilterForOtp && groupName == "intent_otp" && isOtpNegated(textNorm, normIdx)) {
                            continue
                        }
                        val span = preprocessed.originalSpan(normIdx, normIdx + needle.length)
                        val snippet = span?.let {
                            preprocessed.originalDisplayText.substring(it.first, it.second)
                        } ?: entry.phrase
                        results.add(
                            PhraseHit(
                                group = groupName,
                                phrase = entry.phrase,
                                matchedText = snippet,
                                start = span?.first,
                                end = span?.second,
                                weight = entry.weight,
                                fuzzyMatched = false
                            )
                        )
                        seenKeys.add(dedupeKey)
                        continue
                    }

                    val matchIdx = findBoundaryIndex(textMatch, needle)
                    if (matchIdx >= 0) {
                        if (enableNegationFilterForOtp && groupName == "intent_otp" && isOtpNegated(textMatch, matchIdx)) {
                            continue
                        }
                        val span = preprocessed.originalSpanFromMatching(matchIdx, matchIdx + needle.length)
                        val snippet = span?.let {
                            preprocessed.originalDisplayText.substring(it.first, it.second)
                        } ?: entry.phrase
                        results.add(
                            PhraseHit(
                                group = groupName,
                                phrase = entry.phrase,
                                matchedText = snippet,
                                start = span?.first,
                                end = span?.second,
                                weight = entry.weight,
                                fuzzyMatched = false
                            )
                        )
                        seenKeys.add(dedupeKey)
                    }
                }
            }
        }

        // Pass 2: lightweight typo / misspelling tolerance on ASCII tokens >= 6 chars
        val wordRegex = Regex("""[a-z]{6,}""")
        val words = wordRegex.findAll(textMatch).toList()
        if (words.isNotEmpty()) {
            for (lexicon in lexicons) {
                for (groupName in groupNames) {
                    val entries = lexicon.groups[groupName].orEmpty()
                    for (entry in entries) {
                        val needle = entry.phrase.lowercase().trim()
                        if (needle.length < 6 || !needle.all { it in 'a'..'z' }) continue
                        val dedupeKey = "$groupName|$needle"
                        if (dedupeKey in seenKeys) continue

                        for (wordMatch in words) {
                            val token = wordMatch.value
                            if (abs(token.length - needle.length) > 1) continue
                            if (damerauLevenshtein(token, needle) == 1) {
                                val span = preprocessed.originalSpanFromMatching(
                                    wordMatch.range.first,
                                    wordMatch.range.last + 1
                                )
                                val snippet = span?.let {
                                    preprocessed.originalDisplayText.substring(it.first, it.second)
                                } ?: token
                                results.add(
                                    PhraseHit(
                                        group = groupName,
                                        phrase = entry.phrase,
                                        matchedText = snippet,
                                        start = span?.first,
                                        end = span?.second,
                                        weight = (entry.weight - 1).coerceAtLeast(1),
                                        fuzzyMatched = true
                                    )
                                )
                                seenKeys.add(dedupeKey)
                                break
                            }
                        }
                    }
                }
            }
        }

        return results.sortedByDescending { it.weight }
    }

    fun findBoundaryIndex(haystack: String, needle: String): Int {
        var searchFrom = 0
        while (searchFrom <= haystack.length - needle.length) {
            val idx = haystack.indexOf(needle, startIndex = searchFrom)
            if (idx < 0) return -1
            val beforeOk = if (idx == 0) {
                true
            } else {
                val prev = haystack[idx - 1]
                val first = needle.first()
                !isSameScriptWordChar(prev, first)
            }
            val endIdx = idx + needle.length
            val afterOk = if (endIdx >= haystack.length) {
                true
            } else {
                val next = haystack[endIdx]
                val last = needle.last()
                !isSameScriptWordChar(next, last)
            }
            if (beforeOk && afterOk) return idx
            searchFrom = idx + 1
        }
        return -1
    }

    private fun isSameScriptWordChar(adjacent: Char, edge: Char): Boolean {
        if (adjacent == '\u0964' || adjacent == '\u0965' || adjacent == '\u0970') return false
        return if (edge in 'a'..'z' || edge in '0'..'9') {
            adjacent in 'a'..'z' || adjacent in '0'..'9'
        } else if (edge in '\u0900'..'\u097F') {
            adjacent in '\u0900'..'\u0963' || adjacent in '\u0966'..'\u097F'
        } else {
            false
        }
    }

    private fun isOtpNegated(text: String, idx: Int): Boolean {
        val windowStart = (idx - 45).coerceAtLeast(0)
        val windowEnd = (idx + 55).coerceAtMost(text.length)
        val window = text.substring(windowStart, windowEnd)
        val negations = listOf(
            "do not share", "don't share", "dont share", "never share",
            "never asks", "bank never asks", "do not disclose",
            "साझा न करें", "किसी को न बताएं", "नहीं मांगता", "मत बताएं",
            "mat batana", "share mat", "nahi mangta"
        )
        return negations.any { window.contains(it) }
    }

    private fun damerauLevenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val d = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) d[i][0] = i
        for (j in 0..n) d[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, d[i - 2][j - 2] + cost)
                }
                d[i][j] = value
            }
        }
        return d[m][n]
    }
}
