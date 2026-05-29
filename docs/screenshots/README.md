# Steintroll — Screenshots

Captured on a Google Pixel 10 Pro (Android 16) via wireless adb, 2026-05-29.

## Main app

| File | Screen |
|------|--------|
| `01-home.png` | Home — prominent troll mark, "17 calls blocked" stat (taps to log), red "Block these" mode, UK (+44) in the block-list, Add country, Scan-call-history, withheld toggle |
| `02-log.png` | Blocked log — UK numbers (flag + country + timestamp) and "Withheld number" entries, each with Delete; Clear in the bar |
| `03-add-country.png` | Add country (mode-aware title "Add country to block") — searchable list with flags + dial codes; ✓ on already-blocked United Kingdom |
| `08-suggestions.png` | After "Scan call history for spam" — SUGGESTED TO BLOCK section with foreign + infrequent codes (Netherlands +31, Denmark +45), each with Dismiss / Block |

## Debug companion (`Steintroll Debug`, debug builds only)

| File | Screen |
|------|--------|
| `05-debug-top.png` | Terminal-style console — `steintroll::debug` + DEV badge, QUICK CALLS (🇬🇧/🇳🇴/🇸🇪/🔒) + custom fire, live STATUS, LOG |
| `06-debug-advanced.png` | Lower console — STATUS readouts, LOG, the `advanced` divider, SETUP `$` commands, SETTINGS (mode toggle, withheld, add/rm code) |
