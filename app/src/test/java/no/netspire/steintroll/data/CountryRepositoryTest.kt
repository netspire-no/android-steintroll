package no.netspire.steintroll.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryRepositoryTest {
    private val repo = CountryRepository()

    @Test fun findsUkByDialCode() {
        val uk = repo.byDialCode("44")
        assertThat(uk).isNotNull()
        assertThat(uk!!.name).isEqualTo("United Kingdom")
        assertThat(uk.flag).isEqualTo("🇬🇧")
    }

    @Test fun searchMatchesNameCaseInsensitive() {
        val results = repo.search("norw")
        assertThat(results.map { it.dialCode }).contains("47")
    }

    @Test fun searchMatchesDialCodeWithOrWithoutPlus() {
        assertThat(repo.search("+47").map { it.name }).contains("Norway")
        assertThat(repo.search("47").map { it.name }).contains("Norway")
    }

    @Test fun allCountriesHaveNonEmptyFields() {
        assertThat(repo.all).isNotEmpty()
        repo.all.forEach {
            assertThat(it.iso).isNotEmpty()
            assertThat(it.name).isNotEmpty()
            assertThat(it.flag).isNotEmpty()
            assertThat(it.dialCode).isNotEmpty()
        }
    }
}
