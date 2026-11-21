package com.example.integrations.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareIntentsHelper {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun readFromClipboard(context: Context): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        val clip = cm.primaryClip ?: return ""
        return if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString().orEmpty() else ""
    }

    fun sendTrustedContactSmsAlert(
        context: Context,
        phoneNumber: String,
        riskLevelLabel: String
    ) {
        val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date())
        val body = "RakshakAI Safety Alert: $riskLevelLabel risk situation detected at $timeStr. Please call me when you see this. (Message content not shared for privacy.)"
        val uri = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun shareTrustedContactAlertSheet(
        context: Context,
        riskLevelLabel: String
    ) {
        val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date())
        val body = "RakshakAI Safety Alert: $riskLevelLabel risk situation detected at $timeStr. Please call me right away. (Message content not shared.)"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Send safety alert via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun dialTrustedContact(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun dialCyberHelpline1930(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun sharePlainText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareExportFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "RakshakAI Decrypted Evidence Archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Evidence Vault").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openUrlInBrowserExplicitlyConfirmed(context: Context, rawUrl: String) {
        val normalized = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
