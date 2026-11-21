package com.example.detection.explain

import com.example.detection.intent.DetectedIntent
import com.example.domain.models.RiskLevel
import com.example.domain.models.UpiAnalysis
import com.example.domain.models.UrlResult

object RecommendationEngine {

    fun build(
        level: RiskLevel,
        uncertain: Boolean,
        categoryId: String?,
        intents: List<DetectedIntent>,
        tactics: List<String>,
        urlResults: List<UrlResult>,
        upiAnalysis: UpiAnalysis?,
        isHindiOutput: Boolean
    ): List<String> {
        val recs = LinkedHashSet<String>()

        if (level == RiskLevel.LOW && !uncertain) {
            if (isHindiOutput) {
                recs.add("अभी कोई गंभीर खतरा नहीं दिखा, लेकिन अनजान नंबर या लिंक पर हमेशा सावधान रहें।")
                recs.add("ओटीपी, यूपीआई पिन या सीवीवी कभी किसी को न बताएं।")
                recs.add("किसी भी भुगतान या जांच के लिए हमेशा आधिकारिक ऐप खुद खोलें।")
            } else {
                recs.add("No strong red flags detected, though this is not a guarantee of safety.")
                recs.add("Never share an OTP, UPI PIN, or CVV with anyone.")
                recs.add("For payments or account updates, open the official app yourself.")
            }
            return recs.toList()
        }

        if (DetectedIntent.OTP in intents || categoryId == "otp_theft") {
            recs.add(
                if (isHindiOutput) "अपना ओटीपी, यूपीआई पिन, सीवीवी या पासवर्ड किसी भी कॉल करने वाले या लिंक पर न दें।"
                else "Do not share your OTP, UPI PIN, CVV, or password with anyone — banks never ask for them."
            )
        }

        if (DetectedIntent.APP_INSTALL in intents || categoryId == "remote_access") {
            recs.add(
                if (isHindiOutput) "AnyDesk, TeamViewer या कोई अनजान APK ऐप इंस्टॉल न करें और स्क्रीन शेयर न करें।"
                else "Do not install AnyDesk, TeamViewer, QuickSupport, or APK files, and never share a 9-digit code."
            )
        }

        if ("isolation" in tactics || categoryId == "digital_arrest") {
            recs.add(
                if (isHindiOutput) "तुरंत कॉल काटें और किसी भरोसेमंद परिवारजन से बात करें। असली पुलिस वीडियो कॉल पर 'डिजिटल अरेस्ट' या पैसे की मांग नहीं करती।"
                else "Hang up immediately and speak to a family member. Real police or CBI never conduct 'digital arrest' or demand safe-account transfers."
            )
        }

        if (upiAnalysis != null || DetectedIntent.MONEY in intents || categoryId == "payment_refund") {
            recs.add(
                if (isHindiOutput) "पैसे पाने के लिए कभी क्यूआर कोड स्कैन न करें और न ही यूपीआई पिन डालें।"
                else "Never scan a QR code or enter your UPI PIN to receive money or refunds."
            )
        }

        if (urlResults.isNotEmpty() || DetectedIntent.LINK_CLICK in intents) {
            recs.add(
                if (isHindiOutput) "इस लिंक पर क्लिक न करें। अपना बैंक या सेवा का आधिकारिक ऐप खुद खोलकर स्थिति जांचें।"
                else "Do not tap this link. Open your official bank or service app directly to check your status."
            )
        }

        if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL || uncertain) {
            recs.add(
                if (isHindiOutput) "कोई कदम उठाने से पहले रक्षकएआई प्री-एक्शन सुरक्षा जांच चलाएं या परिवार के किसी व्यक्ति से पूछें।"
                else "Run RakshakAI Pre-Action Safety Check or pause and verify with a trusted family member."
            )
        }

        if (level == RiskLevel.CRITICAL || level == RiskLevel.HIGH) {
            recs.add(
                if (isHindiOutput) "अगर पैसे कट गए हैं या जानकारी साझा हो गई है, तो तुरंत 1930 पर कॉल करें और cybercrime.gov.in पर रिपोर्ट करें।"
                else "If money was transferred or credentials shared, call 1930 (verify official helpline) and report at cybercrime.gov.in."
            )
        }

        return recs.toList()
    }
}
