package no.netspire.steintroll.data

/**
 * Static, offline table of countries and their calling codes.
 * Not exhaustive of every territory, but covers the common set; add rows freely.
 * Dial codes are NOT unique (e.g. +1 = US/CA); that is expected — blocking is by code.
 */
class CountryRepository {
    val all: List<Country> = listOf(
        Country("NO", "Norway", "🇳🇴", "47"),
        Country("SE", "Sweden", "🇸🇪", "46"),
        Country("DK", "Denmark", "🇩🇰", "45"),
        Country("FI", "Finland", "🇫🇮", "358"),
        Country("GB", "United Kingdom", "🇬🇧", "44"),
        Country("IE", "Ireland", "🇮🇪", "353"),
        Country("NL", "Netherlands", "🇳🇱", "31"),
        Country("DE", "Germany", "🇩🇪", "49"),
        Country("FR", "France", "🇫🇷", "33"),
        Country("ES", "Spain", "🇪🇸", "34"),
        Country("PT", "Portugal", "🇵🇹", "351"),
        Country("IT", "Italy", "🇮🇹", "39"),
        Country("BE", "Belgium", "🇧🇪", "32"),
        Country("PL", "Poland", "🇵🇱", "48"),
        Country("US", "United States", "🇺🇸", "1"),
        Country("CA", "Canada", "🇨🇦", "1"),
        Country("IN", "India", "🇮🇳", "91"),
        Country("CN", "China", "🇨🇳", "86"),
        Country("RU", "Russia", "🇷🇺", "7"),
        Country("PK", "Pakistan", "🇵🇰", "92"),
        Country("NG", "Nigeria", "🇳🇬", "234"),
        Country("PH", "Philippines", "🇵🇭", "63"),
        Country("AU", "Australia", "🇦🇺", "61"),
        Country("CH", "Switzerland", "🇨🇭", "41"),
        Country("AT", "Austria", "🇦🇹", "43"),
    )

    /** First country matching a dial code (codes aren't unique). */
    fun byDialCode(code: String): Country? = all.firstOrNull { it.dialCode == code }

    /** All distinct dial codes present in the table, longest first (for prefix matching). */
    val dialCodesLongestFirst: List<String> =
        all.map { it.dialCode }.distinct().sortedByDescending { it.length }

    fun search(query: String): List<Country> {
        val q = query.trim().removePrefix("+").lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.lowercase().contains(q) || it.dialCode.startsWith(q) || it.iso.lowercase() == q
        }
    }
}
