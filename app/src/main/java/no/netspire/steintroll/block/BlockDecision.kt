package no.netspire.steintroll.block

import no.netspire.steintroll.data.Mode
import no.netspire.steintroll.data.Settings

sealed interface Decision {
    data object Allow : Decision
    data class Block(val reason: String) : Decision
}

/** Pure blocking logic. No Android dependencies. */
object BlockDecision {
    fun decide(number: ParsedNumber, settings: Settings): Decision = when (number) {
        is ParsedNumber.Withheld ->
            if (settings.blockWithheld) Decision.Block("withheld") else Decision.Allow

        is ParsedNumber.NoCountryCode -> when (settings.mode) {
            Mode.BLOCKLIST -> Decision.Allow                 // domestic/unknown → allow
            Mode.ALLOWLIST -> Decision.Block("not an allowed country code")
        }

        is ParsedNumber.Resolved -> {
            val listed = number.dialCode in settings.activeCodes
            when (settings.mode) {
                Mode.BLOCKLIST ->
                    if (listed) Decision.Block("+${number.dialCode} is blocked") else Decision.Allow
                Mode.ALLOWLIST ->
                    if (listed) Decision.Allow else Decision.Block("+${number.dialCode} not allowed")
            }
        }
    }
}
