# ADR-002 — H2 (modo PostgreSQL) para test/dev y PostgreSQL real vía docker-compose

- **Estado**: Aceptado (2026-10-28) — implementado en `4dca3a6` y `bf107a7`
- **HU**: HU-00

## Contexto

`application.properties` apuntaba a un PostgreSQL local con credenciales fijas, y el único test (`contextLoads`) fallaba sin esa base. El equipo trabaja en Windows y no todos tienen Docker. La CI (GitHub Actions) debe ejecutar los tests sin servicios externos. HU-03 necesita 24 meses de histórico para demostrar la previsión.

## Decisión

| Perfil | Base | `ddl-auto` | Seed | Uso |
|--------|------|-----------|------|-----|
| `dev` (por defecto, `spring.profiles.default=dev`) | H2 en memoria, `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH` | `create-drop` | Sí (`DevDataSeeder`) + `/h2-console` | `./mvnw spring-boot:run` sin dependencias |
| `test` | H2 en memoria (misma URL de compatibilidad) | `create-drop` | No: cada test crea sus datos | `./mvnw test`, CI |
| `postgres` | PostgreSQL 16 de `docker-compose.yml`; `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `update` | No | Demo y verificación previa a `main` |

Reglas de portabilidad (obligatorias en revisión de código):
1. Solo JPQL o métodos derivados de Spring Data. **Prohibido** `nativeQuery = true` y funciones propias de Postgres (`date_trunc`, `generate_series`, `daterange`, `EXCLUDE`).
2. Agregaciones por mes en Java (`OccupancyCalculator`), no en SQL.
3. Sin columnas con nombres reservados en H2 (`value`, `year`, `month`, `day`, `user`, `order`, `key`).
4. Dinero con `BigDecimal` + `@Column(precision = 10, scale = 2)`; enums con `@Enumerated(EnumType.STRING)`.

## Alternativas consideradas

| Alternativa | Por qué no (ahora) |
|-------------|--------------------|
| Testcontainers con PostgreSQL | La fidelidad es máxima, pero exige Docker en cada máquina Windows del equipo y alarga la CI. Es la mejora natural si aparecen diferencias de dialecto. |
| PostgreSQL obligatorio en dev | Bloquea a quien no tenga Docker o Postgres instalado. |
| Embedded Postgres (zonky) | Binarios nativos por SO; más frágil en Windows. |

## Consecuencias

- (+) `git clone` → `./mvnw spring-boot:run` funciona sin instalar nada más, con datos de demo.
- (+) CI rápida y determinista.
- (−) H2 no es Postgres: pueden aparecer diferencias en bloqueos, mayúsculas o tipos. Mitigación: QA arranca con el perfil `postgres` y ejecuta el checklist manual antes de fusionar a `main`.
- (−) `ddl-auto=update` en `postgres` no elimina columnas renombradas (`availbility`, `start_date`). Mitigación: `docker compose down -v` al cambiar el modelo. Las migraciones quedan fuera de alcance.
