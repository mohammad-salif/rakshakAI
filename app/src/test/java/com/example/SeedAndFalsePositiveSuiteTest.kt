package com.example

import com.example.detection.DemoSamples
import com.example.detection.engine.RuleBasedScamEngine
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScanInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedAndFalsePositiveSuiteTest {

    private val engine = RuleBasedScamEngine(FileSystemRuleSource())

    // --- All 15 §10 Benchmark Samples ---

    @Test fun `sample 01 fake SBI KYC lookalike is Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[0].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.CRITICAL, res.riskLevel)
    }

    @Test fun `sample 02 Hinglish KYC shortened URL is High or Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[1].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 03 Hindi Devanagari KYC OTP is High or Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[2].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 04 OTP theft without link is High`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[3].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 05 KBC lottery upfront fee is High or Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[4].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 06 Part-time job registration fee is High or Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[5].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 07 Investment 5x returns is Medium or High`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[6].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL))
        assertTrue(res.riskScore >= 50)
    }

    @Test fun `sample 08 Unseen electricity disconnection paraphrase is High`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[7].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
        assertEquals("utility_disconnection", res.scamCategoryId)
    }

    @Test fun `sample 09 AnyDesk 9 digit code is Critical`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[8].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.CRITICAL, res.riskLevel)
    }

    @Test fun `sample 10 Parcel fee with top TLD is High`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[9].payload))
        assertTrue("Level was ${res.riskLevel} (${res.riskScore})", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `sample 11 Legitimate HDFC OTP warning stays Low`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[10].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.LOW, res.riskLevel)
    }

    @Test fun `sample 12 Legitimate Amazon shipped update stays Low`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[11].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.LOW, res.riskLevel)
    }

    @Test fun `sample 13 Legitimate electricity reminder stays Low`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[12].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.LOW, res.riskLevel)
    }

    @Test fun `sample 14 Legitimate Hindi electricity reminder stays Low`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[13].payload))
        assertEquals("Score was ${res.riskScore}", RiskLevel.LOW, res.riskLevel)
    }

    @Test fun `sample 15 Short greeting Hi is Low and uncertain`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[14].payload))
        assertEquals(RiskLevel.LOW, res.riskLevel)
        assertTrue(res.uncertain)
    }

    // --- Extra Misspellings, Paraphrases, Hinglish & Hindi Scams ---

    @Test fun `detects misspelling paraphrase in urgent account block`() {
        val res = engine.analyze(ScanInput("Your bank acount will be blokced immediatly. Share your OTP to verify."))
        assertTrue("Score was ${res.riskScore}", res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `detects Hinglish digital arrest threat`() {
        val res = engine.analyze(ScanInput("CBI se bol raha hoon. Aap par case darj hai. Kisi ko mat batana warna giraftar ho jaoge. Safe account mein paise bhejo."))
        assertEquals(RiskLevel.CRITICAL, res.riskLevel)
    }

    @Test fun `detects Hinglish remote access QuickSupport trap`() {
        val res = engine.analyze(ScanInput("Refund ke liye TeamViewer QuickSupport install karo aur 9 digit code batao."))
        assertEquals(RiskLevel.CRITICAL, res.riskLevel)
    }

    @Test fun `detects Hindi lottery fee scam`() {
        val res = engine.analyze(ScanInput("बधाई हो! आपने लॉटरी में इनाम जीता है। अभी ₹2000 प्रोसेसिंग फीस जमा करें।"))
        assertTrue(res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `detects Hindi electricity cut scam`() {
        val res = engine.analyze(ScanInput("आपका बिजली कनेक्शन आज रात काट दी जाएगी। तुरंत भुगतान करें http://bijli-verify.xyz/pay"))
        assertTrue(res.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL))
    }

    @Test fun `detects Hindi explanation output when outputLanguage is hi`() {
        val res = engine.analyze(ScanInput(DemoSamples.messageSamples[2].payload), outputLanguage = "hi")
        assertTrue(res.recommendations.any { r -> r.any { it in '\u0900'..'\u097F' } })
        assertTrue(res.whatCouldHappen.any { it in '\u0900'..'\u097F' })
    }

    @Test fun `performance sanity stays well below 2 seconds per scan`() {
        val start = System.currentTimeMillis()
        DemoSamples.messageSamples.forEach { sample ->
            engine.analyze(ScanInput(sample.payload))
        }
        val elapsed = System.currentTimeMillis() - start
        assertTrue("15 scans took ${elapsed}ms", elapsed < 2000L)
    }

    // --- Legitimate Messages False-Positive Audit (Target: 0 High/Critical false positives) ---

    @Test fun `false positive audit across 15 realistic legitimate Indian messages produces ZERO High or Critical`() {
        val legitMessages = listOf(
            "123456 is your OTP for HDFC Bank NetBanking. Do not share this OTP with anyone. Bank never asks for OTP.",
            "Your Amazon order #402-1234567 has been shipped and will arrive tomorrow.",
            "Reminder: your electricity bill of ₹1,240 is due on 25 Nov. Pay via the official app or website.",
            "आपका बिजली बिल ₹1,240 है, अंतिम तिथि 25 नवंबर। भुगतान आधिकारिक ऐप से करें।",
            "Dear customer, ₹1,500.00 debited from A/C XX4521 on 20-Nov via UPI. If not done by you, call bank helpline.",
            "Your IRCTC ticket PNR 4821940212 is confirmed for 24 Nov. Have a safe journey!",
            "Airtel: Your recharge of ₹299 is successful. Validity 28 days.",
            "Your doctor appointment at Apollo Clinic is scheduled for tomorrow at 10:30 AM.",
            "Hi Mom, reached office safely. Will call you in the evening.",
            "Your SBI NetBanking password reset code is 849201. Do not share OTP with anyone.",
            "Flipkart: Your package is out for delivery today. Share delivery PIN 4412 only at doorstep.",
            "NPCI reminder: Never share your UPI PIN with anyone. Visit https://www.npci.org.in for tips.",
            "Please check the attached class schedule for Friday morning.",
            "Jio: 50% daily data quota used. Manage your plan in MyJio app.",
            "Happy Diwali! Wishing you and your family health and happiness."
        )

        var highOrCriticalCount = 0
        for (msg in legitMessages) {
            val res = engine.analyze(ScanInput(msg))
            if (res.riskLevel == RiskLevel.HIGH || res.riskLevel == RiskLevel.CRITICAL) {
                highOrCriticalCount++
            }
            assertFalse("Legit message '$msg' unexpectedly scored ${res.riskScore} (${res.riskLevel})", res.riskScore >= 60)
        }
        assertEquals(0, highOrCriticalCount)
    }
}
