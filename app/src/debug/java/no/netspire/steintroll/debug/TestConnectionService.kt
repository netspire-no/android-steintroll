package no.netspire.steintroll.debug

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle

/**
 * DEBUG-ONLY. A minimal managed [ConnectionService] used purely to inject synthetic
 * incoming calls (via [TestCallHarness]) so we can exercise the real
 * CallScreeningService path on a physical device without placing an actual call.
 *
 * It must be a MANAGED account (CAPABILITY_CALL_PROVIDER). Self-managed calls are
 * deliberately exempted from call screening by the platform, so they would never
 * reach SteintrollCallScreeningService.
 */
class TestConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection {
        // Return a connection that immediately reports itself as ringing. The
        // screening verdict is applied by Telecom before/around this; if Steintroll
        // rejects the call it is torn down without ringing. If allowed, we don't
        // want a fake call lingering, so we self-disconnect shortly.
        return object : Connection() {
            init {
                setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
                setRinging()
            }

            override fun onAnswer() {
                // If a human (or the system) answers, just disconnect — this is a test call.
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }

            override fun onReject() {
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                destroy()
            }

            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }
        }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        // No-op: happens when the call is rejected by screening before a connection
        // is fully created. Nothing to clean up.
    }
}
