# ADR-006 — Reloj inyectable, zona horaria del hotel y hora de check-in

- **Estado**: Aceptado (2026-10-28) — ⚠️ los valores por defecto son **suposiciones** pendientes de validar por el equipo
- **HU**: HU-00 (base), usado en HU-01, HU-02, HU-03 y HU-05

## Contexto

Varias reglas dependen de "ahora" y "hoy": no reservar en el pasado (HU-01), deshabilitar días pasados (HU-02), ventana de 24 meses y mes actual (HU-03), reembolso si se cancela ≥ 48 h antes del check-in (HU-05). Las fechas de estancia son `LocalDate` sin hora, así que "48 h antes del check-in" necesita una hora de check-in. Si cada clase llama a `LocalDate.now()`, los tests dependen del día de ejecución y de la zona horaria del runner de CI (UTC).

## Decisión

1. Bean `java.time.Clock` en `ClockConfig`: `Clock.system(ZoneId.of(app.time-zone))`. Todo el código usa `LocalDate.now(clock)` / `LocalDateTime.now(clock)`. Se prohíbe la versión sin argumento (se revisa en PR).
2. Propiedades en `application.properties`:
   - `app.time-zone=America/Panama` — ⚠️ **ASSUMPTION**: zona del hotel/demostración.
   - `app.reservation.check-in-time=15:00` — ⚠️ **ASSUMPTION**: hora estándar de check-in.
   - `app.reservation.max-nights=30`, `app.reservation.max-days-ahead=365`, `app.reservation.free-cancellation-hours=48`.
3. Las marcas de tiempo se serializan como `LocalDateTime` en esa zona y sin offset (API-CONTRACT §0).
4. `GET /api/room/get-availability/{id}` devuelve `today` y `checkInTime` para que el frontend no dependa del reloj ni de la zona del navegador.
5. En los tests, `@TestConfiguration` registra un `@Primary Clock.fixed(Instant, zona)`. Los tests de dominio reciben el `Clock` o la fecha por parámetro.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| UTC en todo | "Hoy" cambiaría a las 19:00 hora de Panamá; resultaría confuso en la demo. |
| Fechas con hora en la reserva (`LocalDateTime`) | El usuario elige días, no horas; complica el calendario y el cálculo de noches. |
| Zona del navegador | Dos usuarios verían "hoy" distinto; el servidor debe ser la referencia. |

## Consecuencias

- (+) Reglas temporales 100 % deterministas en los tests (límite de 48 h exacto, cambio de mes, años bisiestos).
- (+) Cambiar de zona o de hora de check-in es solo configuración.
- (−) Un único hotel/zona por despliegue. Para varias zonas haría falta `Hotel.timeZone` (fuera de alcance).
