package no.netspire.steintroll.block

import com.google.common.truth.Truth.assertThat
import no.netspire.steintroll.data.Mode
import no.netspire.steintroll.data.Settings
import org.junit.Test

class BlockDecisionTest {
    private val uk = ParsedNumber.Resolved("44", "7700900123")
    private val no = ParsedNumber.Resolved("47", "99887766")

    private fun settings(mode: Mode, codes: Set<String>, withheld: Boolean = false) =
        if (mode == Mode.BLOCKLIST)
            Settings(mode = mode, blockCodes = codes, blockWithheld = withheld)
        else
            Settings(mode = mode, allowCodes = codes, blockWithheld = withheld)

    // --- Blocklist mode ---
    @Test fun blocklist_blocksListedCode() {
        val d = BlockDecision.decide(uk, settings(Mode.BLOCKLIST, setOf("44")))
        assertThat(d).isInstanceOf(Decision.Block::class.java)
    }

    @Test fun blocklist_allowsUnlistedCode() {
        val d = BlockDecision.decide(no, settings(Mode.BLOCKLIST, setOf("44")))
        assertThat(d).isEqualTo(Decision.Allow)
    }

    // --- Allowlist mode ---
    @Test fun allowlist_allowsListedCode() {
        val d = BlockDecision.decide(no, settings(Mode.ALLOWLIST, setOf("47", "46")))
        assertThat(d).isEqualTo(Decision.Allow)
    }

    @Test fun allowlist_blocksUnlistedCode() {
        val d = BlockDecision.decide(uk, settings(Mode.ALLOWLIST, setOf("47", "46")))
        assertThat(d).isInstanceOf(Decision.Block::class.java)
    }

    // --- Withheld is decided ONLY by the toggle, regardless of mode ---
    @Test fun withheld_blockedWhenToggleOn_blocklist() {
        val d = BlockDecision.decide(ParsedNumber.Withheld,
            settings(Mode.BLOCKLIST, setOf("44"), withheld = true))
        assertThat(d).isInstanceOf(Decision.Block::class.java)
    }

    @Test fun withheld_allowedWhenToggleOff_blocklist() {
        val d = BlockDecision.decide(ParsedNumber.Withheld,
            settings(Mode.BLOCKLIST, setOf("44"), withheld = false))
        assertThat(d).isEqualTo(Decision.Allow)
    }

    @Test fun withheld_blockedWhenToggleOn_allowlist() {
        val d = BlockDecision.decide(ParsedNumber.Withheld,
            settings(Mode.ALLOWLIST, setOf("47"), withheld = true))
        assertThat(d).isInstanceOf(Decision.Block::class.java)
    }

    // --- NoCountryCode: domestic-ish. Blocklist allows; Allowlist blocks (not an allowed code) ---
    @Test fun noCountryCode_allowedInBlocklist() {
        val d = BlockDecision.decide(ParsedNumber.NoCountryCode, settings(Mode.BLOCKLIST, setOf("44")))
        assertThat(d).isEqualTo(Decision.Allow)
    }

    @Test fun noCountryCode_blockedInAllowlist() {
        val d = BlockDecision.decide(ParsedNumber.NoCountryCode, settings(Mode.ALLOWLIST, setOf("47")))
        assertThat(d).isInstanceOf(Decision.Block::class.java)
    }

    // The lists are independent: a code in the block-list must NOT leak into allow-mode.
    @Test fun blockListAndAllowListAreIndependent() {
        // UK is in the block-list; allow-list contains only Norway. In ALLOWLIST mode,
        // UK must be blocked (not allowed) because it's not in the allow-list.
        val s = Settings(
            mode = Mode.ALLOWLIST,
            blockCodes = setOf("44"),
            allowCodes = setOf("47"),
        )
        assertThat(BlockDecision.decide(uk, s)).isInstanceOf(Decision.Block::class.java)
        assertThat(BlockDecision.decide(no, s)).isEqualTo(Decision.Allow)
    }
}
