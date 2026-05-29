package no.netspire.steintroll.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.ui.add.AddCountryScreen
import no.netspire.steintroll.ui.add.AddCountryViewModel
import no.netspire.steintroll.ui.home.HomeScreen
import no.netspire.steintroll.ui.home.HomeViewModel
import no.netspire.steintroll.ui.log.LogScreen
import no.netspire.steintroll.ui.log.LogViewModel
import no.netspire.steintroll.ui.role.RoleManagerHelper

@Composable
fun SteintrollNav() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SteintrollApp

    // Role state, refreshed when returning from the role request.
    var roleHeld by remember { mutableStateOf(RoleManagerHelper.isScreeningRoleHeld(context)) }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { roleHeld = RoleManagerHelper.isScreeningRoleHeld(context) }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app))
            val state by vm.uiState.collectAsState()
            // Re-check role on each entry.
            LaunchedEffect(Unit) { roleHeld = RoleManagerHelper.isScreeningRoleHeld(context) }

            // READ_CALL_LOG runtime permission; scan once granted.
            val callLogLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) vm.scanCallLog() }

            HomeScreen(
                state = state,
                roleHeld = roleHeld,
                onRequestRole = {
                    RoleManagerHelper.requestRoleIntent(context)?.let { roleLauncher.launch(it) }
                },
                onSetMode = vm::setMode,
                onRemoveCountry = vm::removeCountry,
                onSetBlockWithheld = vm::setBlockWithheld,
                onAddCountry = { nav.navigate("add") },
                onOpenLog = { nav.navigate("log") },
                onScanCallLog = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        vm.scanCallLog()
                    } else {
                        callLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                },
                onAcceptSuggestion = vm::acceptSuggestion,
                onDismissSuggestion = vm::dismissSuggestion,
            )
        }
        composable("add") {
            val vm: AddCountryViewModel = viewModel(factory = AddCountryViewModel.Factory(app))
            val state by vm.uiState.collectAsState()
            AddCountryScreen(
                state = state,
                onQueryChange = vm::onQueryChange,
                onAdd = { country ->
                    vm.add(country)
                    nav.popBackStack() // tap = added + return straight to Home
                },
                onRemove = vm::remove,
                onBack = { nav.popBackStack() },
            )
        }
        composable("log") {
            val vm: LogViewModel = viewModel(factory = LogViewModel.Factory(app))
            val calls by vm.calls.collectAsState()
            LogScreen(
                calls = calls,
                onDelete = vm::delete,
                onClearAll = vm::clearAll,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
