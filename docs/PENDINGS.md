# Pendientes

Actualizado: 2026-10-28.

## Desarrollo
1. **HU-05**: mis reservas y cancelación (backend, frontend, tests, PR #6). Ver [estado/HU-05-estado.md](estado/HU-05-estado.md).
2. **HU-03**: terminar analítica (DTO, servicio, controlador, seed de 24 meses, frontend con recharts, PR #4). Ver [estado/HU-03-estado.md](estado/HU-03-estado.md).
3. **Deuda menor de HU-01**: lock failure → 409/503, unique constraint en `payment_method`, `refund` en el gateway, `ReservationConcurrencyTest`.

## Evidencia (se deja para el final)
- Consolidar `ai-log/raw.md` en el AI Development Log (actividad, herramienta, qué se pidió, resultado, decisión humana).
- Ejemplos de respuestas incorrectas de la IA, cambios rechazados y decisiones humanas.
- Material para la presentación.

## Cierre
- Los merges a `develop` no cierran issues solos: cerrarlos a mano.
- Cualquier fix de un check fallido va en una rama `fix/HU-XX-descripcion`.
