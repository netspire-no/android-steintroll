package no.netspire.steintroll.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.data.Country
import no.netspire.steintroll.data.CountryRepository
import no.netspire.steintroll.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddCountryUiState(
    val query: String = "",
    val results: List<Country> = emptyList(),
    val selectedCodes: Set<String> = emptySet(),
    val mode: no.netspire.steintroll.data.Mode = no.netspire.steintroll.data.Mode.BLOCKLIST,
)

class AddCountryViewModel(
    private val settingsRepo: SettingsRepository,
    private val countryRepo: CountryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    fun onQueryChange(q: String) { query.value = q }

    val uiState: StateFlow<AddCountryUiState> =
        combine(query, settingsRepo.settings) { q, s ->
            AddCountryUiState(
                query = q,
                results = countryRepo.search(q),
                selectedCodes = s.activeCodes,
                mode = s.mode,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AddCountryUiState(results = countryRepo.all))

    fun add(country: Country) =
        viewModelScope.launch { settingsRepo.addCodeForCurrentMode(country.dialCode) }
    fun remove(country: Country) =
        viewModelScope.launch { settingsRepo.removeCodeForCurrentMode(country.dialCode) }

    class Factory(private val app: SteintrollApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddCountryViewModel(app.settingsRepository, app.countryRepository) as T
    }
}
