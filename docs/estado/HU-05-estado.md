# HU-05 — Mis reservas y cancelación (extra 2): registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · en PR** |
| Issue | #6 |
| Pull request | (se completa al abrir el PR) |
| Rama | `feature/HU-05-mis-reservas-cancelacion` |
| Especificación | [../user-stories/HU-05.md](../user-stories/HU-05.md) |

## Qué se implementó
- `GET /api/reservation/my-reservations` (200 `[]` si no hay) y `PUT /api/reservation/cancel-reservation/{id}`.
- Cancelación con la fila de la reserva bloqueada (`findOwnedByIdForUpdate`): dos cancelaciones simultáneas no reembolsan dos veces.
- Política recalculada en el servidor en el instante de cancelar: ≥ 48 h → `REFUNDED` y `refundedAmount = total`; < 48 h → sigue `COMPLETED` con 0; desde el check-in → 409 `RESERVATION_NOT_CANCELLABLE`; ya cancelada → 409 `RESERVATION_ALREADY_CANCELLED`; ajena → 404.
- `PaymentGateway.refund` (pendiente de HU-01) en la pasarela simulada.
- Seed `dev`: el usuario `demo` tiene una reserva reembolsable, una que empieza mañana, una pasada y una cancelada; las reservas sintéticas ya no se solapan con ellas.
- Frontend: `/bookings` con pestañas Próximas / Pasadas / Canceladas (contadores, flechas del teclado), estados de carga, error y vacío; `ReservationCard` y `CancelReservationModal` (foco atrapado, Esc, foco inicial en "Mantener reserva").

## Desviaciones del brief
- `listMyReservations`/`cancelMyReservation` viven en `BookingService` (flujos del huésped, junto a `getMyReservation` de HU-01) y no en `ReservationServiceImp`, que es el CRUD de administración.
- Se quitaron los botones "Download PDF" y "Rate & Review" de la maqueta original: no hacían nada.

## Pruebas
- Backend: `MyReservationsTest` (10, con `MutableClockConfig` para mover el reloj al límite exacto de 48 h, a +1 s y a la hora de check-in) y `CancellationPolicyTest` (+1 caso de límite).
- Frontend: `Bookings.test.tsx` (4), `ReservationCard.test.tsx` (6), `CancelReservationModal.test.tsx` (5).
- QA manual con `demo` en el perfil `dev`: cancelar la reembolsable ($360) y la de mañana (sin reembolso) por teclado; `GET /api/room/get-availability` deja de listar esas noches.

## Pendiente
- Nada de la HU. Merge del PR y cierre manual del issue #6.
