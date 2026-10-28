# HU-02 — Calendario dinámico de disponibilidad: registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · mergeada** |
| Issue | #3 |
| Pull request | #11 (fix #12) |
| Rama | `feature/HU-02-calendario-disponibilidad-v2` |
| Especificación | [../user-stories/HU-02.md](../user-stories/HU-02.md) |

## Qué se implementó
- Disponibilidad derivada de reservas CONFIRMED (`OccupancyCalculator`).
- `GET /api/room/get-availability/{id}` y `get-all?activeOnly`.
- `Reservation` con `LocalDate` y `ReservationStatus`; 409/422 ante solapes.
- Seed: 2 hoteles, 8 habitaciones, reservas de 60 días.
- Frontend: `components/calendar/*` y `RoomDetailsPage`.

## Pruebas
- Backend: RoomAvailabilityTest.
- Frontend: 49 tests (test inestable corregido en #12).

## Pendiente
- El revisor web automático falló por límite de uso; la revisión la hizo el flujo principal.
