package no.netspire.steintroll.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private val repo = SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test fun defaultsAreBlocklistEmptyWithheldOff() = runTest {
        val s = repo.settings.first()
        assertThat(s.mode).isEqualTo(Mode.BLOCKLIST)
        assertThat(s.blockCodes).isEmpty()
        assertThat(s.allowCodes).isEmpty()
        assertThat(s.blockWithheld).isFalse()
    }

    @Test fun addAndRemoveCodeTargetsCurrentModeList() = runTest {
        // Default mode is BLOCKLIST -> add goes to blockCodes, not allowCodes.
        repo.setMode(Mode.BLOCKLIST)
        repo.addCodeForCurrentMode("44")
        val afterAdd = repo.settings.first()
        assertThat(afterAdd.blockCodes).contains("44")
        assertThat(afterAdd.allowCodes).doesNotContain("44")
        repo.removeCodeForCurrentMode("44")
        assertThat(repo.settings.first().blockCodes).doesNotContain("44")
    }
}
