# Printing Test Hardening — Evidence

- **Date:** 2026-03-02
- **Branch:** `codex/printing-test-hardening`
- **Module:** `feature:printing`

## Tests Added / Modified

### BrotherErrorMapperTest (42 tests — was 15, added 27)

**New tests for legacy SDK (numeric) error codes:**
- `negative error code maps to UNKNOWN_PRINTER_ERROR`
- `Int MAX_VALUE error code maps to UNKNOWN_PRINTER_ERROR`
- `each known error code maps to a unique error code string`

**New tests for SDK v4 (string) error codes — full branch coverage:**
- `v4 PrinterStatusErrorCoverOpen maps to COVER_OPEN`
- `v4 PrinterStatusErrorPaperEmpty maps to NO_PAPER`
- `v4 SetLabelSizeError maps to NO_PAPER`
- `v4 PrinterStatusErrorBatteryWeak maps to BATTERY_LOW`
- `v4 PrinterStatusErrorBatteryChargeError maps to BATTERY_LOW`
- `v4 ChannelErrorOpen maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorNotSupported maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorGetStatusTimeout maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorBidirectionalOff maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorNoSendData maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorPjWrongResponse maps to COMMUNICATION_ERROR`
- `v4 ChannelErrorCommandError maps to COMMUNICATION_ERROR`
- `v4 PrinterStatusErrorCommunicationError maps to COMMUNICATION_ERROR`
- `all v4 communication errors share same code and message`
- `v4 PrinterStatusErrorOverHeat maps to OVERHEATING`
- `v4 PrinterStatusErrorPaperJam maps to PAPER_JAM`
- `v4 PrinterStatusErrorHighVoltageAdapter maps to HIGH_VOLTAGE_ADAPTER`
- `v4 PrintSettingsError maps to PRINT_SETTINGS_ERROR`
- `v4 InvalidParameterError maps to PRINT_SETTINGS_ERROR`
- `v4 TemplateFileNotMatchModelError maps to PRINT_SETTINGS_ERROR`
- `v4 unknown error with null description falls back to errorCode in message`
- `v4 unknown error with blank description falls back to errorCode in message`
- `v4 unknown error with empty string description falls back to errorCode in message`
- `v4 unknown error without description parameter uses default`
- `all known v4 error codes produce non-blank code and message`

### PrintRepositoryTest (35 tests — was 22, added 13)

**New retry / transient failure tests:**
- `printWithRetry succeeds on second attempt after transient failure`
- `printWithRetry returns last error code not the first`
- `printWithRetry with maxAttempts 1 does not retry`
- `printWithRetry uses default MAX_RETRY_ATTEMPTS of 3`
- `printWithRetry with 3 copies prints exactly 3 on all success`
- `printWithRetry multi-copy retries each copy independently`

**New cleanup on permanent failure tests:**
- `permanent failure returns error without crashing`
- `disconnect after permanent print failure does not throw`

**New rapid-press / double-action guard tests:**
- `concurrent printWithRetry calls both execute independently`
- `selectPrinter overwrites previous selection`

**New auto-connect edge cases:**
- `tryAutoConnect returns false when connect throws exception`
- `tryAutoConnect sets selectedPrinter on success`

**New state management tests:**
- `isConnected returns false when PrinterManager says false`
- `forgetPrinter is safe when no printer was selected`

## Test Execution Output

```
BUILD SUCCESSFUL

BrotherErrorMapperTest : tests=42, skipped=0, failures=0, errors=0
PrintRepositoryTest    : tests=35, skipped=0, failures=0, errors=0
LabelRendererTest      : tests=11, skipped=0, failures=0, errors=0
PrinterPreferencesTest : tests=12, skipped=0, failures=0, errors=0

TOTAL: 100 tests, 0 failures, 0 skipped
```

## Coverage Improvements

| Test Class | Before | After | Delta |
|---|---|---|---|
| BrotherErrorMapperTest | 15 | 42 | +27 |
| PrintRepositoryTest | 22 | 35 | +13 |
| LabelRendererTest | 11 | 11 | 0 (untouched) |
| PrinterPreferencesTest | 12 | 12 | 0 (untouched) |
| **Total** | **60** | **100** | **+40** |

Key coverage gains:
- **BrotherErrorMapper.mapSdkV4Error:** All 21 known error code strings now have dedicated assertions (was 3)
- **BrotherErrorMapper.mapSdkV4Error unknown fallback:** null, blank, empty, and omitted `description` parameter all tested
- **PrintRepository retry logic:** transient-then-success, last-error-returned, max-attempts boundary, multi-copy retry independence
- **PrintRepository cleanup:** permanent failure + disconnect sequence, forgetPrinter from clean state
- **PrintRepository concurrency:** concurrent coroutine calls verified

## Status

**PASS**
