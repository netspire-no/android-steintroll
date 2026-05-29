package no.netspire.steintroll.debug

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * DEBUG-ONLY test harness. Registers a managed PhoneAccount backed by
 * [TestConnectionService] and injects synthetic incoming calls so the real
 * SteintrollCallScreeningService.onScreenCall path runs on a physical device
 * with no actual phone call.
 */
object TestCallHarness {
    private const val TAG = "SteintrollTest"
    private const val ACCOUNT_ID = "steintroll-screening-test"

    fun handle(context: Context): PhoneAccountHandle =
        PhoneAccountHandle(
            ComponentName(context, TestConnectionService::class.java),
            ACCOUNT_ID,
        )

    /** Register (idempotent) the managed calling account used for test calls. */
    fun registerAccount(context: Context): PhoneAccountHandle {
        val tm = context.getSystemService(TelecomManager::class.java)
        val handle = handle(context)
        val account = PhoneAccount.builder(handle, "Steintroll Test")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER) // MANAGED -> gets screened
            .build()
        tm.registerPhoneAccount(account)
        Log.i(TAG, "Registered test PhoneAccount $ACCOUNT_ID. Enable it in Settings > " +
            "Calling accounts, or via: adb shell cmd telecom set-phone-account-enabled " +
            "${context.packageName}/${TestConnectionService::class.java.name} $ACCOUNT_ID 0")
        return handle
    }

    /** True if our managed test PhoneAccount is currently registered with Telecom. */
    fun isAccountRegistered(context: Context): Boolean = try {
        val tm = context.getSystemService(TelecomManager::class.java)
        tm?.getPhoneAccount(handle(context)) != null
    } catch (e: Exception) {
        false
    }

    /**
     * Inject a synthetic incoming call from [number]. Requires the test account to be
     * registered (see [registerAccount]) and enabled, and Steintroll to be the active
     * call-screening app.
     */
    fun injectIncomingCall(context: Context, number: String) {
        val tm = context.getSystemService(TelecomManager::class.java)
        val handle = handle(context)
        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                Uri.fromParts("tel", number, null),
            )
        }
        try {
            tm.addNewIncomingCall(handle, extras)
            Log.i(TAG, "Injected test incoming call from $number")
        } catch (e: SecurityException) {
            Log.e(TAG, "addNewIncomingCall failed — is the test account registered & enabled? ${e.message}")
        }
    }

    /**
     * Inject a synthetic incoming call with NO caller address (withheld / private number).
     * Steintroll should treat this as [ParsedNumber.Withheld] and apply the blockWithheld toggle.
     */
    fun injectWithheldCall(context: Context) {
        val tm = context.getSystemService(TelecomManager::class.java)
        val handle = handle(context)
        // No EXTRA_INCOMING_CALL_ADDRESS at all -> the call has no handle -> withheld.
        try {
            tm.addNewIncomingCall(handle, Bundle())
            Log.i(TAG, "Injected withheld test incoming call (no address)")
        } catch (e: SecurityException) {
            Log.e(TAG, "addNewIncomingCall (withheld) failed — account registered & enabled? ${e.message}")
        }
    }
}
