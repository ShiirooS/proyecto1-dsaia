# Contexto del proyecto

Proyecto #1 «Desarrollo asistido por IA» (Tópicos Especiales II). Repo base: `natanael-lima/hotel-reservation-system`, trabajado en `ShiirooS/proyecto1-dsaia`.

## Decisiones humanas clave
- Rehacer todo sobre la rama `developer` del upstream, rechazando la recomendación de la IA de seguir sobre `main` ([ADR-007](architecture/adr/ADR-007-base-developer-en-lugar-de-main.md)). El trabajo previo quedó archivado en `origin/archive/main-*`.
- HU extra 1: control de acceso por roles (la IA recomendaba «búsqueda por fechas»; se rechazó). HU extra 2: mis reservas y cancelación.
- Un PR por HU, revisado y mergeado a `develop`; los fixes van en ramas `fix/HU-XX-descripcion`.

## Stack
Spring Boot 3.3 / Java 17, JWT, JPA (H2 en dev/test, PostgreSQL en prod); React 18 + Vite + TypeScript + Tailwind; Vitest; CI en GitHub Actions y SonarCloud.

## Flujo de trabajo
Issues por HU (#1 a #6, etiqueta `historia-usuario`), rama `feature/HU-XX-...`, commits `feat(HU-XX): ...`, PR a `develop` con CI y SonarCloud, revisión y merge.

## Estado
| HU | Estado | Registro |
|----|--------|----------|
| HU-00 base técnica | Mergeada (PR #8) | [estado](estado/HU-00-estado.md) |
| HU-01 reserva con datos guardados | Mergeada (PR #13) | [estado](estado/HU-01-estado.md) |
| HU-02 calendario | Mergeada (PR #11, #12) | [estado](estado/HU-02-estado.md) |
| HU-03 previsión de demanda | Parcial | [estado](estado/HU-03-estado.md) |
| HU-04 roles | Mergeada (PR #9, #10) | [estado](estado/HU-04-estado.md) |
| HU-05 mis reservas | Pendiente | [estado](estado/HU-05-estado.md) |

## Incidentes y correcciones
- Revisión de HU-04: 3 críticos (mass assignment de `id`, pagos accesibles a USER, toma de cuenta por renombrado) corregidos con tests.
- Revisión de HU-01: fuga de PAN/CVV en el log DEBUG por `@Pattern`; corregido con `checkFormat` y `SensitiveDataTest`.
- SonarCloud S7044/S8476 en HU-04: se resolvió con `toPathId`.
- Test inestable de HU-02: `cleanup()` antes de resetear mocks (#12).

Relacionados: [ARCHITECTURE](architecture/ARCHITECTURE.md), [API-CONTRACT](architecture/API-CONTRACT.md), [historias](user-stories/README.md), [revisiones](reviews/).
