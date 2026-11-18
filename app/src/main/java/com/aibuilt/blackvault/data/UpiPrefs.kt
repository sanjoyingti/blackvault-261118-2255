package com.aibuilt.blackvault.data

import android.content.Context

class UpiPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("blackvault_upi", Context.MODE_PRIVATE)

    var vpa: String
        get() = prefs.getString("vpa", "").orEmpty()
        set(value) {
            prefs.edit().putString("vpa", value.trim()).apply()
        }
}
