# Contrato REST — Sprint HU-01..HU-05

> Fuente única de verdad entre frontend y backend. Si el código y este documento difieren, **gana este documento** hasta que se cambie por PR (con revisión de ambos lados).
> Versión 1.0 · 2026-10-28 · Relacionado: [ARCHITECTURE.md](ARCHITECTURE.md), [ADRs](adr/)

## Índice

- [0. Convenciones](#0-convenciones)
- [1. Tipos TypeScript compartidos](#1-tipos-typescript-compartidos)
- [2. Autenticación (HU-04)](#2-autenticación-hu-04)
- [3. Habitaciones y disponibilidad (HU-02)](#3-habitaciones-y-disponibilidad-hu-02)
- [4. Métodos de pago guardados (HU-01)](#4-métodos-de-pago-guardados-hu-01)
- [5. Reservas (HU-01, HU-05)](#5-reservas-hu-01-hu-05)
- [6. Analítica de previsión (HU-03)](#6-analítica-de-previsión-hu-03)
- [7. Endpoints existentes de Hotel](#7-endpoints-existentes-de-hotel)
- [8. Tarjetas de prueba (pasarela simulada)](#8-tarjetas-de-prueba-pasarela-simulada)
- [9. Resumen endpoint → rol → HU](#9-resumen-endpoint--rol--hu)

---

## 0. Convenciones

| Tema | Regla |
|------|-------|
| Base URL | `/api`. En desarrollo el frontend llama rutas relativas (`/api/...`); el proxy de Vite reenvía a `http://localhost:3000`. |
| Content-Type | `application/json; charset=UTF-8` en request y response. |
| Autenticación | Cabecera `Authorization: Bearer <jwt>`. Sin cookies. |
| Fechas de estancia | `LocalDate` ISO `YYYY-MM-DD` (`checkIn`, `checkOut`, `from`, `to`). Noches = `checkOut − checkIn` (en días). |
| Marcas de tiempo | `LocalDateTime` ISO **sin zona** `YYYY-MM-DDTHH:mm:ss`, expresado en la zona del hotel (`app.time-zone`, ver [ADR-006](adr/ADR-006-reloj-zona-horaria-check-in.md)). |
| Mes | `YYYY-MM` (analítica). |
| Dinero | Número JSON con 2 decimales (`450.00`). Moneda única implícita (USD). Backend: `BigDecimal` escala 2, `HALF_UP`. |
| Tasas | Número JSON entre 0 y 1 con 4 decimales (`0.6900`). |
| Nulos | En **respuestas de recursos**, los campos anulables se envían **presentes con `null`** (nunca se omiten). En el **sobre de error**, `code` y `errors` se omiten si no aplican. |
| Listas | Siempre `200` con `[]` si están vacías. **Desviación deliberada** de `GET /api/hotel/get-all`, que conserva su `204` histórico. |
| IDs | Enteros (`number`). Un recurso de otro usuario responde **404** (no 403) para no revelar su existencia. |
| Mensajes | `message` en español, para mostrar al usuario. El frontend decide su lógica por `code`, **nunca** por `message`. |
| Enums | Se serializan como texto en MAYÚSCULAS. Valores listados en §1. |

### 0.1 Sobre de error (`ApiResponse` extendido)

Se mantiene la clase `ApiResponse{message}` existente y se le añaden dos campos opcionales (`@JsonInclude(NON_NULL)`), compatible hacia atrás con el CRUD de hoteles.

```json
{
  "message": "Datos inválidos",
  "code": "VALIDATION_ERROR",
  "errors": { "email": "debe ser una dirección de correo válida", "card.cardNumber": "es obligatorio" }
}
```

- `errors` solo aparece con `VALIDATION_ERROR` (clave = ruta del campo con punto para anidados; valor = primer mensaje de ese campo).
- Los **401/403 de Spring Security** usan este mismo sobre (requiere `AuthenticationEntryPoint` y `AccessDeniedHandler` propios — ver HU-04); nunca cuerpo vacío.

### 0.2 Catálogo de códigos de error

| HTTP | `code` | Cuándo |
|------|--------|--------|
| 400 | `VALIDATION_ERROR` | Falla Bean Validation del body (`@Valid`). Incluye `errors`. |
| 400 | `MALFORMED_REQUEST` | JSON mal formado, fecha con formato inválido en el body, enum desconocido, tipo incorrecto en el body. |
| 400 | `MISSING_PARAMETER` | Falta un query param obligatorio. |
| 400 | `INVALID_PARAMETER` | Query/path param con tipo o rango inválido (p. ej. `from=2026-13-01`, `id=abc`, `horizon=20`). |
| 400 | `INVALID_DATE_RANGE` | Disponibilidad: `to < from` o rango > 366 días. |
| 400 | `INVALID_PAYMENT_SOURCE` | Reserva sin `paymentMethodId` ni `card`, con ambos, o `saveCard=true` sin `card`. |
| 401 | `UNAUTHORIZED` | Endpoint protegido sin token, token mal firmado, expirado o de usuario inexistente. |
| 401 | `INVALID_CREDENTIALS` | Login con usuario/email o contraseña incorrectos (mensaje genérico, sin revelar cuál falló). |
| 402 | `PAYMENT_DECLINED` | La pasarela simulada rechaza el cobro (tarjeta terminada en `0002`). |
| 403 | `FORBIDDEN` | Autenticado pero sin el rol requerido. |
| 404 | `NOT_FOUND` | Ruta inexistente. |
| 404 | `ROOM_NOT_FOUND` | Habitación inexistente o inactiva. |
| 404 | `HOTEL_NOT_FOUND` | Hotel inexistente (analítica con `hotelId`). |
| 404 | `PAYMENT_METHOD_NOT_FOUND` | Método inexistente o de otro usuario. |
| 404 | `RESERVATION_NOT_FOUND` | Reserva inexistente o de otro usuario. |
| 405 | `METHOD_NOT_ALLOWED` | Verbo HTTP no soportado en la ruta. |
| 409 | `USERNAME_ALREADY_EXISTS` | Registro con `username` ya usado (comparación sin mayúsculas). |
| 409 | `EMAIL_ALREADY_EXISTS` | Registro con email ya usado (comparación sin mayúsculas). |
| 409 | `ROOM_NOT_AVAILABLE` | Alguna noche del rango solicitado ya está ocupada por una reserva `CONFIRMED`. |
| 409 | `DUPLICATE_PAYMENT_METHOD` | Ya existe una tarjeta del usuario con misma marca, últimos 4 y expiración. |
| 409 | `RESERVATION_ALREADY_CANCELLED` | Cancelar una reserva ya cancelada. |
| 409 | `RESERVATION_NOT_CANCELLABLE` | Cancelar cuando ya pasó la hora de check-in. |
| 422 | `CHECKIN_IN_PAST` | `checkIn < hoy`. |
| 422 | `INVALID_STAY_DATES` | `checkOut <= checkIn`. |
| 422 | `MAX_NIGHTS_EXCEEDED` | Más de 30 noches. |
| 422 | `BOOKING_TOO_FAR` | `checkIn > hoy + 365 días`. |
| 422 | `GUESTS_EXCEED_CAPACITY` | `guests > room.capacity`. |
| 422 | `CARD_INVALID` | Número no pasa Luhn, longitud no corresponde a la marca, o CVV con longitud incorrecta para la marca. |
| 422 | `CARD_EXPIRED` | (`expYear`,`expMonth`) anterior al mes actual (tarjeta nueva o guardada). |
| 422 | `CARD_BRAND_UNSUPPORTED` | Marca distinta de VISA / MASTERCARD / AMEX. |
| 422 | `INSUFFICIENT_HISTORY` | Analítica sin habitaciones activas o con 0 noches ocupadas en la ventana de 24 meses. |
| 500 | `INTERNAL_ERROR` | Cualquier otra excepción. `message` genérico, sin stack trace. |

Mapeo obligatorio de excepciones de framework (en `GlobalExceptionHandler`): `HttpMessageNotReadableException`→`MALFORMED_REQUEST`; `MissingServletRequestParameterException`→`MISSING_PARAMETER`; `MethodArgumentTypeMismatchException`, `ConstraintViolationException` y `HandlerMethodValidationException` (Spring 6.1: `@Min`/`@Max` sobre `@RequestParam` sin `@Validated` de clase)→`INVALID_PARAMETER`; `MethodArgumentNotValidException`→`VALIDATION_ERROR`; `NoResourceFoundException`→`NOT_FOUND`; `HttpRequestMethodNotSupportedException`→`METHOD_NOT_ALLOWED`; `Exception`→`INTERNAL_ERROR`.

**Orden de evaluación de errores** (para que front y back coincidan cuando hay varios): 401/403 → 400 → 404 → 422 → 409 → 402.

---

## 1. Tipos TypeScript compartidos

Copiar tal cual a `hotel-reservation-system-frontend/src/types/api.ts`. El backend debe producir exactamente estas formas.

```ts
// ---------- Enums ----------
export type Role = 'USER' | 'ADMIN';
export type ReservationStatus = 'CONFIRMED' | 'CANCELLED';
export type ReservationPhase = 'UPCOMING' | 'IN_STAY' | 'COMPLETED' | 'CANCELLED';
export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX';
export type PaymentStatus = 'APPROVED' | 'REFUNDED';
export type InventoryAction = 'INCREASE' | 'KEEP' | 'REDUCE';
export type ErrorCode =
  | 'VALIDATION_ERROR' | 'MALFORMED_REQUEST' | 'MISSING_PARAMETER' | 'INVALID_PARAMETER'
  | 'INVALID_DATE_RANGE' | 'INVALID_PAYMENT_SOURCE' | 'UNAUTHORIZED' | 'INVALID_CREDENTIALS'
  | 'PAYMENT_DECLINED' | 'FORBIDDEN' | 'NOT_FOUND' | 'ROOM_NOT_FOUND' | 'HOTEL_NOT_FOUND'
  | 'PAYMENT_METHOD_NOT_FOUND' | 'RESERVATION_NOT_FOUND' | 'METHOD_NOT_ALLOWED'
  | 'USERNAME_ALREADY_EXISTS' | 'EMAIL_ALREADY_EXISTS' | 'ROOM_NOT_AVAILABLE'
  | 'DUPLICATE_PAYMENT_METHOD' | 'RESERVATION_ALREADY_CANCELLED' | 'RESERVATION_NOT_CANCELLABLE'
  | 'CHECKIN_IN_PAST' | 'INVALID_STAY_DATES' | 'MAX_NIGHTS_EXCEEDED' | 'BOOKING_TOO_FAR'
  | 'GUESTS_EXCEED_CAPACITY' | 'CARD_INVALID' | 'CARD_EXPIRED' | 'CARD_BRAND_UNSUPPORTED'
  | 'INSUFFICIENT_HISTORY' | 'INTERNAL_ERROR';

// ---------- Comunes ----------
export type IsoDate = string;      // 'YYYY-MM-DD'
export type IsoDateTime = string;  // 'YYYY-MM-DDTHH:mm:ss' (zona del hotel, sin offset)
export type YearMonth = string;    // 'YYYY-MM'

export interface ApiError {
  message: string;
  code?: ErrorCode;
  errors?: Record<string, string>;
}

// ---------- Auth ----------
export interface UserProfile {
  id: number;
  username: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  createdAt: IsoDateTime;
}
export interface RegisterRequest {
  username: string; email: string; password: string; fullName: string; phone?: string | null;
}
export interface LoginRequest { username: string; password: string } // username acepta usuario o email
export interface AuthResponse { token: string; user: UserProfile }

// ---------- Habitaciones ----------
export interface HotelSummary { id: number; name: string; location: string }
export interface HotelDetail extends HotelSummary { description: string | null; rating: number | null }
export interface RoomSummary {
  id: number; roomNumber: string; type: string; pricePerNight: number;
  capacity: number; imageUrl: string | null; hotel: HotelSummary;
}
export interface RoomDetail extends Omit<RoomSummary, 'hotel'> {
  description: string | null; hotel: HotelDetail;
}
export interface RoomAvailability {
  roomId: number;
  from: IsoDate; to: IsoDate;          // ambos inclusive
  today: IsoDate;                      // "hoy" según la zona del hotel
  checkInTime: string;                 // 'HH:mm', p. ej. '15:00'
  maxNights: number;                   // 30
  occupiedDates: IsoDate[];            // noches ocupadas dentro de [from, to], orden ascendente, sin duplicados
}

// ---------- Métodos de pago ----------
export interface CardInput {
  cardNumber: string;   // solo dígitos (el back también acepta espacios/guiones y los elimina)
  expMonth: number;     // 1..12
  expYear: number;      // 4 dígitos
  cvv: string;          // 3 dígitos (4 en AMEX). NUNCA se persiste.
  holderName: string;
}
export interface CreatePaymentMethodRequest extends CardInput { makeDefault?: boolean }
export interface PaymentMethod {
  id: number; brand: CardBrand; last4: string; expMonth: number; expYear: number;
  holderName: string; isDefault: boolean; expired: boolean; createdAt: IsoDateTime;
}

// ---------- Reservas ----------
export interface CreateReservationRequest {
  roomId: number; checkIn: IsoDate; checkOut: IsoDate; guests: number;
  paymentMethodId?: number | null;     // opción A: tarjeta guardada
  card?: CardInput | null;             // opción B: tarjeta nueva
  saveCard?: boolean;                  // solo con opción B
}
export interface PaymentSummary {
  id: number; status: PaymentStatus; amount: number; refundedAmount: number;
  cardBrand: CardBrand; cardLast4: string; transactionId: string; paidAt: IsoDateTime;
}
export interface CancellationInfo {
  cancellable: boolean; refundable: boolean;
  refundDeadline: IsoDateTime;         // checkIn a las checkInTime − 48 h
  refundAmountIfCancelledNow: number;
}
export interface Reservation {
  id: number; status: ReservationStatus; phase: ReservationPhase;
  checkIn: IsoDate; checkOut: IsoDate; nights: number; guests: number;
  totalAmount: number; createdAt: IsoDateTime; cancelledAt: IsoDateTime | null;
  room: { id: number; roomNumber: string; type: string; imageUrl: string | null };
  hotel: HotelSummary;
  payment: PaymentSummary;
  cancellation: CancellationInfo;
}
export interface CreateReservationResponse extends Reservation { savedPaymentMethodId: number | null }

// ---------- Analítica ----------
export interface SeasonalIndex { month: number; index: number } // month 1..12 (enero = 1)
export interface HistoryPoint {
  month: YearMonth; occupiedRoomNights: number; availableRoomNights: number;
  occupancyRate: number; trendRate: number;
}
export interface ForecastPoint {
  month: YearMonth; forecastOccupancyRate: number; forecastRoomNights: number;
  availableRoomNights: number; bookedRoomNights: number; bookedOccupancyRate: number;
  priceAdjustmentPct: number; suggestedPricePerNight: number;
  inventoryAction: InventoryAction; recommendedActiveRooms: number;
}
export interface DemandForecast {
  generatedAt: IsoDateTime; hotelId: number | null; horizon: number;
  activeRooms: number; averagePricePerNight: number;
  historyFrom: YearMonth; historyTo: YearMonth;
  model: { type: 'LINEAR_TREND_MONTHLY_SEASONALITY'; intercept: number; slope: number; seasonalIndices: SeasonalIndex[] };
  history: HistoryPoint[];   // 24 elementos, del más antiguo al más reciente
  forecast: ForecastPoint[]; // `horizon` elementos, empezando por el mes actual
}
```

---

## 2. Autenticación (HU-04)

### 2.1 `POST /api/auth/register` — Público

Request:
```json
{ "username": "ana.perez", "email": "ana@example.com", "password": "Secreta123", "fullName": "Ana Pérez", "phone": "+507 6000-0000" }
```
Validación: `username` 3–30 `[a-zA-Z0-9._-]`; `email` válido ≤ 120; `password` 8–72 con ≥1 letra y ≥1 dígito; `fullName` 1–100 no vacío; `phone` opcional ≤ 30. `username` y `email` se guardan en minúsculas. Cualquier campo `role` en el body se **ignora** (siempre `USER`).

Response `201`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "id": 7, "username": "ana.perez", "email": "ana@example.com", "fullName": "Ana Pérez",
            "phone": "+507 6000-0000", "role": "USER", "createdAt": "2026-10-28T10:15:00" }
}
```
Errores: `400 VALIDATION_ERROR`, `400 MALFORMED_REQUEST`, `409 USERNAME_ALREADY_EXISTS`, `409 EMAIL_ALREADY_EXISTS`.

### 2.2 `POST /api/auth/login` — Público

Request (`username` acepta nombre de usuario **o** email, sin distinguir mayúsculas):
```json
{ "username": "ana@example.com", "password": "Secreta123" }
```
Response `200`: mismo cuerpo que 2.1.
Errores: `400 VALIDATION_ERROR` (campos vacíos), `401 INVALID_CREDENTIALS` → `{"message":"Usuario o contraseña incorrectos","code":"INVALID_CREDENTIALS"}`.

JWT: HS256, `sub` = id de usuario (string), claims `username`, `role`; expiración `app.jwt.expiration-minutes` (por defecto 1440). Cierre de sesión = el cliente descarta el token (no hay endpoint de logout).

### 2.3 `GET /api/auth/me` — Autenticado (USER o ADMIN)

Response `200`: `UserProfile`.
```json
{ "id": 7, "username": "ana.perez", "email": "ana@example.com", "fullName": "Ana Pérez",
  "phone": "+507 6000-0000", "role": "USER", "createdAt": "2026-10-28T10:15:00" }
```
Errores: `401 UNAUTHORIZED` → `{"message":"Debe iniciar sesión para acceder a este recurso","code":"UNAUTHORIZED"}`.

Respuesta 403 estándar (cualquier ruta con rol insuficiente): `{"message":"No tiene permisos para acceder a este recurso","code":"FORBIDDEN"}`.

Token inválido en un endpoint **público**: se ignora y la petición se atiende como anónima (no 401).

---

## 3. Habitaciones y disponibilidad (HU-02)

### 3.1 `GET /api/rooms?hotelId={id}` — Público

`hotelId` opcional. Devuelve solo habitaciones `active = true`, orden `hotel.id ASC, roomNumber ASC`.

Response `200` (`RoomSummary[]`):
```json
[
  { "id": 3, "roomNumber": "203", "type": "DOBLE", "pricePerNight": 150.00, "capacity": 2,
    "imageUrl": "https://img.example.com/203.jpg",
    "hotel": { "id": 1, "name": "Hotel Mar Azul", "location": "Ciudad de Panamá" } }
]
```
Errores: `400 INVALID_PARAMETER` (`hotelId` no numérico). `hotelId` inexistente → `200 []`.

### 3.2 `GET /api/rooms/{id}` — Público

Response `200` (`RoomDetail`):
```json
{ "id": 3, "roomNumber": "203", "type": "DOBLE", "pricePerNight": 150.00, "capacity": 2,
  "imageUrl": "https://img.example.com/203.jpg", "description": "Vista al mar, cama queen.",
  "hotel": { "id": 1, "name": "Hotel Mar Azul", "location": "Ciudad de Panamá",
             "description": "Frente a la bahía.", "rating": 4.5 } }
```
Errores: `400 INVALID_PARAMETER`, `404 ROOM_NOT_FOUND` (también si está inactiva).

### 3.3 `GET /api/rooms/{id}/availability?from=YYYY-MM-DD&to=YYYY-MM-DD` — Público

**`from` y `to` son ambos INCLUSIVE** y se refieren a **noches**. Una fecha `d` aparece en `occupiedDates` si existe una reserva `CONFIRMED` de esa habitación con `checkIn <= d < checkOut`. El día de check-out de una reserva **no** está ocupado. Rango máximo: 366 fechas. `from` puede estar en el pasado (el front deshabilita fechas `< today`).

Ejemplo — reserva existente `checkIn=2026-11-10`, `checkOut=2026-11-13`:

`GET /api/rooms/3/availability?from=2026-11-01&to=2026-11-30` → `200`
```json
{ "roomId": 3, "from": "2026-11-01", "to": "2026-11-30", "today": "2026-10-28",
  "checkInTime": "15:00", "maxNights": 30,
  "occupiedDates": ["2026-11-10", "2026-11-11", "2026-11-12"] }
```
Consecuencias (las mismas reglas aplica el servidor en 5.1):

| Intento | Noches pedidas | Resultado |
|---------|----------------|-----------|
| 08 → 10 | 08, 09 | Válido: el check-out cae en un día ocupado, pero esa noche no se usa. |
| 13 → 15 | 13, 14 | Válido: el 13 es el check-out de la otra reserva. |
| 09 → 11 | 09, **10** | `409 ROOM_NOT_AVAILABLE`. |
| 10 → 11 | **10** | `409 ROOM_NOT_AVAILABLE`. |

Errores: `400 MISSING_PARAMETER`, `400 INVALID_PARAMETER` (fecha mal formada), `400 INVALID_DATE_RANGE` (`to < from` o > 366 fechas), `404 ROOM_NOT_FOUND`.

---

## 4. Métodos de pago guardados (HU-01)

Todas requieren autenticación (USER o ADMIN) y solo operan sobre los métodos del usuario del token. El **token simulado** (`tok_...`) nunca sale del backend.

### 4.1 `GET /api/payment-methods`

Response `200` (`PaymentMethod[]`), orden: predeterminado primero, luego `createdAt DESC`:
```json
[
  { "id": 5, "brand": "VISA", "last4": "4242", "expMonth": 12, "expYear": 2028,
    "holderName": "ANA PEREZ", "isDefault": true, "expired": false, "createdAt": "2026-10-28T10:20:00" }
]
```
Nota backend: el JSON debe llamarse `isDefault` (usar `@JsonProperty("isDefault")`; Lombok/Jackson lo serializa como `default` por defecto).

### 4.2 `POST /api/payment-methods` — tokeniza y guarda

Request:
```json
{ "cardNumber": "4242424242424242", "expMonth": 12, "expYear": 2028, "cvv": "123",
  "holderName": "Ana Pérez", "makeDefault": false }
```
Proceso: normalizar número (quitar espacios/guiones) → detectar marca → Luhn → longitud por marca → CVV por marca → expiración → duplicado → generar `tok_` + 24 caracteres alfanuméricos aleatorios → persistir **solo** token, marca, últimos 4, mes/año, titular (en MAYÚSCULAS). El CVV y el número completo se descartan y **nunca** se registran en logs. Si es la primera tarjeta del usuario, queda predeterminada aunque `makeDefault=false`.

Validación de formato (400): `cardNumber` 13–19 dígitos tras normalizar; `expMonth` 1–12; `expYear` 2000–2099; `cvv` `^\d{3,4}$`; `holderName` 2–60.

Response `201`: `PaymentMethod`.
Errores: `400 VALIDATION_ERROR`, `422 CARD_BRAND_UNSUPPORTED`, `422 CARD_INVALID`, `422 CARD_EXPIRED`, `409 DUPLICATE_PAYMENT_METHOD`.

### 4.3 `DELETE /api/payment-methods/{id}`

Response `204` sin cuerpo. Si era la predeterminada y quedan otras, pasa a predeterminada la más reciente (`createdAt DESC, id DESC`). Los pagos históricos no se ven afectados (guardan su propia copia de marca/últimos 4).
Errores: `404 PAYMENT_METHOD_NOT_FOUND`.

### 4.4 `PATCH /api/payment-methods/{id}/default`

Sin body. Marca esta tarjeta como predeterminada y desmarca las demás del usuario (en una transacción). Idempotente.
Response `200`: `PaymentMethod` actualizado.
Errores: `404 PAYMENT_METHOD_NOT_FOUND`.

---

## 5. Reservas (HU-01, HU-05)

### 5.1 `POST /api/reservations` — Autenticado

Exactamente una fuente de pago: **A)** `paymentMethodId` (tarjeta guardada, sin CVV), o **B)** `card` (tarjeta nueva) con `saveCard` opcional.

Request A (reserva en un clic con la tarjeta guardada):
```json
{ "roomId": 3, "checkIn": "2026-12-20", "checkOut": "2026-12-23", "guests": 2, "paymentMethodId": 5 }
```
Request B (tarjeta nueva y guardarla):
```json
{ "roomId": 3, "checkIn": "2026-12-20", "checkOut": "2026-12-23", "guests": 2,
  "card": { "cardNumber": "5555555555554444", "expMonth": 6, "expYear": 2029, "cvv": "321", "holderName": "Ana Pérez" },
  "saveCard": true }
```
Reglas (en este orden, ver §0.2):
1. Formato: `roomId`, `checkIn`, `checkOut`, `guests` (1–10) obligatorios → `400 VALIDATION_ERROR`. Fuente de pago: ninguna, ambas, o `saveCard=true` sin `card` → `400 INVALID_PAYMENT_SOURCE`.
2. Habitación inexistente/inactiva → `404 ROOM_NOT_FOUND`. `paymentMethodId` inexistente o ajeno → `404 PAYMENT_METHOD_NOT_FOUND`.
3. `checkIn < hoy` → `422 CHECKIN_IN_PAST`; `checkOut <= checkIn` → `422 INVALID_STAY_DATES`; noches > 30 → `422 MAX_NIGHTS_EXCEEDED`; `checkIn > hoy+365` → `422 BOOKING_TOO_FAR`; `guests > capacity` → `422 GUESTS_EXCEED_CAPACITY`; validación de tarjeta (nueva o guardada vencida) → `422 CARD_*`.
4. Con la fila de la habitación bloqueada (ADR-005), si existe reserva `CONFIRMED` que solape (`existente.checkIn < nuevo.checkOut AND nuevo.checkIn < existente.checkOut`) → `409 ROOM_NOT_AVAILABLE`.
5. Cobro simulado de `totalAmount = noches × room.pricePerNight`. Últimos 4 = `0002` → `402 PAYMENT_DECLINED` y **no se persiste nada** (ni reserva, ni pago, ni tarjeta).
6. Se persisten reserva (`CONFIRMED`) + pago (`APPROVED`, `transactionId = "txn_" + 24 alfanuméricos`) en la misma transacción. Si `saveCard=true`, se guarda la tarjeta; si ya existía un duplicado, **no falla**: se reutiliza y se devuelve su id.

Doble clic: el segundo POST idéntico responde `409 ROOM_NOT_AVAILABLE` (solapa con la primera); el front debe deshabilitar el botón mientras espera.

Response `201` (`CreateReservationResponse`), cabecera `Location: /api/reservations/42`:
```json
{
  "id": 42, "status": "CONFIRMED", "phase": "UPCOMING",
  "checkIn": "2026-12-20", "checkOut": "2026-12-23", "nights": 3, "guests": 2,
  "totalAmount": 450.00, "createdAt": "2026-10-28T10:30:00", "cancelledAt": null,
  "room": { "id": 3, "roomNumber": "203", "type": "DOBLE", "imageUrl": "https://img.example.com/203.jpg" },
  "hotel": { "id": 1, "name": "Hotel Mar Azul", "location": "Ciudad de Panamá" },
  "payment": { "id": 77, "status": "APPROVED", "amount": 450.00, "refundedAmount": 0.00,
               "cardBrand": "MASTERCARD", "cardLast4": "4444", "transactionId": "txn_8f3k2m9q1z7x4c6v0b5n2a1s",
               "paidAt": "2026-10-28T10:30:00" },
  "cancellation": { "cancellable": true, "refundable": true,
                    "refundDeadline": "2026-12-18T15:00:00", "refundAmountIfCancelledNow": 450.00 },
  "savedPaymentMethodId": 6
}
```
Errores: `400 VALIDATION_ERROR | MALFORMED_REQUEST | INVALID_PAYMENT_SOURCE`, `401 UNAUTHORIZED`, `404 ROOM_NOT_FOUND | PAYMENT_METHOD_NOT_FOUND`, `422 CHECKIN_IN_PAST | INVALID_STAY_DATES | MAX_NIGHTS_EXCEEDED | BOOKING_TOO_FAR | GUESTS_EXCEED_CAPACITY | CARD_INVALID | CARD_EXPIRED | CARD_BRAND_UNSUPPORTED`, `409 ROOM_NOT_AVAILABLE`, `402 PAYMENT_DECLINED`.

### 5.2 `GET /api/reservations/me` — Autenticado

Reservas del usuario del token (todas las fases), orden `checkIn DESC, id DESC`. Sin paginación.
Response `200`: `Reservation[]` (misma forma que 5.1 sin `savedPaymentMethodId`).

Cálculo de `phase` (con `hoy` en la zona del hotel): `CANCELLED` si `status=CANCELLED`; `UPCOMING` si `hoy < checkIn`; `IN_STAY` si `checkIn <= hoy < checkOut`; `COMPLETED` si `checkOut <= hoy`.

Cálculo de `cancellation` (`ahora` = `LocalDateTime.now(clock)`; `checkInAt = checkIn` a las `app.reservation.check-in-time`):
- `refundDeadline = checkInAt − 48 h`.
- `cancellable = status == CONFIRMED && ahora < checkInAt`.
- `refundable = cancellable && ahora <= refundDeadline` (≥ 48 h de antelación, límite inclusivo).
- `refundAmountIfCancelledNow = refundable ? totalAmount : 0.00`.

### 5.3 `GET /api/reservations/{id}` — Autenticado (dueño)

Response `200`: `Reservation`. Errores: `400 INVALID_PARAMETER`, `404 RESERVATION_NOT_FOUND` (inexistente o ajena; un ADMIN tampoco ve reservas ajenas por este endpoint).

### 5.4 `POST /api/reservations/{id}/cancel` — Autenticado (dueño)

Sin body. Aplica la política de §5.2 en el instante de la petición:
- Reembolsable → `status=CANCELLED`, `cancelledAt=ahora`, `payment.status=REFUNDED`, `payment.refundedAmount=totalAmount`.
- No reembolsable (menos de 48 h) → `status=CANCELLED`, `cancelledAt=ahora`, `payment.status=APPROVED`, `payment.refundedAmount=0.00`.
- Las noches quedan libres de inmediato (la disponibilidad solo cuenta `CONFIRMED`).

Response `200`: `Reservation` actualizada. Ejemplo (cancelada con reembolso):
```json
{ "id": 42, "status": "CANCELLED", "phase": "CANCELLED", "checkIn": "2026-12-20", "checkOut": "2026-12-23",
  "nights": 3, "guests": 2, "totalAmount": 450.00, "createdAt": "2026-10-28T10:30:00",
  "cancelledAt": "2026-11-02T09:00:00",
  "room": { "id": 3, "roomNumber": "203", "type": "DOBLE", "imageUrl": "https://img.example.com/203.jpg" },
  "hotel": { "id": 1, "name": "Hotel Mar Azul", "location": "Ciudad de Panamá" },
  "payment": { "id": 77, "status": "REFUNDED", "amount": 450.00, "refundedAmount": 450.00, "cardBrand": "MASTERCARD",
               "cardLast4": "4444", "transactionId": "txn_8f3k2m9q1z7x4c6v0b5n2a1s", "paidAt": "2026-10-28T10:30:00" },
  "cancellation": { "cancellable": false, "refundable": false, "refundDeadline": "2026-12-18T15:00:00",
                    "refundAmountIfCancelledNow": 0.00 } }
```
Errores: `404 RESERVATION_NOT_FOUND`, `409 RESERVATION_ALREADY_CANCELLED`, `409 RESERVATION_NOT_CANCELLABLE`.

---

## 6. Analítica de previsión (HU-03)

### 6.1 `GET /api/admin/analytics/demand-forecast?horizon=6&hotelId={id}` — ADMIN

- `horizon` opcional, entero 1–12, por defecto 6 → fuera de rango: `400 INVALID_PARAMETER`.
- `hotelId` opcional; sin él se agregan todos los hoteles.
- Ventana histórica: los **24 meses completos anteriores al mes actual**. El pronóstico empieza en el **mes actual**.
- Algoritmo, redondeos y reglas de precio/inventario: **[ADR-004](adr/ADR-004-algoritmo-prevision-demanda.md)** (normativo).

Response `200` (`DemandForecast`), ejemplo con reloj fijo `2027-01-15T10:00` y la serie del caso de prueba del ADR-004 (abreviado `...`):
```json
{
  "generatedAt": "2027-01-15T10:00:00", "hotelId": null, "horizon": 6,
  "activeRooms": 10, "averagePricePerNight": 100.00,
  "historyFrom": "2025-01", "historyTo": "2026-12",
  "model": { "type": "LINEAR_TREND_MONTHLY_SEASONALITY", "intercept": 0.5350, "slope": 0.0100,
             "seasonalIndices": [ { "month": 1, "index": -0.0950 }, { "month": 2, "index": -0.1050 }, "..." ] },
  "history": [
    { "month": "2025-01", "occupiedRoomNights": 140, "availableRoomNights": 310, "occupancyRate": 0.4516, "trendRate": 0.5450 },
    "..."
  ],
  "forecast": [
    { "month": "2027-01", "forecastOccupancyRate": 0.6900, "forecastRoomNights": 214, "availableRoomNights": 310,
      "bookedRoomNights": 35, "bookedOccupancyRate": 0.1129, "priceAdjustmentPct": 0,
      "suggestedPricePerNight": 100.00, "inventoryAction": "KEEP", "recommendedActiveRooms": 10 },
    { "month": "2027-05", "forecastOccupancyRate": 0.8900, "forecastRoomNights": 276, "availableRoomNights": 310,
      "bookedRoomNights": 0, "bookedOccupancyRate": 0.0000, "priceAdjustmentPct": 15,
      "suggestedPricePerNight": 115.00, "inventoryAction": "INCREASE", "recommendedActiveRooms": 12 },
    "..."
  ]
}
```
Los valores de `history[0]` y `bookedRoomNights` son ilustrativos (el caso de prueba del ADR-004 trabaja directamente sobre tasas, no sobre noches enteras).

Campos: `bookedRoomNights` = noches ya reservadas (`CONFIRMED`) en ese mes futuro ("on the books"); `averagePricePerNight` = media de `pricePerNight` de las habitaciones activas del alcance.
Errores: `400 INVALID_PARAMETER`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 HOTEL_NOT_FOUND`, `422 INSUFFICIENT_HISTORY` → `{"message":"No hay suficiente histórico para calcular la previsión","code":"INSUFFICIENT_HISTORY"}`.

---

## 7. Endpoints existentes de Hotel

Se conservan rutas y formas actuales (`HotelDTO`, `ApiResponse{message}`, `204` en lista vacía). Único cambio: seguridad.

| Método | Ruta | Rol |
|--------|------|-----|
| GET | `/api/hotel/get-all` | Público |
| GET | `/api/hotel/get-hotel/{id}` | Público |
| POST | `/api/hotel/registration-hotel` | ADMIN |
| PUT | `/api/hotel/update-hotel/{id}` | ADMIN |
| DELETE | `/api/hotel/delete-hotel/{id}` | ADMIN |

---

## 8. Tarjetas de prueba (pasarela simulada)

| Número | Marca | CVV | Resultado |
|--------|-------|-----|-----------|
| 4242 4242 4242 4242 | VISA | 3 díg. | Aprobada |
| 5555 5555 5555 4444 | MASTERCARD | 3 díg. | Aprobada |
| 3782 822463 10005 | AMEX | 4 díg. | Aprobada |
| 4000 0000 0000 0002 | VISA | 3 díg. | Se tokeniza bien; el **cobro** responde `402 PAYMENT_DECLINED` |
| 4242 4242 4242 4241 | — | — | `422 CARD_INVALID` (Luhn) |
| 6011 1111 1111 1117 | Discover | — | `422 CARD_BRAND_UNSUPPORTED` |

Detección de marca: VISA = empieza por `4`, longitud 13/16/19; MASTERCARD = prefijo `51`–`55` o `2221`–`2720`, longitud 16; AMEX = prefijo `34`/`37`, longitud 15. CVV: AMEX 4 dígitos, resto 3.

---

## 9. Resumen endpoint → rol → HU

| # | Método | Ruta | Rol | HU |
|---|--------|------|-----|----|
| 1 | POST | `/api/auth/register` | Público | HU-04 |
| 2 | POST | `/api/auth/login` | Público | HU-04 |
| 3 | GET | `/api/auth/me` | Autenticado | HU-04 |
| 4 | GET | `/api/rooms` | Público | HU-02 |
| 5 | GET | `/api/rooms/{id}` | Público | HU-02 |
| 6 | GET | `/api/rooms/{id}/availability` | Público | HU-02 |
| 7 | GET | `/api/payment-methods` | Autenticado | HU-01 |
| 8 | POST | `/api/payment-methods` | Autenticado | HU-01 |
| 9 | DELETE | `/api/payment-methods/{id}` | Autenticado | HU-01 |
| 10 | PATCH | `/api/payment-methods/{id}/default` | Autenticado | HU-01 |
| 11 | POST | `/api/reservations` | Autenticado | HU-01 |
| 12 | GET | `/api/reservations/me` | Autenticado | HU-05 |
| 13 | GET | `/api/reservations/{id}` | Autenticado (dueño) | HU-01/HU-05 |
| 14 | POST | `/api/reservations/{id}/cancel` | Autenticado (dueño) | HU-05 |
| 15 | GET | `/api/admin/analytics/demand-forecast` | ADMIN | HU-03 |
