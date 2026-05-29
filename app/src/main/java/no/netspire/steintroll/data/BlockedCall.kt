package no.netspire.steintroll.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawNumber: String?,        // null when withheld
    val dialCode: String?,         // resolved calling code, null if none/withheld
    val countryName: String?,      // resolved display name, null if unknown
    val flag: String?,             // resolved flag emoji, null if unknown
    val timestamp: Long,           // epoch millis
    val wasWithheld: Boolean,
)
