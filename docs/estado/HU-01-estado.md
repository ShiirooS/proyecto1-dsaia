# HU-01 — Reservar con mis datos de pago guardados: registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · mergeada** |
| Issue | #2 |
| Pull request | #13 |
| Rama | `feature/HU-01-reserva-datos-guardados` |
| Especificación | [../user-stories/HU-01.md](../user-stories/HU-01.md) |

## Qué se implementó
- Entidad `PaymentMethod`: solo token, marca, últimos 4, vencimiento y titular.
- `/api/payment-method/*`: listar, crear, predeterminada, borrar.
- `POST /api/reservation/book` (201 + Location): tarjeta guardada o nueva, `saveCard`.
- `CardValidator` (Luhn, marca, CVV, vencimiento) y `CancellationPolicy` como lógica pura.
- Pasarela simulada `PaymentGateway` (`****0002` → 402).
- Bloqueo pesimista de la habitación (ADR-005), también en reservas del admin.
- Frontend: `ConfirmBookingModal` como paso de pago y `PaymentMethodsPage`.

## Pruebas
- Backend: BookingTest (15), SensitiveDataTest (2), CardValidatorTest, CancellationPolicyTest.
- Frontend: 71 tests en total.

## Pendiente
- `PessimisticLockingFailureException` → 409/503.
- Unique constraint en `payment_method`.
- `refund` en el gateway (lo necesita HU-05).
- `ReservationConcurrencyTest`.
- `savedPaymentMethodId` siempre presente (null).
