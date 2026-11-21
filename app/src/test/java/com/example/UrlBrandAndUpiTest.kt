package com.example

import com.example.detection.engine.RuleBasedScamEngine
import com.example.detection.upi.UpiQrAnalyzer
import com.example.detection.url.UrlAnalyzer
import com.example.detection.url.UrlExtractor
import com.example.domain.models.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlBrandAndUpiTest {

    private val engine = RuleBasedScamEngine(FileSystemRuleSource())
    private val bundle = engine.getRuleBundleOrNull()!!
    private val urlAnalyzer = UrlAnalyzer(
        brands = bundle.brands.brands,
        suspiciousTlds = bundle.suspiciousTlds.suspiciousTlds.toSet(),
        shorteners = bundle.suspiciousTlds.shorteners.toSet(),
        suspiciousPathKeywords = bundle.suspiciousTlds.suspiciousPathKeywords.toSet(),
        twoLevelSuffixes = bundle.publicSuffixes.twoLevelSuffixes.toSet()
    )

    @Test fun `extracts explicit urls, upi uris, phones, and amounts`() {
        val extracted = UrlExtractor.extract(
            "Pay ₹5,000 via upi://pay?pa=test@upi&am=5000 or visit http://bit.ly/abc123 or call +91 9876543210",
            suspiciousTlds = bundle.suspiciousTlds.suspiciousTlds.toSet(),
            shorteners = bundle.suspiciousTlds.shorteners.toSet()
        )
        assertTrue(extracted.urls.any { it.contains("bit.ly") })
        assertTrue(extracted.upiUris.any { it.startsWith("upi://pay") })
        assertTrue(extracted.amounts.isNotEmpty())
        assertTrue(extracted.phoneNumbers.isNotEmpty())
    }

    @Test fun `registered domain respects two level Indian suffixes`() {
        assertEquals("sbi.co.in", urlAnalyzer.computeRegisteredDomain("onlinesbi.sbi.co.in"))
        assertEquals("uidai.gov.in", urlAnalyzer.computeRegisteredDomain("myaadhaar.uidai.gov.in"))
        assertEquals("attacker.test", urlAnalyzer.computeRegisteredDomain("sbi.co.in.secure-login.attacker.test"))
    }

    @Test fun `flags IP address URL as high risk`() {
        val res = urlAnalyzer.analyze("http://192.168.4.7/login")
        assertTrue("Expected score >= 60, was ${res.score}", res.score >= 60)
    }

    @Test fun `flags punycode xn-- host`() {
        val res = urlAnalyzer.analyze("https://xn--sb-kyc-0va.com/verify")
        assertTrue(res.score >= 50)
        assertTrue(res.flags.any { it.contains("Punycode", true) })
    }

    @Test fun `flags url shortener as uncertain`() {
        val res = urlAnalyzer.analyze("https://bit.ly/abc123")
        assertTrue(res.isShortener)
        assertTrue(res.isUncertain)
        assertTrue(res.summary.contains("not verified", true))
    }

    @Test fun `flags suspicious TLD dot top and dot xyz`() {
        val res1 = urlAnalyzer.analyze("http://courier-track.top/pay")
        val res2 = urlAnalyzer.analyze("https://claim-offer.xyz/reward")
        assertTrue(res1.score >= 45)
        assertTrue(res2.score >= 35)
    }

    @Test fun `flags at sign in URL`() {
        val res = urlAnalyzer.analyze("https://sbi.co.in@evil.xyz/login")
        assertTrue(res.score >= 60)
        assertTrue(res.flags.any { it.contains("@") })
    }

    @Test fun `flags deep subdomain brand impersonation mismatch`() {
        val res = urlAnalyzer.analyze("https://sbi.co.in.secure-login.attacker.test/kyc")
        assertTrue(res.isBrandMismatch)
        assertTrue("Expected high score, was ${res.score}", res.score >= 60)
    }

    @Test fun `flags hyphen padded brand lookalike domain`() {
        val res = urlAnalyzer.analyze("http://sbi-kyc-update.xyz/login")
        assertTrue(res.score >= 80)
    }

    @Test fun `flags typosquatting levenshtein lookalike domain`() {
        val res = urlAnalyzer.analyze("https://hdfcbnak.com/login")
        assertTrue("Expected lookalike score >= 60, was ${res.score}", res.score >= 60)
    }

    @Test fun `flags homoglyph substitution lookalike domain`() {
        val res = urlAnalyzer.analyze("https://paytm0ffice.com/refund")
        assertTrue(res.score >= 30)
    }

    @Test fun `flags direct apk download URL`() {
        val res = urlAnalyzer.analyze("https://support-help.site/anydesk.apk")
        assertTrue(res.score >= 70)
    }

    @Test fun `flags non-standard port and very long URL`() {
        val longPath = "a".repeat(80)
        val res = urlAnalyzer.analyze("http://verify-account.online:8443/$longPath")
        assertTrue(res.score >= 55)
    }

    @Test fun `official domain caps score at 10`() {
        val res = urlAnalyzer.analyze("https://www.sbi.co.in")
        assertTrue(res.isOfficialMatch)
        assertTrue(res.score <= 10)
    }

    @Test fun `https alone never guarantees safety on suspicious domain`() {
        val res = urlAnalyzer.analyze("https://kyc-verify-now.xyz/login")
        assertFalse(res.isOfficialMatch)
        assertTrue(res.score >= 35)
    }

    @Test fun `malformed URL never throws exception`() {
        val res = urlAnalyzer.analyze("http://[invalid::host:::/??")
        assertTrue(res.isUncertain)
    }

    @Test fun `genuine bank URL inside suspicious message still flags suspicious content`() {
        val res = engine.analyzeUrl("Share your OTP and PIN right now via call or check https://www.sbi.co.in")
        assertTrue("Content demand should stay elevated (${res.riskScore})", res.riskScore >= 60)
    }

    @Test fun `parses UPI refund QR and warns without launching payment`() {
        val upi = UpiQrAnalyzer.analyze("upi://pay?pa=refund.helpdesk@upi&pn=Refund%20Desk&am=5000&tn=Receive%20refund")
        assertEquals("refund.helpdesk@upi", upi.payeeAddress)
        assertEquals("Refund Desk", upi.payeeName)
        assertEquals("5000", upi.amount)
        assertTrue(upi.riskScore >= 60)
        assertTrue(upi.warnings.any { it.contains("RECEIVE", ignoreCase = true) })
    }

    @Test fun `scanQrPayload marks refund UPI trap High or Critical`() {
        val res = engine.analyzeQrPayload("upi://pay?pa=refund.helpdesk@upi&pn=Refund%20Desk&am=5000&tn=Receive%20refund")
        assertTrue(res.riskLevel == RiskLevel.HIGH || res.riskLevel == RiskLevel.CRITICAL)
    }
}
