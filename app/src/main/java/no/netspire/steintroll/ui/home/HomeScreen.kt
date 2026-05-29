package no.netspire.steintroll.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.netspire.steintroll.R
import no.netspire.steintroll.data.Country
import no.netspire.steintroll.data.Mode
import no.netspire.steintroll.suggest.CountrySuggestion

// Mode intent colors: blocking = negative (red), allowing = positive (green).
private val BlockRed = Color(0xFFDC2626)
private val AllowGreen = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    roleHeld: Boolean,
    onRequestRole: () -> Unit,
    onSetMode: (Mode) -> Unit,
    onRemoveCountry: (String) -> Unit,
    onSetBlockWithheld: (Boolean) -> Unit,
    onAddCountry: () -> Unit,
    onOpenLog: () -> Unit,
    onScanCallLog: () -> Unit,
    onAcceptSuggestion: (CountrySuggestion) -> Unit,
    onDismissSuggestion: (CountrySuggestion) -> Unit,
) {
    var showRoleSheet by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // Prominent header: large mark bleeding toward the left edge (past the
                // LazyColumn's 16dp padding) so the brand reads boldly.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = (-8).dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_troll_mark),
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Steintroll", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (!roleHeld) {
                item { RoleBanner(onEnable = { showRoleSheet = true }) }
            }

            item { StatCard(count = state.blockedCount, onClick = onOpenLog) }

            item { ModeToggle(mode = state.mode, onSetMode = onSetMode) }

            item {
                Text(
                    if (state.mode == Mode.BLOCKLIST) "BLOCKING" else "ALLOWING ONLY",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (state.mode == Mode.BLOCKLIST) BlockRed else AllowGreen,
                )
            }

            items(state.countries, key = { it.dialCode + it.iso }) { c ->
                CountryRow(c) { onRemoveCountry(c.dialCode) }
            }

            item {
                Button(
                    onClick = onAddCountry,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add country")
                }
            }

            // Spam suggestions from call history (blocklist mode only).
            if (state.mode == Mode.BLOCKLIST) {
                if (state.suggestions.isNotEmpty()) {
                    item {
                        Text(
                            "SUGGESTED TO BLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = BlockRed,
                        )
                    }
                    items(state.suggestions, key = { "sugg-" + it.dialCode }) { s ->
                        SuggestionRow(
                            suggestion = s,
                            onAccept = { onAcceptSuggestion(s) },
                            onDismiss = { onDismissSuggestion(s) },
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = onScanCallLog,
                        enabled = !state.scanning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.scanning) "Scanning call history…" else "Scan call history for spam")
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Block withheld / unknown", modifier = Modifier.weight(1f))
                        Switch(checked = state.blockWithheld, onCheckedChange = onSetBlockWithheld)
                    }
                }
            }
        }

        if (showRoleSheet) {
            RoleExplainerSheet(
                onContinue = {
                    showRoleSheet = false
                    onRequestRole()
                },
                onDismiss = { showRoleSheet = false },
            )
        }
    }
}

@Composable
private fun RoleBanner(onEnable: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Steintroll isn't active yet", fontWeight = FontWeight.Bold)
            Text("Grant the call-screening permission so Steintroll can block calls.", fontSize = 13.sp)
            Button(onClick = onEnable) { Text("Enable call blocking") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleExplainerSheet(onContinue: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Enable call blocking", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Android will now ask which app should screen your incoming calls.",
                fontSize = 15.sp,
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "On the next screen, choose “Steintroll”.\n\n" +
                        "Picking “Phone” or “None” will leave blocking off. " +
                        "You can change this anytime in the app.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Continue", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun StatCard(count: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary)
            Text("calls blocked — tap to view log", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ModeToggle(mode: Mode, onSetMode: (Mode) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp)) {
            SegItem("Block these", mode == Mode.BLOCKLIST, BlockRed, Modifier.weight(1f)) {
                onSetMode(Mode.BLOCKLIST)
            }
            SegItem("Allow only", mode == Mode.ALLOWLIST, AllowGreen, Modifier.weight(1f)) {
                onSetMode(Mode.ALLOWLIST)
            }
        }
    }
}

@Composable
private fun SegItem(
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (active) activeColor else MaterialTheme.colorScheme.surface,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (active) Color.White
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CountryRow(country: Country, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(country.flag, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(country.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("+${country.dialCode}", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove") }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: CountrySuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = BlockRed.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(suggestion.flag, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(suggestion.countryName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val times = if (suggestion.callCount == 1) "1 unanswered call"
                    else "${suggestion.callCount} unanswered calls"
                Text("+${suggestion.dialCode} · $times", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = BlockRed),
            ) { Text("Block") }
        }
    }
}
