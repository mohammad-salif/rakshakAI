package com.example.integrations.appscan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class RemoteAppCandidate(
    val packageName: String,
    val displayName: String
)

object RemoteAccessAppScanner {

    val monitoredApps = listOf(
        RemoteAppCandidate("com.anydesk.anydeskandroid", "AnyDesk"),
        RemoteAppCandidate("com.teamviewer.quicksupport.market", "TeamViewer QuickSupport"),
        RemoteAppCandidate("com.teamviewer.teamviewer.market.mobile", "TeamViewer"),
        RemoteAppCandidate("com.carriez.flutter_hbb", "RustDesk"),
        RemoteAppCandidate("com.sand.airdroid", "AirDroid"),
        RemoteAppCandidate("com.google.chromeremotedesktop", "Chrome Remote Desktop")
    )

    fun scanInstalledRemoteApps(context: Context): List<String> {
        val pm = context.packageManager
        val found = mutableListOf<String>()
        for (app in monitoredApps) {
            val isInstalled = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(app.packageName, 0)
                }
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
            if (isInstalled) {
                found.add(app.displayName)
            }
        }
        return found
    }
}
