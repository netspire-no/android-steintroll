package no.netspire.steintroll.block

import no.netspire.steintroll.data.CountryRepository

/**
 * Resolves a raw phone string into a [ParsedNumber].
 *
 * @param homeDialCode the device's home-country calling code (e.g. "47"); a number
 *        with no international prefix is assumed to be a domestic call from this code.
 */
class NumberParser(
    private val countries: CountryRepository,
    private val homeDialCode: String,
) {
    private val codesLongestFirst = countries.dialCodesLongestFirst

    fun parse(raw: String?, isWithheld: Boolean): ParsedNumber {
        if (isWithheld || raw.isNullOrBlank()) return ParsedNumber.Withheld

        // Normalize: keep leading +, strip spaces/dashes/parens.
        var n = raw.trim().replace(Regex("[\\s\\-()]"), "")
        // Convert international 00-prefix to +.
        if (n.startsWith("00")) n = "+" + n.substring(2)

        if (n.startsWith("+")) {
            val digits = n.substring(1)
            // Longest-prefix match against known codes.
            for (code in codesLongestFirst) {
                if (digits.startsWith(code)) {
                    return ParsedNumber.Resolved(code, digits.removePrefix(code))
                }
            }
            return ParsedNumber.NoCountryCode
        }

        // No '+' and not 00-prefixed → domestic number, assume home country.
        val justDigits = n.filter { it.isDigit() }
        return if (justDigits.isNotEmpty())
            ParsedNumber.Resolved(homeDialCode, justDigits)
        else ParsedNumber.NoCountryCode
    }
}
