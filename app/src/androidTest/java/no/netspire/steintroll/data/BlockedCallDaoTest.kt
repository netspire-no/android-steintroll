package no.netspire.steintroll.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedCallDaoTest {
    private lateinit var db: SteintrollDatabase
    private lateinit var dao: BlockedCallDao

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SteintrollDatabase::class.java).build()
        dao = db.blockedCallDao()
    }

    @After fun teardown() = db.close()

    @Test fun insertAndObserve() = runTest {
        dao.insert(BlockedCall(rawNumber = "+447700900123", dialCode = "44",
            countryName = "United Kingdom", flag = "🇬🇧", timestamp = 1000,
            wasWithheld = false))
        val all = dao.observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all.first().dialCode).isEqualTo("44")
        assertThat(dao.observeCount().first()).isEqualTo(1)
    }
}
