package no.netspire.steintroll.block

import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.data.BlockedCall

class SteintrollCallScreeningService : CallScreeningService() {

    private val app get() = application as SteintrollApp
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        // Only screen incoming calls.
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val handle = callDetails.handle // tel: Uri or null
        val rawNumber = handle?.schemeSpecificPart
        val isWithheld = rawNumber.isNullOrBlank()

        scope.launch {
            val settings = app.settingsRepository.settings.first()
            val homeCode = app.deviceHomeDialCode()
            val parser = NumberParser(app.countryRepository, homeCode)
            val parsed = parser.parse(rawNumber, isWithheld)

            when (BlockDecision.decide(parsed, settings)) {
                is Decision.Allow ->
                    respondToCall(callDetails, CallResponse.Builder().build())

                is Decision.Block -> {
                    respondToCall(
                        callDetails,
                        CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipNotification(true)
                            .setSkipCallLog(true)
                            .build(),
                    )
                    logBlockedCall(parsed, rawNumber)
                }
            }
        }
    }

    private suspend fun logBlockedCall(parsed: ParsedNumber, rawNumber: String?) {
        val described = describe(parsed)
        runCatching {
            app.blockedCallDao.insert(
                BlockedCall(
                    rawNumber = rawNumber,
                    dialCode = described.dialCode,
                    countryName = described.country,
                    flag = described.flag,
                    timestamp = System.currentTimeMillis(),
                    wasWithheld = described.withheld,
                )
            )
        }
    }

    private data class Described(
        val dialCode: String?, val country: String?, val flag: String?, val withheld: Boolean,
    )

    private fun describe(parsed: ParsedNumber): Described = when (parsed) {
        is ParsedNumber.Withheld -> Described(null, null, null, true)
        is ParsedNumber.NoCountryCode -> Described(null, null, null, false)
        is ParsedNumber.Resolved -> {
            val c = app.countryRepository.byDialCode(parsed.dialCode)
            Described(parsed.dialCode, c?.name, c?.flag, false)
        }
    }
}
