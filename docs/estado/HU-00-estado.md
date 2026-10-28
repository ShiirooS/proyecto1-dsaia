# HU-00 — Base técnica sobre developer: registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · mergeada** |
| Issue | #1 |
| Pull request | #8 |
| Rama | `feature/HU-00-base-tecnica-v2` |
| Especificación | [../user-stories/HU-00.md](../user-stories/HU-00.md) |

## Qué se implementó
- Se eliminó `bin/` versionado y se corrigieron 37 errores de TypeScript.
- Perfiles `dev` (H2 modo PostgreSQL), `test` y `postgres` (+ `docker-compose.yml`).
- `User.role` pasó a `@Enumerated(STRING)` (el ORDINAL rompía H2).
- Errores unificados: `ApiResponse`, `BusinessException`, `ConflictException`, `ResourceNotFoundException`, `GlobalExceptionHandler`.
- CI en GitHub Actions (backend + frontend) y Vitest configurado.
- `ExistingApiCharacterizationTest` fija el comportamiento previo.

## Pruebas
- Backend: suite de caracterización y de contexto.
- Frontend: build y tsc limpios.

## Pendiente
- Ninguno.
