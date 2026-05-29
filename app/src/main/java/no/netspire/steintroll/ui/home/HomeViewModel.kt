package no.netspire.steintroll.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.data.BlockedCallDao
import no.netspire.steintroll.data.Country
import no.netspire.steintroll.data.CountryRepository
import no.netspire.steintroll.data.Mode
import no.netspire.steintroll.data.Settings
import no.netspire.steintroll.data.SettingsRepository
import no.netspire.steintroll.suggest.CallLogReader
import no.netspire.steintroll.suggest.CountrySuggestion
import no.netspire.steintroll.suggest.SuggestionAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val mode: Mode = Mode.BLOCKLIST,
    val countries: List<Country> = emptyList(),
    val blockWithheld: Boolean = false,
    val blockedCount: Int = 0,
    val suggestions: List<CountrySuggestion> = emptyList(),
    val scanning: Boolean = false,
)

class HomeViewModel(
    private val settingsRepo: SettingsRepository,
    private val countryRepo: CountryRepository,
    private val callLogReader: CallLogReader,
    private val homeDialCode: String,
    blockedDao: BlockedCallDao,
) : ViewModel() {

    private val suggestions = MutableStateFlow<List<CountrySuggestion>>(emptyList())
    private val scanning = MutableStateFlow(false)
    // Codes the user dismissed this session — don't re-suggest until next app launch.
    private val dismissed = mutableSetOf<String>()

    val uiState: StateFlow<HomeUiState> =
        combine(
            settingsRepo.settings, blockedDao.observeCount(), suggestions, scanning,
        ) { s: Settings, count: Int, suggs: List<CountrySuggestion>, isScanning: Boolean ->
            HomeUiState(
                mode = s.mode,
                countries = s.activeCodes.mapNotNull { countryRepo.byDialCode(it) }
                    .sortedBy { it.name },
                blockWithheld = s.blockWithheld,
                blockedCount = count,
                // Only surface suggestions in blocklist mode, and never ones already on the block-list.
                suggestions = if (s.mode == Mode.BLOCKLIST)
                    suggs.filter { it.dialCode !in s.blockCodes && it.dialCode !in dismissed }
                else emptyList(),
                scanning = isScanning,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setMode(mode: Mode) = viewModelScope.launch { settingsRepo.setMode(mode) }
    fun removeCountry(dialCode: String) =
        viewModelScope.launch { settingsRepo.removeCodeForCurrentMode(dialCode) }
    fun setBlockWithheld(value: Boolean) = viewModelScope.launch { settingsRepo.setBlockWithheld(value) }

    /** Call only after READ_CALL_LOG is granted. Reads + analyzes the call log off-main. */
    fun scanCallLog() = viewModelScope.launch {
        scanning.value = true
        val blocked = settingsRepo.settings.first().blockCodes
        val result = withContext(Dispatchers.IO) {
            val analyzer = SuggestionAnalyzer(countryRepo, homeDialCode)
            analyzer.analyze(callLogReader.read(), alreadyBlocked = blocked)
        }
        suggestions.value = result
        scanning.value = false
    }

    fun acceptSuggestion(suggestion: CountrySuggestion) = viewModelScope.launch {
        // Suggestions only appear in blocklist mode, so this adds to the block-list.
        settingsRepo.addCodeForCurrentMode(suggestion.dialCode)
    }

    fun dismissSuggestion(suggestion: CountrySuggestion) {
        dismissed += suggestion.dialCode
        suggestions.value = suggestions.value.filter { it.dialCode != suggestion.dialCode }
    }

    class Factory(private val app: SteintrollApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(
                app.settingsRepository,
                app.countryRepository,
                CallLogReader(app),
                app.deviceHomeDialCode(),
                app.blockedCallDao,
            ) as T
    }
}
