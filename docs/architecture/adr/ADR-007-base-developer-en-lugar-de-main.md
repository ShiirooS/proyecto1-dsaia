# ADR-007 — Base del proyecto: `upstream/developer` en lugar de `upstream/main`

- **Estado**: Aceptado (2026-10-28). Decisión humana del equipo.
- **Sustituye**: la línea base implícita de la versión 1.0 de los documentos (`upstream/main` @ `2f82bdb`).

## Contexto

La primera versión de la arquitectura partía de `upstream/main` (PR #22): solo funcionaba el CRUD de hoteles y el frontend era una maqueta. Sobre esa base ya existían commits de HU-00, HU-04 (login JWT) y HU-02. En paralelo se detectó que `upstream/developer` va 32 commits por delante: tiene Spring Security con JWT, controladores y servicios de habitaciones, reservas, pagos, reseñas y usuarios, y un frontend con login/registro, panel de administración y servicios axios.

## Decisión

1. `upstream/developer` se integra en `develop` (`950bff7`) y se publica en `origin/main` y `origin/develop`.
2. El trabajo sobre `main` se archiva en `origin/archive/main-HU-00`, `archive/main-HU-04`, `archive/main-HU-02` (y `archive/main-docs`). Se **portan** piezas concretas, no ramas enteras (lista en ARCHITECTURE §2.3).
3. La HU-04 extra "registro e inicio de sesión JWT" se **descarta** porque `developer` ya la tiene. La sustituye **HU-04 "Control de acceso por roles"**, que absorbe los hallazgos de seguridad de `developer`.
4. Reglas de adaptación:
   - Se conservan rutas y formas de los endpoints que consume el frontend (`/auth/*`, `/api/hotel/*`, `/api/room/*`, `/api/reservation/*`, `/api/payment/*`, `/api/review/*`, `/api/user/*`).
   - Los endpoints nuevos siguen el estilo `/api/<recurso singular>/<verbo-recurso>`.
   - Un solo sobre de error `ApiResponse{message, code?, errors?}`.
   - Un solo cliente HTTP en el frontend (axios).
   - El modelo cambia lo mínimo para el frontend existente.
5. Ramas nuevas: `feature/HU-00-base-tecnica-v2`, `feature/HU-04-control-acceso-roles`, `feature/HU-02-calendario-disponibilidad-v2`, `feature/HU-01-reserva-datos-guardados`, `feature/HU-03-prevision-demanda`, `feature/HU-05-mis-reservas-cancelacion`.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Seguir sobre `main` | Se reimplementaría lo que `developer` ya resuelve (auth, CRUDs, panel admin) y el resultado divergiría del repositorio original. |
| Fusionar `archive/main-*` sobre `developer` | Conflictos masivos: dos `AuthContext`, dos clientes HTTP, dos juegos de rutas y DTOs. |

## Consecuencias

- (+) Se parte de más funcionalidad real (auth, CRUD admin, páginas). El sprint se concentra en las HU y en la calidad (tests, seguridad).
- (+) El trabajo archivado sigue disponible y trazable para el AI Development Log (decisión humana + cambios rechazados).
- (−) `developer` no compila en frontend (`tsc -b`), no tiene tests y trae deuda de seguridad; HU-00 y HU-04 la pagan antes de las funcionalidades.
- (−) Se heredan convenciones discutibles para no romper el frontend: rutas tipo RPC, dinero en enteros, `availbility` mal escrito, `204` en listas vacías.
- (−) Los documentos v1 (rutas `/api/rooms`, `/api/reservations`, `BigDecimal`, etc.) quedan sustituidos por la versión 2.0.
