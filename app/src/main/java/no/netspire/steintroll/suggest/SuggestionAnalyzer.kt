package no.netspire.steintroll.suggest

import no.netspire.steintroll.block.NumberParser
import no.netspire.steintroll.block.ParsedNumber
import no.netspire.steintroll.data.CountryRepository

/** Direction/outcome of a call-log entry, abstracted from android.provider.CallLog. */
enum class CallType {
    INCOMING_ANSWERED, // you picked it up
    OUTGOING,          // you called them
    MISSED,            // rang, not answered
    REJECTED,          // you declined
    BLOCKED,           // already blocked by the system/an app
    OTHER,
}

/** One row from the call log, abstracted for testability. */
data class CallLogEntry(val number: String?, val type: CallType)

/** A suggested country code to block, with why. */
data class CountrySuggestion(
    val dialCode: String,
    val countryName: String,
    val flag: String,
    val callCount: Int, // number of unanswered foreign calls from this code
)

/**
 * Pure analysis: from a call log, suggest FOREIGN country codes that look like spam.
 *
 * Rules (home country and already-blocked codes are always excluded):
 *  - An OUTGOING call to a code is a strong relationship signal → never suggest it
 *    (if you've called them, they're not spam).
 *  - ANSWERED incoming calls are a weak signal, not an absolute veto: a code that used
 *    to be legitimate but now spams should still surface.
 *  - Suggest a code when its unanswered (missed + rejected) calls strongly outweigh
 *    answered ones: unanswered >= [SPAM_RATIO] * answered, and unanswered >= [MIN_UNANSWERED].
 *
 * No Android dependencies.
 */
class SuggestionAnalyzer(
    private val countries: CountryRepository,
    private val homeDialCode: String,
) {
    private val parser = NumberParser(countries, homeDialCode)

    private data class Counts(var answered: Int = 0, var unanswered: Int = 0)

    fun analyze(entries: List<CallLogEntry>, alreadyBlocked: Set<String>): List<CountrySuggestion> {
        // Codes you've actively called → never suggest.
        val calledCodes = mutableSetOf<String>()
        val counts = mutableMapOf<String, Counts>()

        for (e in entries) {
            val code = resolveForeignCode(e.number) ?: continue
            when (e.type) {
                CallType.OUTGOING -> calledCodes += code
                CallType.INCOMING_ANSWERED ->
                    counts.getOrPut(code) { Counts() }.answered++
                CallType.MISSED, CallType.REJECTED ->
                    counts.getOrPut(code) { Counts() }.unanswered++
                CallType.BLOCKED, CallType.OTHER -> { /* ignore */ }
            }
        }

        return counts
            .filterKeys { it !in calledCodes && it !in alreadyBlocked }
            .filterValues { it.unanswered >= MIN_UNANSWERED && it.unanswered >= SPAM_RATIO * it.answered }
            .mapNotNull { (code, c) ->
                val country = countries.byDialCode(code) ?: return@mapNotNull null
                CountrySuggestion(code, country.name, country.flag, c.unanswered)
            }
            .sortedByDescending { it.callCount }
    }

    companion object {
        /** Minimum unanswered foreign calls before a code is worth suggesting. */
        const val MIN_UNANSWERED = 2
        /** Unanswered must be at least this multiple of answered calls. */
        const val SPAM_RATIO = 3
    }

    /** Returns the dial code if the number resolves to a known FOREIGN country, else null. */
    private fun resolveForeignCode(number: String?): String? {
        val parsed = parser.parse(number, isWithheld = false)
        val code = (parsed as? ParsedNumber.Resolved)?.dialCode ?: return null
        if (code == homeDialCode) return null
        // Only suggest codes we can name (present in the table).
        return if (countries.byDialCode(code) != null) code else null
    }
}
