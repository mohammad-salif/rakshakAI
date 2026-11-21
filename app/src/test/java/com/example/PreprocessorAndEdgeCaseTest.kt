package com.example

import com.example.detection.engine.LanguageDetector
import com.example.detection.engine.RuleBasedScamEngine
import com.example.detection.engine.TextPreprocessor
import com.example.detection.rules.RuleSource
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScanInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreprocessorAndEdgeCaseTest {

    private val engine = RuleBasedScamEngine(FileSystemRuleSource())
    private val markers = engine.getRuleBundleOrNull()!!.lexiconHinglish.hinglishMarkers

    @Test fun `strips zero width characters before matching`() {
        val pre = TextPreprocessor.preprocess("Sh\u200Bare y\u200Cour O\u200BTP")
        assertEquals("share your otp", pre.normalizedLower)
    }

    @Test fun `converts devanagari digits to ascii digits`() {
        val pre = TextPreprocessor.preprocess("₹५,००० जमा करें")
        assertTrue(pre.normalizedLower.contains("5,000"))
    }

    @Test fun `deobfuscates leet tokens while keeping valid numbers`() {
        val pre = TextPreprocessor.preprocess("123456 is OTP. l0gin on sbi p@ge urrrgent")
        assertTrue(pre.matchingText.contains("123456"))
        assertTrue(pre.matchingText.contains("login"))
        assertTrue(pre.matchingText.contains("page"))
        assertTrue(pre.matchingText.contains("urrgent") || pre.matchingText.contains("urgent"))
    }

    @Test fun `truncates input exceeding 5000 chars with flag`() {
        val huge = "a ".repeat(3000)
        val res = engine.analyze(ScanInput(huge))
        assertTrue(res.wasTruncated)
        assertEquals(TextPreprocessor.MAX_INPUT_CHARS, res.sourceText.length)
    }

    @Test fun `handles empty input safely without crashing`() {
        val res = engine.analyze(ScanInput(""))
        assertEquals(RiskLevel.LOW, res.riskLevel)
        assertTrue(res.uncertain)
    }

    @Test fun `handles emoji only unreadable input safely`() {
        val res = engine.analyze(ScanInput("🔒⚠️💰💳"))
        assertEquals(RiskLevel.LOW, res.riskLevel)
        assertTrue(res.uncertain)
    }

    @Test fun `handles RTL and control chars safely`() {
        val res = engine.analyze(ScanInput("\u202Ehello\u0007\u200F test"))
        assertEquals(RiskLevel.LOW, res.riskLevel)
    }

    @Test fun `detects english language`() {
        val lang = LanguageDetector.detect("please verify your order delivery status", markers)
        assertEquals("en", lang.code)
        assertFalse(lang.unsupportedScriptWarning)
    }

    @Test fun `detects hindi devanagari language`() {
        val lang = LanguageDetector.detect("आपका बैंक खाता बंद हो जाएगा तुरंत अपडेट करें", markers)
        assertEquals("hi", lang.code)
    }

    @Test fun `detects hinglish language`() {
        val lang = LanguageDetector.detect("aapka khata aaj raat band ho jayega turant click karein", markers)
        assertEquals("hinglish", lang.code)
    }

    @Test fun `detects mixed hindi english language`() {
        val lang = LanguageDetector.detect("आपका bank account आज रात block हो जाएगा update करें", markers)
        assertEquals("mixed", lang.code)
    }

    @Test fun `flags unsupported non latin non devanagari script`() {
        val res = engine.analyze(ScanInput("Привет проверьте ваш банковский счет срочно"))
        assertTrue(res.unsupportedLanguageWarning)
        assertTrue(res.uncertain)
    }

    @Test fun `manual language override respects hindi`() {
        val res = engine.analyze(ScanInput("Share your OTP to verify your account.", languageHint = "hi"))
        assertTrue(res.language.contains("Hindi"))
    }

    @Test fun `missing or corrupt rule files return detectorUnavailable never fake safe`() {
        val brokenSource = object : RuleSource {
            override fun readRuleText(fileName: String): String = "{ invalid json"
        }
        val brokenEngine = RuleBasedScamEngine(brokenSource)
        assertFalse(brokenEngine.isReady())
        val res = brokenEngine.analyze(ScanInput("Dear customer, share your OTP immediately"))
        assertFalse(res.detectorAvailable)
        assertTrue(res.detectorError?.contains("Detector unavailable", ignoreCase = true) == true)
    }
}
