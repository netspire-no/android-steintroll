package no.netspire.steintroll.data

data class Country(
    val iso: String,      // ISO 3166-1 alpha-2, e.g. "GB"
    val name: String,     // display name, e.g. "United Kingdom"
    val flag: String,     // emoji flag, e.g. "🇬🇧"
    val dialCode: String, // calling code without '+', e.g. "44"
)
