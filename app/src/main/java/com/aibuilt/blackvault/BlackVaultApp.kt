package com.aibuilt.blackvault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aibuilt.blackvault.data.AppContainer

class BlackVaultApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        channel()
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Vault runs",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            ch.description = "Payout and recon completion"
            mgr?.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_ID = "vault_runs"
    }
}
