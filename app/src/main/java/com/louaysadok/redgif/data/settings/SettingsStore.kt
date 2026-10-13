package com.louaysadok.redgif.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
    BLACK("Black (AMOLED)"),
}

enum class AutoplayMode(val label: String) {
    ALWAYS("Always"),
    WIFI_ONLY("Wi-Fi only"),
    OFF("Off"),
}

enum class VideoQuality(val label: String) {
    AUTO("Auto (HD on Wi-Fi, SD on mobile data)"),
    HD("Always HD"),
    SD("Always SD (save data)"),
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val autoplay: AutoplayMode = AutoplayMode.WIFI_ONLY,
    val quality: VideoQuality = VideoQuality.AUTO,
    val muted: Boolean = true,
    val adultConfirmed: Boolean = false,
    val signedIn: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val THEME = stringPreferencesKey("theme")
private val AUTOPLAY = stringPreferencesKey("autoplay")
private val QUALITY = stringPreferencesKey("quality")
private val MUTED = booleanPreferencesKey("muted")
private val ADULT = booleanPreferencesKey("adult_confirmed")
private val SIGNED_IN = booleanPreferencesKey("signed_in")

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toAppSettings())
            prefs[THEME] = next.theme.name
            prefs[AUTOPLAY] = next.autoplay.name
            prefs[QUALITY] = next.quality.name
            prefs[MUTED] = next.muted
            prefs[ADULT] = next.adultConfirmed
            prefs[SIGNED_IN] = next.signedIn
        }
    }
}

private fun Preferences.toAppSettings() = AppSettings(
    theme = enumOr(this[THEME], ThemeMode.SYSTEM),
    autoplay = enumOr(this[AUTOPLAY], AutoplayMode.WIFI_ONLY),
    quality = enumOr(this[QUALITY], VideoQuality.AUTO),
    muted = this[MUTED] ?: true,
    adultConfirmed = this[ADULT] ?: false,
    signedIn = this[SIGNED_IN] ?: false,
)

private inline fun <reified T : Enum<T>> enumOr(raw: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default
