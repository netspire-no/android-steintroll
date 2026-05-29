package no.netspire.steintroll.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "steintroll_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val BLOCK_CODES = stringSetPreferencesKey("block_codes")
        val ALLOW_CODES = stringSetPreferencesKey("allow_codes")
        val BLOCK_WITHHELD = booleanPreferencesKey("block_withheld")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            mode = when (p[Keys.MODE]) { "ALLOWLIST" -> Mode.ALLOWLIST; else -> Mode.BLOCKLIST },
            blockCodes = p[Keys.BLOCK_CODES] ?: emptySet(),
            allowCodes = p[Keys.ALLOW_CODES] ?: emptySet(),
            blockWithheld = p[Keys.BLOCK_WITHHELD] ?: false,
        )
    }

    suspend fun setMode(mode: Mode) =
        context.dataStore.edit { it[Keys.MODE] = mode.name }.let {}

    /** Adds a code to the list matching the current mode (block-list or allow-list). */
    suspend fun addCodeForCurrentMode(code: String) = context.dataStore.edit { p ->
        val key = if (p[Keys.MODE] == "ALLOWLIST") Keys.ALLOW_CODES else Keys.BLOCK_CODES
        p[key] = (p[key] ?: emptySet()) + code
    }.let {}

    /** Removes a code from the list matching the current mode. */
    suspend fun removeCodeForCurrentMode(code: String) = context.dataStore.edit { p ->
        val key = if (p[Keys.MODE] == "ALLOWLIST") Keys.ALLOW_CODES else Keys.BLOCK_CODES
        p[key] = (p[key] ?: emptySet()) - code
    }.let {}

    suspend fun setBlockWithheld(value: Boolean) =
        context.dataStore.edit { it[Keys.BLOCK_WITHHELD] = value }.let {}
}
