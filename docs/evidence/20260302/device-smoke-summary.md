# Device Smoke Test Summary

- **Date:** 2026-03-02
- **Branch:** `codex/p0-manual-matrix`

## Device Info

| Field         | Value                  |
|---------------|------------------------|
| Serial        | 49281FDAQ0011J         |
| Connection    | USB (usb:1-1)          |
| Product       | tokay                  |
| Model         | Pixel_9                |
| Device        | tokay                  |
| Transport ID  | 1                      |

## Port Forwarding Status

| Port  | Direction | Status  |
|-------|-----------|---------|
| 3000  | reverse   | SUCCESS |
| 54321 | reverse   | SUCCESS |

## Log Capture Results

- **Logcat buffer cleared:** YES
- **Capture mode:** `adb logcat` (live stream with filter + tee)
- **Filter pattern:** `TXN|HTTP|TICKET|PAYMENT|PRINT|BROTHER|Correlation`
- **Status:** PASS (flujo manual ejecutado)

### Evidencia clave capturada

```text
03-02 19:14:54.102 D HTTP  : [f712a2b8-ea26-447c-a0a6-e189d10e4a2e] POST http://127.0.0.1:3000/api/tickets
03-02 19:14:54.107 I okhttp.OkHttpClient: X-Correlation-Id: f712a2b8-ea26-447c-a0a6-e189d10e4a2e
03-02 19:15:00.904 I PrintModule: using BrotherPrinterManager (useRealBrother=true)
03-02 19:15:05.997 D PRINT : Brother discovery completed: 16 printer(s)
03-02 19:15:08.831 D PRINT : Connecting to: QL-810W (192.168.100.47)
03-02 19:15:09.033 I PRINT : Brother connected: type=WIFI address=192.168.100.47 name=QL-810W
03-02 19:15:10.450 I PRINT : Brother print success: ticket=T-20260303-0004 folio=4 printer=QL-810W
```

### Verificación de no-regresión stub

- No aparecieron líneas `STUB-BROTHER` durante esta corrida.
- No apareció `Brother SDK reflection bridge unavailable` después del hotfix.

### Correlación backend (A3 script)

Comando:

```bash
cd /Users/diegolden/Code/LCX/v0-lcx-pwa
./scripts/qa/correlation-audit-proof.sh f712a2b8-ea26-447c-a0a6-e189d10e4a2e
```

Resultado:

```text
| ticket_create | 2026-03-03 01:14:52.733 | /api/tickets | f712a2b8-ea26-447c-a0a6-e189d10e4a2e | source=encargo count=1 |
```

## Errors

- Ninguno en el smoke de impresión WiFi.
