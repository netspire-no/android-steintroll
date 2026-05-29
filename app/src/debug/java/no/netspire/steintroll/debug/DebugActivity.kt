package no.netspire.steintroll.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.netspire.steintroll.SteintrollApp
import no.netspire.steintroll.data.Mode
import no.netspire.steintroll.ui.role.RoleManagerHelper

/**
 * DEBUG-ONLY developer console for Steintroll. A terminal-styled control panel that
 * exposes every state and action we test: screening role, test PhoneAccount, mode,
 * block/allow lists, withheld toggle, and synthetic call injection — plus a live log.
 * Debug builds only (src/debug); never shipped.
 */
class DebugActivity : ComponentActivity() {

    private val app get() = application as SteintrollApp

    // --- terminal palette ---
    private val bg = Color(0xFF0A0E14)
    private val panel = Color(0xFF0F141C)
    private val line = Color(0xFF1E2630)
    private val green = Color(0xFF3DDC84)
    private val amber = Color(0xFFF7B733)
    private val red = Color(0xFFFF5C5C)
    private val cyan = Color(0xFF38BDF8)
    private val dim = Color(0xFF6B7686)
    private val fg = Color(0xFFD7E0EA)
    private val mono = FontFamily.Monospace

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { Console() }
    }

    @Composable
    private fun Console() {
        // live state
        var roleHeld by remember { mutableStateOf(false) }
        var accountRegistered by remember { mutableStateOf(false) }
        var mode by remember { mutableStateOf(Mode.BLOCKLIST) }
        var blockCodes by remember { mutableStateOf(emptySet<String>()) }
        var allowCodes by remember { mutableStateOf(emptySet<String>()) }
        var blockWithheld by remember { mutableStateOf(false) }
        var number by remember { mutableStateOf(TextFieldValue("+447700900123")) }
        var codeField by remember { mutableStateOf(TextFieldValue("44")) }
        val logLines = remember { mutableStateListOf<Pair<Color, String>>() }

        fun log(c: Color, s: String) { logLines.add(0, c to s); if (logLines.size > 60) logLines.removeAt(logLines.size - 1) }

        suspend fun refresh() {
            roleHeld = RoleManagerHelper.isScreeningRoleHeld(this@DebugActivity)
            accountRegistered = TestCallHarness.isAccountRegistered(this@DebugActivity)
            val s = app.settingsRepository.settings.first()
            mode = s.mode; blockCodes = s.blockCodes; allowCodes = s.allowCodes; blockWithheld = s.blockWithheld
        }

        LaunchedEffect(Unit) { refresh(); log(dim, "console ready — steintroll debug @ ${packageName}") }

        Box(Modifier.fillMaxSize().background(bg)) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ---- header ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("steintroll", color = fg, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("::debug", color = green, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    Badge("DEV", red)
                }

                // ---- QUICK CALLS (fast path, up top) ----
                Panel("QUICK CALLS") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigBtn("🇬🇧\n+44", red, Modifier.weight(1f)) { TestCallHarness.injectIncomingCall(this@DebugActivity, "+447700900123"); log(amber, "→ inject +44") }
                        BigBtn("🇳🇴\n+47", green, Modifier.weight(1f)) { TestCallHarness.injectIncomingCall(this@DebugActivity, "+4799887766"); log(amber, "→ inject +47") }
                        BigBtn("🇸🇪\n+46", green, Modifier.weight(1f)) { TestCallHarness.injectIncomingCall(this@DebugActivity, "+46701234567"); log(amber, "→ inject +46") }
                        BigBtn("🔒\nhidden", amber, Modifier.weight(1f)) { TestCallHarness.injectWithheldCall(this@DebugActivity); log(amber, "→ inject withheld") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Field(number, { number = it }, "custom number", Modifier.weight(1f), KeyboardType.Phone)
                        Spacer(Modifier.width(8.dp))
                        MiniBtn("▶ fire", green) {
                            TestCallHarness.injectIncomingCall(this@DebugActivity, number.text.trim())
                            log(amber, "→ inject ${number.text.trim()}")
                        }
                    }
                }

                // ---- STATUS panel ----
                Panel("STATUS") {
                    StatusLine("screening role", roleHeld, if (roleHeld) "HELD" else "NOT HELD")
                    StatusLine("test account", accountRegistered, if (accountRegistered) "REGISTERED" else "MISSING")
                    KV("mode", mode.name, if (mode == Mode.BLOCKLIST) red else green)
                    KV("block_codes", if (blockCodes.isEmpty()) "—" else blockCodes.sorted().joinToString(","), red)
                    KV("allow_codes", if (allowCodes.isEmpty()) "—" else allowCodes.sorted().joinToString(","), green)
                    KV("block_withheld", if (blockWithheld) "ON" else "off", if (blockWithheld) amber else dim)
                }

                // ---- LOG panel (close to the action) ----
                Panel("LOG") {
                    if (logLines.isEmpty()) Text("—", color = dim, fontFamily = mono, fontSize = 12.sp)
                    logLines.forEach { (c, s) ->
                        Text(s, color = c, fontFamily = mono, fontSize = 11.5.sp, lineHeight = 15.sp)
                    }
                }

                // ===== ADVANCED (setup + settings) =====
                Text("─── advanced ───", color = dim, fontFamily = mono, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

                Panel("SETUP") {
                    Cmd("register test PhoneAccount") {
                        TestCallHarness.registerAccount(this@DebugActivity)
                        log(cyan, "$ register account → done (enable in Settings ▸ Calling accounts)")
                        lifecycleScope.launch { refresh() }
                    }
                    Cmd("request screening role") {
                        RoleManagerHelper.requestRoleIntent(this@DebugActivity)?.let { startActivity(it) }
                        log(cyan, "$ request role → system dialog")
                    }
                    Cmd("refresh status") { lifecycleScope.launch { refresh(); log(dim, "$ refresh → ok") } }
                }

                Panel("SETTINGS") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Toggle("BLOCKLIST", mode == Mode.BLOCKLIST, red, Modifier.weight(1f)) {
                            lifecycleScope.launch { app.settingsRepository.setMode(Mode.BLOCKLIST); refresh(); log(cyan, "$ mode = BLOCKLIST") }
                        }
                        Toggle("ALLOWLIST", mode == Mode.ALLOWLIST, green, Modifier.weight(1f)) {
                            lifecycleScope.launch { app.settingsRepository.setMode(Mode.ALLOWLIST); refresh(); log(cyan, "$ mode = ALLOWLIST") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("block_withheld", color = fg, fontFamily = mono, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(checked = blockWithheld, onCheckedChange = { v ->
                            lifecycleScope.launch { app.settingsRepository.setBlockWithheld(v); refresh(); log(cyan, "$ block_withheld = $v") }
                        }, colors = SwitchDefaults.colors(checkedTrackColor = amber))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Field(codeField, { codeField = it }, "code", Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        MiniBtn("+ add", green) {
                            val c = codeField.text.trim().removePrefix("+"); if (c.isNotEmpty())
                                lifecycleScope.launch { app.settingsRepository.addCodeForCurrentMode(c); refresh(); log(green, "$ add $c → ${mode.name}") }
                        }
                        Spacer(Modifier.width(6.dp))
                        MiniBtn("− rm", red) {
                            val c = codeField.text.trim().removePrefix("+"); if (c.isNotEmpty())
                                lifecycleScope.launch { app.settingsRepository.removeCodeForCurrentMode(c); refresh(); log(red, "$ rm $c → ${mode.name}") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ---------- styled building blocks ----------
    @Composable private fun Panel(title: String, content: @Composable () -> Unit) {
        Column(
            Modifier.fillMaxWidth().background(panel, RoundedCornerShape(10.dp))
                .border(1.dp, line, RoundedCornerShape(10.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("▸ $title", color = dim, fontFamily = mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }

    @Composable private fun StatusLine(label: String, ok: Boolean, value: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (ok) "●" else "○", color = if (ok) green else red, fontFamily = mono, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, color = fg, fontFamily = mono, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = if (ok) green else red, fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable private fun KV(k: String, v: String, vColor: Color) {
        Row {
            Text("  $k", color = dim, fontFamily = mono, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
            Text(v, color = vColor, fontFamily = mono, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable private fun Cmd(label: String, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick)
                .background(Color(0xFF131A24), RoundedCornerShape(8.dp))
                .border(1.dp, line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$", color = green, fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(label, color = fg, fontFamily = mono, fontSize = 13.sp)
        }
    }

    @Composable private fun MiniBtn(label: String, c: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Box(
            modifier.clickable(onClick = onClick).background(Color(0xFF131A24), RoundedCornerShape(8.dp))
                .border(1.dp, line, RoundedCornerShape(8.dp)).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(label, color = c, fontFamily = mono, fontSize = 13.sp) }
    }

    @Composable private fun BigBtn(label: String, c: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Box(
            modifier.clickable(onClick = onClick)
                .background(c.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                .border(1.dp, c.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label, color = c, fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    @Composable private fun Toggle(label: String, active: Boolean, c: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Box(
            modifier.clickable(onClick = onClick)
                .background(if (active) c.copy(alpha = 0.18f) else Color(0xFF131A24), RoundedCornerShape(8.dp))
                .border(1.dp, if (active) c else line, RoundedCornerShape(8.dp)).padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) { Text(label, color = if (active) c else dim, fontFamily = mono, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }

    @Composable private fun Field(
        value: TextFieldValue, onChange: (TextFieldValue) -> Unit, hint: String,
        modifier: Modifier = Modifier, kbd: KeyboardType = KeyboardType.Phone,
    ) {
        Box(
            modifier.background(Color(0xFF0A0E14), RoundedCornerShape(8.dp))
                .border(1.dp, line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            if (value.text.isEmpty()) Text(hint, color = dim, fontFamily = mono, fontSize = 14.sp)
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = TextStyle(color = green, fontFamily = mono, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(green),
                keyboardOptions = KeyboardOptions(keyboardType = kbd),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable private fun Badge(text: String, c: Color) {
        Box(Modifier.background(c, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(text, color = Color(0xFF0A0E14), fontFamily = mono, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}
