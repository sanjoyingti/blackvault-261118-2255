package com.aibuilt.blackvault.data

import android.content.Context
import com.aibuilt.blackvault.engine.PayoutEngine
import com.aibuilt.blackvault.engine.ReconEngine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val db: VaultDatabase = VaultDatabase.build(appContext)
    val secure: SecureStore = SecureStore(appContext)
    val settings: SettingsStore = SettingsStore(appContext)
    val recon: ReconEngine = ReconEngine(client)
    val payout: PayoutEngine = PayoutEngine(client, secure)
}
