package com.omv.client.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "omv_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var hostPort: String
        get() = prefs.getString("host_port", "") ?: ""
        set(value) = prefs.edit().putString("host_port", value).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    var useHttps: Boolean
        get() = prefs.getBoolean("use_https", false)
        set(value) = prefs.edit().putBoolean("use_https", value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean("remember_me", true)
        set(value) = prefs.edit().putBoolean("remember_me", value).apply()

    var sessionId: String
        get() = prefs.getString("session_id", "") ?: ""
        set(value) = prefs.edit().putString("session_id", value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(value) = prefs.edit().putBoolean("first_launch", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0)
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var accentColor: Int
        get() = prefs.getInt("accent_color", 0)
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    var sessionCookie: String
        get() = prefs.getString("session_cookie", "") ?: ""
        set(value) = prefs.edit().putString("session_cookie", value).apply()

    var cookieExpiry: Long
        get() = prefs.getLong("cookie_expiry", 0)
        set(value) = prefs.edit().putLong("cookie_expiry", value).apply()

    var language: String
        get() = prefs.getString("language", "") ?: ""
        set(value) = prefs.edit().putString("language", value).apply()

    var diskThreshold: Int
        get() = prefs.getInt("disk_threshold", 10)
        set(value) = prefs.edit().putInt("disk_threshold", value).apply()

    var notifyDiskLow: Boolean
        get() = prefs.getBoolean("notify_disk_low", true)
        set(value) = prefs.edit().putBoolean("notify_disk_low", value).apply()

    var notifyContainers: Boolean
        get() = prefs.getBoolean("notify_containers", true)
        set(value) = prefs.edit().putBoolean("notify_containers", value).apply()

    var notifyUpdates: Boolean
        get() = prefs.getBoolean("notify_updates", true)
        set(value) = prefs.edit().putBoolean("notify_updates", value).apply()

    var lastBackupDate: String
        get() = prefs.getString("last_backup_date", "") ?: ""
        set(value) = prefs.edit().putString("last_backup_date", value).apply()

    fun saveWidgetCache(cpu: String, ram: String, disks: String, hostname: String) {
        val wp = context.getSharedPreferences("omv_widget_cache", Context.MODE_PRIVATE)
        wp.edit()
            .putString("cpu", cpu)
            .putString("ram", ram)
            .putString("disks", disks)
            .putString("hostname", hostname)
            .apply()
    }

    fun loadWidgetCache(): Map<String, String> {
        val wp = context.getSharedPreferences("omv_widget_cache", Context.MODE_PRIVATE)
        return mapOf(
            "cpu" to (wp.getString("cpu", "—") ?: "—"),
            "ram" to (wp.getString("ram", "—") ?: "—"),
            "disks" to (wp.getString("disks", "—") ?: "—"),
            "hostname" to (wp.getString("hostname", "—") ?: "—")
        )
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun logout() {
        prefs.edit()
            .remove("session_id")
            .remove("session_cookie")
            .remove("cookie_expiry")
            .apply()
    }

    fun hasValidSession(): Boolean {
        return sessionCookie.isNotEmpty() && cookieExpiry > System.currentTimeMillis()
    }

    fun getBaseUrl(): String {
        val proto = if (useHttps) "https" else "http"
        return "$proto://$hostPort/"
    }
}
