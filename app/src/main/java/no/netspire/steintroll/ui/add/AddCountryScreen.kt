package no.netspire.steintroll.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.netspire.steintroll.data.Country

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountryScreen(
    state: AddCountryUiState,
    onQueryChange: (String) -> Unit,
    onAdd: (Country) -> Unit,
    onRemove: (Country) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    if (state.mode == no.netspire.steintroll.data.Mode.BLOCKLIST)
                        "Add country to block" else "Add country to allow"
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Search +code (or scroll the list)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.results, key = { it.iso }) { c ->
                    val selected = c.dialCode in state.selectedCodes
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text("+${c.dialCode}") },
                        leadingContent = { Text(c.flag, fontSize = 26.sp) },
                        trailingContent = {
                            if (selected) Icon(Icons.Default.Check, "Added",
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable {
                            // Tap = add (or remove if already added) and return immediately.
                            if (selected) onRemove(c) else onAdd(c)
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
