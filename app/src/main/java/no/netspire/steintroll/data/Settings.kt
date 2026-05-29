package no.netspire.steintroll.data

enum class Mode { BLOCKLIST, ALLOWLIST }

data class Settings(
    val mode: Mode = Mode.BLOCKLIST,
    /** Codes to block (used in BLOCKLIST mode). */
    val blockCodes: Set<String> = emptySet(),
    /** Codes to allow exclusively (used in ALLOWLIST mode). */
    val allowCodes: Set<String> = emptySet(),
    val blockWithheld: Boolean = false,
) {
    /** The set of codes relevant to the current mode. */
    val activeCodes: Set<String>
        get() = if (mode == Mode.BLOCKLIST) blockCodes else allowCodes
}
