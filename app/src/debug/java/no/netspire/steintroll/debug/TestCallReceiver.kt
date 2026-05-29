package no.netspire.steintroll.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * DEBUG-ONLY. Lets us drive the test harness from adb, e.g.:
 *
 *   # one-time: register the managed test account
 *   adb shell am broadcast -a no.netspire.steintroll.TEST_REGISTER \
 *       -n no.netspire.steintroll/.debug.TestCallReceiver
 *
 *   # fire a synthetic incoming call from a chosen number
 *   adb shell am broadcast -a no.netspire.steintroll.TEST_CALL \
 *       -n no.netspire.steintroll/.debug.TestCallReceiver --es number "+447700900123"
 *
 *   # fire a withheld / private-number call (no caller address)
 *   adb shell am broadcast -a no.netspire.steintroll.TEST_WITHHELD \
 *       -n no.netspire.steintroll/.debug.TestCallReceiver
 */
class TestCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REGISTER -> TestCallHarness.registerAccount(context)
            ACTION_CALL -> {
                val number = intent.getStringExtra("number") ?: "+447700900123"
                TestCallHarness.injectIncomingCall(context, number)
            }
            ACTION_WITHHELD -> TestCallHarness.injectWithheldCall(context)
        }
    }

    companion object {
        const val ACTION_REGISTER = "no.netspire.steintroll.TEST_REGISTER"
        const val ACTION_CALL = "no.netspire.steintroll.TEST_CALL"
        const val ACTION_WITHHELD = "no.netspire.steintroll.TEST_WITHHELD"
    }
}
