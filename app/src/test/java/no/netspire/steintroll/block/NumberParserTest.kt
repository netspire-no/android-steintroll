package no.netspire.steintroll.block

import com.google.common.truth.Truth.assertThat
import no.netspire.steintroll.data.CountryRepository
import org.junit.Test

class NumberParserTest {
    private val parser = NumberParser(CountryRepository(), homeDialCode = "47")

    @Test fun resolvesUkFromE164() {
        val r = parser.parse("+447700900123", isWithheld = false)
        assertThat(r).isInstanceOf(ParsedNumber.Resolved::class.java)
        assertThat((r as ParsedNumber.Resolved).dialCode).isEqualTo("44")
    }

    @Test fun resolvesLongerCodeOverShorter() {
        // +351 (Portugal) must win over +35; +1 must not greedily eat +1-region
        val r = parser.parse("+351912345678", isWithheld = false)
        assertThat((r as ParsedNumber.Resolved).dialCode).isEqualTo("351")
    }

    @Test fun resolvesWith00Prefix() {
        val r = parser.parse("00447700900123", isWithheld = false)
        assertThat((r as ParsedNumber.Resolved).dialCode).isEqualTo("44")
    }

    @Test fun domesticNumberNoPrefixTreatedAsHomeCountry() {
        // Norwegian local 8-digit number, no country code
        val r = parser.parse("99887766", isWithheld = false)
        assertThat((r as ParsedNumber.Resolved).dialCode).isEqualTo("47")
    }

    @Test fun unknownPrefixWithNoMatchIsNoCountryCode() {
        val r = parser.parse("+9991234567", isWithheld = false)
        assertThat(r).isEqualTo(ParsedNumber.NoCountryCode)
    }

    @Test fun withheldFlagAlwaysWithheld() {
        assertThat(parser.parse(null, isWithheld = true)).isEqualTo(ParsedNumber.Withheld)
        assertThat(parser.parse("", isWithheld = true)).isEqualTo(ParsedNumber.Withheld)
    }

    @Test fun nullNumberIsWithheld() {
        assertThat(parser.parse(null, isWithheld = false)).isEqualTo(ParsedNumber.Withheld)
    }
}
