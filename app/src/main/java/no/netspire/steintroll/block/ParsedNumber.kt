package no.netspire.steintroll.block

sealed interface ParsedNumber {
    /** A recognizable calling code was found (or assumed home country for domestic). */
    data class Resolved(val dialCode: String, val nationalNumber: String) : ParsedNumber
    /** A number was present but no calling code could be resolved. */
    data object NoCountryCode : ParsedNumber
    /** No number available (withheld/private/restricted presentation). */
    data object Withheld : ParsedNumber
}
