package com.example.detection

data class SeedDemoSample(
    val id: String,
    val title: String,
    val expectedBadge: String,
    val mode: String, // MESSAGE, CONVERSATION, LINK, QR, CALL
    val payload: String
)

object DemoSamples {

    val messageSamples: List<SeedDemoSample> = listOf(
        SeedDemoSample(
            id = "s1",
            title = "1. Fake SBI KYC + Lookalike URL",
            expectedBadge = "Critical",
            mode = "MESSAGE",
            payload = "Dear customer, your SBI account will be blocked today. Update KYC immediately: http://sbi-kyc-update.xyz/login"
        ),
        SeedDemoSample(
            id = "s2",
            title = "2. Hinglish KYC + Shortened URL",
            expectedBadge = "High/Critical",
            mode = "MESSAGE",
            payload = "Aapka SBI account aaj raat band ho jayega. KYC update karne ke liye turant is link pe click karein: http://bit.ly/3xKyc"
        ),
        SeedDemoSample(
            id = "s3",
            title = "3. Hindi (Devanagari) KYC + OTP Trap",
            expectedBadge = "High/Critical",
            mode = "MESSAGE",
            payload = "आपका बैंक खाता आज रात बंद हो जाएगा। तुरंत KYC अपडेट करें और अपना OTP बताएं।"
        ),
        SeedDemoSample(
            id = "s4",
            title = "4. Direct OTP Theft (No Link)",
            expectedBadge = "High",
            mode = "MESSAGE",
            payload = "Share your OTP to verify your account."
        ),
        SeedDemoSample(
            id = "s5",
            title = "5. KBC Lottery Fee Scam",
            expectedBadge = "High",
            mode = "MESSAGE",
            payload = "Congratulations! You won ₹50 lakh in KBC lottery. Pay ₹5000 processing fee to claim."
        ),
        SeedDemoSample(
            id = "s6",
            title = "6. Part-Time Job Registration Scam",
            expectedBadge = "High",
            mode = "MESSAGE",
            payload = "Part time job, earn ₹5000/day. Pay ₹500 registration fee to this UPI ID to start."
        ),
        SeedDemoSample(
            id = "s7",
            title = "7. Investment 5x Returns Lure",
            expectedBadge = "Medium-High",
            mode = "MESSAGE",
            payload = "Guaranteed 5x returns in 7 days. Join our WhatsApp group and deposit now."
        ),
        SeedDemoSample(
            id = "s8",
            title = "8. Electricity Disconnection (Unseen Paraphrase)",
            expectedBadge = "High",
            mode = "MESSAGE",
            payload = "Your electricity connection will be disconnected tonight. Pay the verification amount using this link."
        ),
        SeedDemoSample(
            id = "s9",
            title = "9. Remote Access AnyDesk + 9-Digit Code",
            expectedBadge = "Critical",
            mode = "MESSAGE",
            payload = "Install AnyDesk to get your refund. Tell me the 9 digit code you see."
        ),
        SeedDemoSample(
            id = "s10",
            title = "10. Parcel Customs / .top TLD Scam",
            expectedBadge = "High",
            mode = "MESSAGE",
            payload = "Pay ₹10 to release your parcel: http://courier-track.top/pay"
        ),
        SeedDemoSample(
            id = "s11",
            title = "11. Legitimate Bank OTP Warning",
            expectedBadge = "Low",
            mode = "MESSAGE",
            payload = "123456 is your OTP for HDFC Bank NetBanking. Do not share this OTP with anyone. Bank never asks for OTP."
        ),
        SeedDemoSample(
            id = "s12",
            title = "12. Legitimate Amazon Delivery Update",
            expectedBadge = "Low",
            mode = "MESSAGE",
            payload = "Your Amazon order #402-1234567 has been shipped and will arrive tomorrow."
        ),
        SeedDemoSample(
            id = "s13",
            title = "13. Legitimate Electricity Bill Reminder",
            expectedBadge = "Low",
            mode = "MESSAGE",
            payload = "Reminder: your electricity bill of ₹1,240 is due on 25 Nov. Pay via the official app or website."
        ),
        SeedDemoSample(
            id = "s14",
            title = "14. Legitimate Hindi Electricity Reminder",
            expectedBadge = "Low",
            mode = "MESSAGE",
            payload = "आपका बिजली बिल ₹1,240 है, अंतिम तिथि 25 नवंबर। भुगतान आधिकारिक ऐप से करें।"
        ),
        SeedDemoSample(
            id = "s15",
            title = "15. Short Greeting (Insufficient Context)",
            expectedBadge = "Low / Uncertain",
            mode = "MESSAGE",
            payload = "Hi"
        )
    )

    val digitalArrestConversationText = """
        Them: Hello, this is Inspector Rajesh Verma from CBI cyber cell. A parcel in your name contains illegal items.
        Them: A case is registered against you. You may be arrested today.
        Them: Do not tell anyone, and do not cut this call. Your family will be involved.
        Them: To clear your name, transfer ₹80,000 to the RBI safe account for verification.
    """.trimIndent()

    val scriptedDemoCallLines = listOf(
        "Caller: Hello, I am Inspector Rajesh Verma calling from CBI Cyber Crime Cell, Mumbai.",
        "Caller: A FedEx parcel sent with your Aadhaar contains illegal items and narcotics.",
        "Caller: An FIR is already registered against you. A warrant may execute today.",
        "Caller: This is a national security investigation. Do not tell anyone in your family and do not cut this call.",
        "Caller: Keep your camera on. To prove your innocence, transfer ₹80,000 to the RBI safe account right now."
    )

    val qrUpiDemoPayload = "upi://pay?pa=refund.helpdesk@upi&pn=Refund%20Desk&am=5000&tn=Receive%20refund"

    val linkDemos = listOf(
        SeedDemoSample(
            id = "l1",
            title = "Brand Subdomain Mismatch (sbi.co.in.secure-login.attacker.test)",
            expectedBadge = "High",
            mode = "LINK",
            payload = "https://sbi.co.in.secure-login.attacker.test/kyc"
        ),
        SeedDemoSample(
            id = "l2",
            title = "Raw IP Address Link (192.168.4.7)",
            expectedBadge = "High",
            mode = "LINK",
            payload = "http://192.168.4.7/login"
        ),
        SeedDemoSample(
            id = "l3",
            title = "Official SBI Domain (www.sbi.co.in)",
            expectedBadge = "Lower Risk",
            mode = "LINK",
            payload = "https://www.sbi.co.in"
        ),
        SeedDemoSample(
            id = "l4",
            title = "Shortened URL (bit.ly)",
            expectedBadge = "Uncertain",
            mode = "LINK",
            payload = "https://bit.ly/abc123"
        )
    )
}
