package no.netspire.steintroll.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import no.netspire.steintroll.data.BlockedCall
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    calls: List<BlockedCall>,
    onDelete: (BlockedCall) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Blocked log") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            actions = {
                if (calls.isNotEmpty()) TextButton(onClick = onClearAll) { Text("Clear") }
            },
        )
    }) { padding ->
        if (calls.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No calls blocked yet ☀️",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(calls, key = { it.id }) { call ->
                    ListItem(
                        headlineContent = {
                            Text(call.rawNumber ?: "Withheld number", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            val country = call.countryName?.let { " · $it" } ?: ""
                            Text(formatTime(call.timestamp) + country, fontSize = 12.sp)
                        },
                        leadingContent = { Text(call.flag ?: "🚫", fontSize = 24.sp) },
                        trailingContent = {
                            TextButton(onClick = { onDelete(call) }) { Text("Delete") }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts))
