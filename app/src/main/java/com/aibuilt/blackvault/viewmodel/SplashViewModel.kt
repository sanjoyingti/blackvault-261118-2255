package com.aibuilt.blackvault.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.blackvault.BlackVaultApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.security.MessageDigest

class SplashViewModel(app: Application) : AndroidViewModel(app) {

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun boot() {
        viewModelScope.launch {
            add("BLACKVAULT v1.0")
            add("STEALTH ENGINE .......... ONLINE")
            add("KERNEL ${Build.DISPLAY}")
            add("DEVICE ${Build.MANUFACTURER} ${Build.MODEL}")
            add("ANDROID ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
            add("BUILD PRINT ${fingerprint()}")
            add("NETWORK ${networkType()}")
            add("RESOLVING PUBLIC EGRESS …")
            val ip = publicIp()
            add(if (ip != null) "EGRESS $ip" else "EGRESS unresolved (offline)")
            add("VAULT READY — HANDING OVER")
        }
    }

    private fun add(text: String) {
        _lines.value = _lines.value + text
    }

    private fun fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(Build.FINGERPRINT.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun networkType(): String {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "UNKNOWN"
        val nc = cm.getNetworkCapabilities(cm.activeNetwork ?: return "OFFLINE") ?: return "OFFLINE"
        return when {
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "OTHER"
        }
    }

    private suspend fun publicIp(): String? = withContext(Dispatchers.IO) {
        val client = (getApplication() as? BlackVaultApp)?.container?.client ?: return@withContext null
        runCatching {
            client.newCall(Request.Builder().url("https://api.ipify.org").build()).execute().use { r ->
                if (r.isSuccessful) r.body?.string()?.trim() else null
            }
        }.getOrNull()
    }
}
