package no.netspire.steintroll.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {
    @Insert suspend fun insert(call: BlockedCall): Long

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedCall>>

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun observeCount(): Flow<Int>

    @Delete suspend fun delete(call: BlockedCall)

    @Query("DELETE FROM blocked_calls")
    suspend fun clearAll()
}
