package com.example.privacy.data_management

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

data class RuntimePermissionAudit(
    val declaredPermissions: List<String>,
    val internetRequested: Boolean,
    val networkStateRequested: Boolean,
    val cameraGranted: Boolean,
    val smsReceiveGranted: Boolean,
    val notificationsGranted: Boolean
)

data class StorageStats(
    val historyCount: Int,
    val vaultCount: Int,
    val quarantineCount: Int,
    val contactsCount: Int,
    val totalDiskBytes: Long
)

object PrivacyAuditHelper {

    fun inspectPermissions(context: Context): RuntimePermissionAudit {
        val pm = context.packageManager
        val pkgName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                pkgName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS)
        }

        val declared = packageInfo.requestedPermissions?.toList().orEmpty()
        val hasInternet = declared.any { it == android.Manifest.permission.INTERNET }
        val hasNetState = declared.any { it == android.Manifest.permission.ACCESS_NETWORK_STATE }

        val cameraGranted = context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        val smsGranted = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return RuntimePermissionAudit(
            declaredPermissions = declared,
            internetRequested = hasInternet,
            networkStateRequested = hasNetState,
            cameraGranted = cameraGranted,
            smsReceiveGranted = smsGranted,
            notificationsGranted = notifGranted
        )
    }

    fun calculateAppDataBytes(context: Context): Long {
        var total = 0L
        val dbFile = context.getDatabasePath("rakshakai.db")
        if (dbFile.exists()) total += dbFile.length()
        val dbWal = File(dbFile.parentFile, "rakshakai.db-wal")
        if (dbWal.exists()) total += dbWal.length()
        val vaultDir = File(context.filesDir, "vault_media")
        if (vaultDir.exists()) {
            total += vaultDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return total
    }

    fun vaultMediaDir(context: Context): File {
        return File(context.filesDir, "vault_media").apply {
            if (!exists()) mkdirs()
        }
    }

    fun deleteAllPrivateFiles(context: Context) {
        val vaultDir = File(context.filesDir, "vault_media")
        if (vaultDir.exists()) vaultDir.deleteRecursively()
        val exportsDir = File(context.cacheDir, "exports")
        if (exportsDir.exists()) exportsDir.deleteRecursively()
    }
}
