# HU-05 — Mis reservas y cancelación (extra 2): registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Pendiente** |
| Issue | #6 |
| Pull request | — |
| Rama | `(sin rama aún)` |
| Especificación | [../user-stories/HU-05.md](../user-stories/HU-05.md) |

## Qué se implementó
- Reutilizará `BookingService.getMyReservation/toMyReservation` y `CancellationPolicy` de HU-01.

## Pruebas
- Ninguno.

## Pendiente
- `GET /api/reservation/my-reservations`.
- `PUT /api/reservation/cancel-reservation/{id}`: 48 h o más → reembolso total (Payment REFUNDED); si no, sin reembolso.
- Frontend: página `Bookings` real.
- Tests de servicio y de controlador; PR (Closes #6) y merge.
