package no.netspire.steintroll.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.data.BlockedCall
import no.netspire.steintroll.data.BlockedCallDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(
    private val dao: BlockedCallDao,
) : ViewModel() {

    val calls: StateFlow<List<BlockedCall>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Remove a single entry from the in-app log. */
    fun delete(call: BlockedCall) = viewModelScope.launch { dao.delete(call) }

    fun clearAll() = viewModelScope.launch { dao.clearAll() }

    class Factory(private val app: SteintrollApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogViewModel(app.blockedCallDao) as T
    }
}
