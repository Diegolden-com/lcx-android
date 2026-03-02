# QA Physical Device Report - 2026-03-02

## 1) Resumen Ejecutivo
- Fecha/hora de ejecución: 2026-03-02 17:56:25 CST (America/Mexico_City).
- Objetivo: QA P0 real en teléfono Android por USB contra entorno local.
- Resultado general: **NO COMPLETADO EN DISPOSITIVO FÍSICO** por bloqueo de infraestructura (`adb` sin device conectado).
- Cobertura alternativa ejecutada: pruebas unitarias/contrato/regresión completas en Android + validación de rutas API y correlación en backend.
- Hallazgos críticos:
  - **P1** corregido: cleanup de transacciones terminales en límite temporal (`updatedAt <= cutoff`).
  - **P0 abiertos**: ninguno detectado en cobertura automatizada local.

## 2) Contexto y Entorno
- Android repo: `/Users/diegolden/Code/LCX/lcx-android` (base `af294f3`, rama `main`).
- Web/backend repo: `/Users/diegolden/Code/LCX/v0-lcx-pwa` (commit `5431d23`, rama `main`).
- Stack backend local validado: Next.js API routes + Supabase local.
- Config dev Android validada en build generado:
  - `API_BASE_URL = http://127.0.0.1:3000`
  - `SUPABASE_URL = http://127.0.0.1:54321`
  - `SUPABASE_ANON_KEY = <desde supabase status -o env>`

## 3) Q1 Preflight Agent

### 3.1 Comandos obligatorios
```bash
adb devices
adb reverse tcp:3000 tcp:3000
adb reverse tcp:54321 tcp:54321
```
Resultado:
```text
List of devices attached

adb: no devices/emulators found
adb: no devices/emulators found
```
Estado: **FAIL** (bloqueante por falta de dispositivo USB visible por ADB).

### 3.2 Servicios locales
- Web local (`bun run dev`): **PASS**
- Supabase local (`supabase status`): **PASS**

Evidencia:
```text
node ... TCP *:3000 (LISTEN)
Project URL: http://127.0.0.1:54321
```

### 3.3 Build/instalación dev
Comando:
```bash
LCX_DEV_API_BASE_URL=http://127.0.0.1:3000 \
LCX_DEV_SUPABASE_URL=http://127.0.0.1:54321 \
LCX_DEV_SUPABASE_ANON_KEY=<from supabase status -o env> \
./gradlew :app:installDevDebug
```
Resultado:
- Compilación: **PASS**
- Instalación en device: **FAIL**

Evidencia:
```text
Execution failed for task ':app:installDevDebug'.
com.android.builder.testing.api.DeviceException: No connected devices!
```

## 4) Q2 Functional P0 Agent (Checklist)

> Nota: El tramo “en teléfono físico por USB” no pudo ejecutarse. Se marca FAIL físico y se añade cobertura automatizada equivalente donde aplica.

| Caso P0 | Físico USB | Cobertura alternativa local | Evidencia |
|---|---|---|---|
| Login válido/inválido | FAIL (bloqueado) | PASS | `AuthRepositoryTest` 9/9 OK |
| Crear ticket (validación + éxito) | FAIL (bloqueado) | PASS | `CreateTicketsContractTest` 16/16 OK |
| Cobro success | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (happy path) |
| Cobro cancel (NO paid) | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (`payment cancellation`) |
| Cobro success + fallo API (retry sync, NO recobrar) | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (`PaymentSucceededApiFailed` + retry sync) |
| Impresión success | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` |
| Impresión fail + retry | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (`print failure then retry succeeds`) |
| Impresión skip | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (`print failure then skip`) |
| Reanudación tras kill app (`resumeTransaction`) | FAIL (bloqueado) | PASS | `TransactionOrchestratorTest` (bloque resume) + `TransactionPersistenceTest` |
| Opening checklist bloqueante (409) con mensaje claro | FAIL (bloqueado) | PASS | `CreateTicketsContractTest` (`409 OPENING_CHECKLIST...`) |

## 5) Q3 Observability Agent

### 5.1 Captura de logs en dispositivo
- `adb logcat` con filtros `TXN|HTTP|TICKET|PAYMENT|PRINT|Correlation`: **NO EJECUTABLE** (sin device).

### 5.2 Trazabilidad por correlación (verificación de implementación)
- Android emite/propaga correlación y logs por tags:
  - `TXN` en `TransactionOrchestrator`
  - `HTTP` en `CorrelationIdInterceptor`
  - `TICKET`, `PAYMENT`, `PRINT` en repositorios
- Backend consume `X-Correlation-Id` y registra en `audit_logs`:
  - `POST /api/tickets` -> `action: ticket_create`
  - `PATCH /api/tickets/:id/status` -> `action: status_update`
  - `PATCH /api/tickets/:id/payment` -> `action: payment_update`

Estado Q3: **PARCIAL** (implementación validada por código, evidencia runtime end-to-end pendiente de device).

## 6) Q4 Bugfix Agent

### 6.1 Bug detectado
- ID: `QA-20260302-01`
- Severidad: **P1**
- Área: persistencia de transacciones (`cleanup`)
- Síntoma: con `maxAge=0`, registros `COMPLETED/CANCELLED` en el límite temporal no se eliminaban (condición estricta `<`).
- Evidencia inicial:
```text
TransactionPersistenceTest > cleanup removes old completed records but keeps active ones FAILED
expected null, but was SavedTransaction(... phase=COMPLETED ...)
```

### 6.2 Fix aplicado
- Archivo: `app/src/main/java/com/cleanx/lcx/core/transaction/data/TransactionDao.kt`
- Cambio:
```sql
AND updatedAt < :olderThan
```
->
```sql
AND updatedAt <= :olderThan
```

Racional: hace inclusivo el corte temporal, evita dejar “zombies” terminales en el borde exacto del cutoff.

### 6.3 Regresión post-fix
- `:app:testProdReleaseUnitTest --tests "...TransactionPersistenceTest.cleanup removes old completed records but keeps active ones"` -> **PASS**
- `./gradlew test` -> **PASS**
- Suites P0 relevantes verificadas (XML):
  - `TransactionOrchestratorTest` 22/22 OK
  - `TransactionPersistenceTest` 11/11 OK
  - `CreateTicketsContractTest` 16/16 OK
  - `UpdatePaymentContractTest` 13/13 OK
  - `UpdateStatusContractTest` 13/13 OK
  - `AuthRepositoryTest` 9/9 OK

## 7) Bugs (con severidad)

| ID | Severidad | Estado | Descripción |
|---|---|---|---|
| QA-20260302-01 | P1 | FIXED | Cleanup de transacciones terminales no inclusivo en límite temporal (`<` vs `<=`). |
| QA-20260302-BLOCKER-USB | P0 (infra) | OPEN | No hay dispositivo visible en `adb`; bloquea QA físico USB end-to-end. |

## 8) Commits
- `800b8ec` - `fix(android): make terminal transaction cleanup inclusive at cutoff`
- `29db1ae` - `docs(qa): add physical-device QA report for 2026-03-02`

## 9) Próximo paso operativo para cerrar QA físico
1. Conectar teléfono por USB y autorizar huella RSA (`adb devices -l` debe mostrar estado `device`).
2. Repetir:
   - `adb reverse tcp:3000 tcp:3000`
   - `adb reverse tcp:54321 tcp:54321`
   - `./gradlew :app:installDevDebug`
3. Ejecutar checklist P0 en dispositivo y adjuntar evidencia `adb logcat` + correlación en `audit_logs`.
