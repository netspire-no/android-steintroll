package no.netspire.steintroll.suggest

import com.google.common.truth.Truth.assertThat
import no.netspire.steintroll.data.CountryRepository
import org.junit.Test

class SuggestionAnalyzerTest {
    private val analyzer = SuggestionAnalyzer(CountryRepository(), homeDialCode = "47")

    private fun entry(number: String, type: CallType) = CallLogEntry(number, type)

    @Test fun suggestsForeignCodeWithOnlyUnansweredIncoming() {
        val log = listOf(
            entry("+447700900111", CallType.MISSED),
            entry("+447700900222", CallType.MISSED),
            entry("+447700900333", CallType.REJECTED),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions.map { it.dialCode }).containsExactly("44")
        assertThat(suggestions.first().callCount).isEqualTo(3)
        assertThat(suggestions.first().countryName).isEqualTo("United Kingdom")
    }

    @Test fun doesNotSuggestHomeCountry() {
        val log = listOf(
            entry("+4799887766", CallType.MISSED),
            entry("99887766", CallType.MISSED), // domestic, no prefix -> home
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions).isEmpty()
    }

    @Test fun doesNotSuggestCountryYouHaveARelationshipWith() {
        // Sweden: you have an OUTGOING call -> you talk to them -> not spam
        val log = listOf(
            entry("+46701111111", CallType.MISSED),
            entry("+46701111111", CallType.OUTGOING),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions.map { it.dialCode }).doesNotContain("46")
    }

    @Test fun doesNotSuggestWhenAnsweredCallsAreComparable() {
        // A few answered + a few unanswered: spam does NOT strongly outweigh → don't suggest.
        val log = listOf(
            entry("+34611111111", CallType.INCOMING_ANSWERED),
            entry("+34622222222", CallType.MISSED),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions.map { it.dialCode }).doesNotContain("34")
    }

    @Test fun suggestsWhenSpamStronglyOutweighsOldAnsweredCalls() {
        // Real UK case: a couple of old answered calls, but many recent missed/rejected.
        // unanswered (5) >= 3x answered (1) and >= 2 → suggest.
        val log = listOf(
            entry("+442011110000", CallType.INCOMING_ANSWERED),
            entry("+447700900111", CallType.MISSED),
            entry("+447700900222", CallType.REJECTED),
            entry("+447700900333", CallType.REJECTED),
            entry("+447700900444", CallType.MISSED),
            entry("+447700900555", CallType.REJECTED),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions.map { it.dialCode }).contains("44")
    }

    @Test fun outgoingCallAlwaysVetoesSuggestion() {
        // If you've ever CALLED them, never suggest — even with lots of unanswered incoming.
        val log = listOf(
            entry("+34611111111", CallType.OUTGOING),
            entry("+34622222222", CallType.MISSED),
            entry("+34633333333", CallType.REJECTED),
            entry("+34644444444", CallType.REJECTED),
            entry("+34655555555", CallType.MISSED),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions.map { it.dialCode }).doesNotContain("34")
    }

    @Test fun doesNotSuggestAlreadyBlockedCode() {
        val log = listOf(entry("+447700900111", CallType.MISSED))
        val suggestions = analyzer.analyze(log, alreadyBlocked = setOf("44"))
        assertThat(suggestions).isEmpty()
    }

    @Test fun ordersByCallCountDescending() {
        val log = listOf(
            entry("+447700900111", CallType.MISSED),
            entry("+447700900999", CallType.REJECTED),
            entry("+33611111111", CallType.MISSED),
            entry("+33622222222", CallType.MISSED),
            entry("+33633333333", CallType.REJECTED),
        )
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        // France (3) before UK (2)
        assertThat(suggestions.map { it.dialCode }).containsExactly("33", "44").inOrder()
    }

    @Test fun ignoresUnresolvableForeignNumbers() {
        val log = listOf(entry("+9991234567", CallType.MISSED)) // no known code
        val suggestions = analyzer.analyze(log, alreadyBlocked = emptySet())
        assertThat(suggestions).isEmpty()
    }
}
