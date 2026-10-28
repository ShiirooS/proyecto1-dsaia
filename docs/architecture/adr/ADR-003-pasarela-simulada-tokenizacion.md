# ADR-003 — Pasarela de pago simulada con tokenización

- **Estado**: Aceptado (2026-10-28)
- **HU**: HU-01 (lo usa HU-05 para reembolsos)

## Contexto

HU-01 pide reservar "usando mis datos guardados" sin rellenar el formulario de pago cada vez. Integrar una pasarela real (el README original menciona Mercado Pago y Stripe; `developer` declara el SDK `com.mercadopago:sdk-java` y un `MercadoPagoConfig` que no se usan) exige credenciales, conexión a internet y webhooks, y metería datos de tarjeta reales en el alcance de PCI-DSS. El proyecto es académico y se evalúa el proceso, no la integración con terceros.

## Decisión

1. Interfaz `service/PaymentGateway` con tres operaciones: `tokenize(CardData) → {token, brand, last4}`, `charge(brand, last4, amount) → {transactionId}` (o rechazo; `amount` entero en USD, como `Payment.amount` de `developer`) y `refund(transactionId, amount)`. Implementación única: `SimulatedPaymentGateway`.
2. **Tokenización** (al guardar una tarjeta o al pagar con tarjeta nueva):
   - normaliza el número (quita espacios y guiones);
   - detecta la marca (VISA / MASTERCARD / AMEX; otra → `CARD_BRAND_UNSUPPORTED`);
   - valida Luhn, longitud y CVV según la marca (`CARD_INVALID`) y la expiración contra el `Clock` (`CARD_EXPIRED`);
   - genera `tok_` + 24 caracteres alfanuméricos con `SecureRandom`.
3. **Persistencia permitida**: `token`, `brand`, `last4`, `expMonth`, `expYear`, `holderName`. **Nunca** el número completo (PAN) ni el CVV, ni siquiera cifrados.
4. **Cobro simulado**: aprobado salvo que `last4 == "0002"` → `PAYMENT_DECLINED` (402). Con tarjeta guardada no se pide CVV (*card-on-file*).
5. **Reembolso simulado**: siempre aprobado. Actualiza `Payment.status = REFUNDED` (valor añadido al enum existente `COMPLETED | PENDING | CANCELLED`) y `refundedAmount`. Un cobro aprobado queda en `COMPLETED`.
6. Higiene de datos:
   - Los DTO con tarjeta (`CardInput`, `CreatePaymentMethodRequest`) excluyen `cardNumber` y `cvv` de `toString` (`@ToString.Exclude`).
   - Ningún log incluye bodies de `/api/payment-method/*` ni `/api/reservation/book`.
   - El `token` no se devuelve nunca en respuestas.
7. La validación de tarjeta vive en `domain/CardValidator` (pura, sin Spring). El frontend replica Luhn y marca solo para UX; manda la del servidor.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Stripe o Mercado Pago en modo test | Requiere secretos y red en CI, webhooks y más tiempo de integración; no aporta a las HU evaluadas. Por eso HU-00 retira el SDK sin uso. |
| Guardar el PAN cifrado | Entra en alcance PCI-DSS; contradice la restricción del equipo. |
| Tokens en el frontend (Stripe Elements) | Depende de un proveedor real. |

## Consecuencias

- (+) Demo y tests deterministas: las tarjetas de prueba (API-CONTRACT §11) producen siempre el mismo resultado.
- (+) Sustituible: una pasarela real implementaría `PaymentGateway` sin tocar los servicios.
- (+) Una fuga de BD no expone números ni CVV.
- (−) No representa 3-D Secure, fraude, reintentos ni webhooks asíncronos.
- (−) El rechazo se basa en los últimos 4 dígitos (convención de pruebas), no en un saldo real.
