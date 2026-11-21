package com.example.detection.engine

import java.text.Normalizer

data class PreprocessedText(
    val originalDisplayText: String,
    val wasTruncated: Boolean,
    val normalizedLower: String,
    val matchingText: String,
    val indexMap: IntArray,
    val matchingToNormMap: IntArray,
    val isReadable: Boolean
) {
    fun originalSpan(normStart: Int, normEnd: Int): Pair<Int, Int>? {
        if (normalizedLower.isEmpty() || indexMap.isEmpty()) return null
        val safeStart = normStart.coerceIn(0, indexMap.lastIndex)
        val safeEnd = (normEnd - 1).coerceIn(safeStart, indexMap.lastIndex)
        val origStart = indexMap[safeStart]
        val origEnd = (indexMap[safeEnd] + 1).coerceAtMost(originalDisplayText.length)
        return if (origStart < origEnd) origStart to origEnd else null
    }

    fun originalSpanFromMatching(matchStart: Int, matchEnd: Int): Pair<Int, Int>? {
        if (matchingText.isEmpty() || matchingToNormMap.isEmpty()) return null
        val safeStart = matchStart.coerceIn(0, matchingToNormMap.lastIndex)
        val safeEnd = (matchEnd - 1).coerceIn(safeStart, matchingToNormMap.lastIndex)
        val normStart = matchingToNormMap[safeStart]
        val normEnd = matchingToNormMap[safeEnd] + 1
        return originalSpan(normStart, normEnd)
    }

    override fun equals(other: Any?): Boolean = (other as? PreprocessedText)?.let {
        originalDisplayText == it.originalDisplayText &&
            wasTruncated == it.wasTruncated &&
            normalizedLower == it.normalizedLower
    } ?: false

    override fun hashCode(): Int = originalDisplayText.hashCode()
}

object TextPreprocessor {
    const val MAX_INPUT_CHARS = 5000

    private val zeroWidthChars = setOf('\u200B', '\u200C', '\u200D', '\uFEFF', '\u2060', '\u00AD')

    fun preprocess(rawInput: String): PreprocessedText {
        val wasTruncated = rawInput.length > MAX_INPUT_CHARS
        val display = if (wasTruncated) rawInput.substring(0, MAX_INPUT_CHARS) else rawInput

        val normBuilder = StringBuilder(display.length)
        val mapList = ArrayList<Int>(display.length)
        var lastWasSpace = true

        var i = 0
        while (i < display.length) {
            val ch = display[i]
            if (ch in zeroWidthChars || ch == '\u200E' || ch == '\u200F' || ch == '\u202A' || ch == '\u202E') {
                i++
                continue
            }
            if (ch.isWhitespace()) {
                if (!lastWasSpace) {
                    normBuilder.append(' ')
                    mapList.add(i)
                    lastWasSpace = true
                }
                i++
                continue
            }

            val single = ch.toString()
            val nfkc = Normalizer.normalize(single, Normalizer.Form.NFKC)
            for (nChar in nfkc) {
                val mapped = mapDevanagariDigit(nChar).lowercaseChar()
                if (mapped.isWhitespace()) {
                    if (!lastWasSpace) {
                        normBuilder.append(' ')
                        mapList.add(i)
                        lastWasSpace = true
                    }
                } else {
                    normBuilder.append(mapped)
                    mapList.add(i)
                    lastWasSpace = false
                }
            }
            i++
        }

        while (normBuilder.isNotEmpty() && normBuilder[normBuilder.length - 1] == ' ') {
            normBuilder.deleteCharAt(normBuilder.length - 1)
            mapList.removeAt(mapList.size - 1)
        }

        val normalizedLower = normBuilder.toString()
        val (matchingText, matchingToNormMap) = buildDeobfuscatedCopy(normalizedLower)
        val readable = normalizedLower.any { it.isLetterOrDigit() }

        return PreprocessedText(
            originalDisplayText = display,
            wasTruncated = wasTruncated,
            normalizedLower = normalizedLower,
            matchingText = matchingText,
            indexMap = mapList.toIntArray(),
            matchingToNormMap = matchingToNormMap,
            isReadable = readable
        )
    }

    private fun mapDevanagariDigit(ch: Char): Char = when (ch) {
        '\u0966' -> '0'
        '\u0967' -> '1'
        '\u0968' -> '2'
        '\u0969' -> '3'
        '\u096A' -> '4'
        '\u096B' -> '5'
        '\u096C' -> '6'
        '\u096D' -> '7'
        '\u096E' -> '8'
        '\u096F' -> '9'
        else -> ch
    }

    private fun buildDeobfuscatedCopy(normalized: String): Pair<String, IntArray> {
        if (normalized.isEmpty()) return "" to IntArray(0)
        val out = StringBuilder(normalized.length)
        val map = ArrayList<Int>(normalized.length)

        var idx = 0
        while (idx < normalized.length) {
            val ch = normalized[idx]
            if (ch == ' ') {
                out.append(' ')
                map.add(idx)
                idx++
                continue
            }
            val tokenStart = idx
            while (idx < normalized.length && normalized[idx] != ' ') {
                idx++
            }
            val token = normalized.substring(tokenStart, idx)
            val hasLetter = token.any { it in 'a'..'z' }
            val looksLikeUrlOrEmail = token.contains("http") || token.contains("www.") ||
                (token.contains('@') && token.contains('.'))

            var prevChar = '\u0000'
            var repeatCount = 0
            for (k in token.indices) {
                val origIndex = tokenStart + k
                val rawC = token[k]
                val mappedC = if (hasLetter && !looksLikeUrlOrEmail) {
                    when (rawC) {
                        '0' -> 'o'
                        '@' -> 'a'
                        '$' -> 's'
                        '!' -> 'i'
                        else -> rawC
                    }
                } else {
                    rawC
                }
                if (mappedC == prevChar && mappedC in 'a'..'z') {
                    repeatCount++
                    if (repeatCount >= 2) {
                        // Collapse 3+ repeated ASCII letters (e.g., urrrgent -> urrgent / urgent)
                        continue
                    }
                } else {
                    repeatCount = 0
                }
                prevChar = mappedC
                out.append(mappedC)
                map.add(origIndex)
            }
        }
        return out.toString() to map.toIntArray()
    }
}
