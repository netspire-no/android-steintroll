# Steintroll — Design

This document explains how Steintroll is put together and the reasoning behind the
main decisions. For what it does and how to build it, see the
[README](../../README.md).

## Goal

Silently reject incoming calls by **country calling code**, with no ring, no
vibration, and no missed-call notification — so that spam from a country that rotates
through many phone numbers can be stopped with a single rule, while legitimate calls
get through. Everything runs on-device; the app is never the default dialer.

## Core mechanism: CallScreeningService

Android offers two ways to block calls:

1. **`CallScreeningService`** — the app is notified of each incoming call *before it
   rings* and responds with a `CallResponse` that can disallow the call, reject it,
   and skip the notification and call-log entry. It works without becoming the default
   dialer; it only needs the user to grant the **call-screening role**.
2. **Replacing the default Phone/dialer app** — far more invasive.

Steintroll uses (1). Silent rejection with notification suppression
(`setDisallowCall` / `setRejectCall` / `setSkipNotification` / `setSkipCallLog`) is
available from **API 29 (Android 10)**, which sets the minimum SDK.

Because it's a system-bound service, Android starts it on demand for each incoming
call — so blocking works even when the app's UI is closed.

## Architecture

A single-module app in three layers.

### 1. Call-screening (the workhorse)

`SteintrollCallScreeningService` receives each incoming call, extracts the caller
number, and resolves its country calling code by **longest-prefix match** against a
bundled table (so multi-digit codes like `+44`, `+47`, `+351` resolve correctly). It
then calls a pure decision function and either allows the call or rejects it silently
and writes a record to the on-device log.

The decision is a pure, dependency-free function:

```
decide(number, settings) -> Allow | Block(reason)
```

The number is first classified as exactly one of:

- **Withheld** — no caller ID (private/restricted). Decided solely by the
  *block-withheld* toggle, independent of mode.
- **NoCountryCode** — a number with no resolvable international prefix (typically a
  domestic call). Allowed in blocklist mode; treated as not-allowed in allowlist mode.
- **Resolved(countryCode)** — blocklist mode blocks it if the code is on the block
  list; allowlist mode blocks it if the code is *not* on the allow list.

Keeping this logic free of Android types means it is fully covered by fast unit tests.

### 2. Data

- **Room** stores the blocked-call log (number, country, timestamp, withheld flag).
- **DataStore** stores settings: mode, the block list, the allow list, and the
  block-withheld toggle.
- A bundled, offline **country table** (ISO code, flag, calling code, name) powers the
  picker and resolves codes to names/flags for display.

The block list and allow list are kept as **separate sets**. Each mode reads and edits
only its own list, so switching modes can never accidentally reinterpret one list as
the other (e.g. flipping to "allow only" with a block entry must not suddenly block
everyone).

### 3. UI (Jetpack Compose, Material 3)

Three screens: **Home** (mode toggle, the active list, add-country, the blocked-count
stat that opens the log, the withheld toggle, and call-history spam suggestions),
**Add country** (searchable picker), and **Log** (reverse-chronological blocked
calls). A first-run banner guides the user through granting the call-screening role.

## Modes

- **Blocklist** (default): allow everything except listed country codes.
- **Allowlist**: block everything except listed country codes — including the home
  country unless it's explicitly added. Useful when calls should only come from a few
  known countries.

## Spam suggestions

On request (and only after the user grants `READ_CALL_LOG`), Steintroll analyses the
call log on-device and suggests foreign country codes that look like spam: codes you
have **no relationship with** (you never called them and rarely/never answered them)
but that keep calling. An outgoing call to a code always disqualifies it from being
suggested; answered calls are a weak signal, so a code that was once legitimate but now
floods you with unanswered calls can still surface. Nothing is uploaded.

## Privacy & non-goals

- **Fully offline** — no network, accounts, or analytics.
- **Not the default dialer** — only the call-screening role.
- **No SMS blocking, no per-number management UI, no contact-based rules** — the
  country-code rule is the whole idea, kept deliberately small.

## Error handling

Blocking always takes priority over bookkeeping: if logging fails, the call is still
rejected. Unparseable or null numbers are treated as withheld. The screening callback
never crashes the call flow.

## Testing

The country-code resolver and the block-decision function are covered by JVM unit
tests (a full truth table across modes × number classes × the withheld toggle). Room
and DataStore have instrumented tests. The decision logic — the part that determines
whether your phone rings — is therefore verifiable without a device.

## Stack

Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Coroutines/Flow ·
`CallScreeningService`. Min SDK 29, target SDK 36. No third-party runtime dependencies
beyond AndroidX.
