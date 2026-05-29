package no.netspire.steintroll.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedCall::class], version = 2, exportSchema = false)
abstract class SteintrollDatabase : RoomDatabase() {
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        @Volatile private var instance: SteintrollDatabase? = null
        fun get(context: Context): SteintrollDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, SteintrollDatabase::class.java, "steintroll.db"
                )
                    // Blocked-call log is disposable; drop on schema change rather than migrate.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
