package no.netspire.steintroll

import android.app.Application
import android.telephony.TelephonyManager
import no.netspire.steintroll.data.CountryRepository
import no.netspire.steintroll.data.SettingsRepository
import no.netspire.steintroll.data.SteintrollDatabase

class SteintrollApp : Application() {
    val database by lazy { SteintrollDatabase.get(this) }
    val blockedCallDao by lazy { database.blockedCallDao() }
    val settingsRepository by lazy { SettingsRepository(this) }
    val countryRepository by lazy { CountryRepository() }

    /**
     * The device's home-country calling code (e.g. "47" for Norway), derived from the
     * SIM/network ISO. Defaults to Norway if it can't be determined. Used to treat
     * prefix-less domestic numbers as home, and to exclude home from spam suggestions.
     */
    fun deviceHomeDialCode(): String {
        val tm = getSystemService(TelephonyManager::class.java)
        val iso = (tm?.simCountryIso ?: tm?.networkCountryIso)?.uppercase()
        return countryRepository.all.firstOrNull { it.iso == iso }?.dialCode ?: "47"
    }
}
