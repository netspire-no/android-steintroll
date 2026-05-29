package no.netspire.steintroll.suggest

import android.content.Context
import android.provider.CallLog
import android.util.Log

/**
 * Reads the device call log into [CallLogEntry]s. Requires READ_CALL_LOG (a runtime
 * permission) — callers must ensure it is granted first. All on-device; no network.
 */
class CallLogReader(private val context: Context) {

    /** Reads up to [limit] most-recent call-log rows. Returns empty on any failure. */
    fun read(limit: Int = 2000): List<CallLogEntry> {
        val out = ArrayList<CallLogEntry>()
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                val numberIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                while (c.moveToNext() && out.size < limit) {
                    val number = c.getString(numberIdx)
                    out += CallLogEntry(number, mapType(c.getInt(typeIdx)))
                }
            }
        } catch (e: SecurityException) {
            Log.w("Steintroll", "READ_CALL_LOG not granted: ${e.message}")
        } catch (e: Exception) {
            Log.w("Steintroll", "call log read failed: ${e.message}")
        }
        return out
    }

    private fun mapType(type: Int): CallType = when (type) {
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING_ANSWERED
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
        else -> CallType.OTHER
    }
}
