# Contrato REST — Sprint HU-00..HU-05 (base `developer`)

> Fuente única de verdad entre frontend y backend. Si el código y este documento difieren, **gana este documento** hasta que se cambie por PR (con revisión de ambos lados).
> Versión 2.0 · 2026-10-28 · Base: `upstream/developer` integrada en `develop` (`950bff7`) — ver [ADR-007](adr/ADR-007-base-developer-en-lugar-de-main.md).
> Relacionado: [ARCHITECTURE.md](ARCHITECTURE.md), [ADRs](adr/)

## Leyenda de estado

| Marca | Significado |
|-------|-------------|
| **EXISTENTE** | Ruta y forma actuales de `developer`; solo cambia su regla de acceso (§13) y, en errores, el sobre §0.1. |
| **EXISTENTE A MODIFICAR** | Ruta actual; se indica exactamente qué cambia. El caso feliz conserva su forma salvo que se diga lo contrario. |
| **NUEVO** | Endpoint que no existe en `developer`. |

Regla: la UI de `developer` llama a estas rutas desde `src/services/*.ts`. No se renombra ninguna ruta existente.

## Índice

- [0. Convenciones](#0-convenciones)
- [1. Tipos TypeScript](#1-tipos-typescript)
- [2. Autenticación](#2-autenticación)
- [3. Hoteles](#3-hoteles)
- [4. Habitaciones y disponibilidad (HU-02)](#4-habitaciones-y-disponibilidad-hu-02)
- [5. Métodos de pago guardados (HU-01)](#5-métodos-de-pago-guardados-hu-01)
- [6. Reservas (HU-01, HU-05)](#6-reservas-hu-01-hu-05)
- [7. Pagos (administración)](#7-pagos-administración)
- [8. Reseñas](#8-reseñas)
- [9. Usuarios](#9-usuarios)
- [10. Analítica de previsión (HU-03)](#10-analítica-de-previsión-hu-03)
- [11. Tarjetas de prueba](#11-tarjetas-de-prueba-pasarela-simulada)
- [12. Resumen de endpoints](#12-resumen-de-endpoints)
- [13. Matriz RBAC](#13-matriz-rbac)

---

## 0. Convenciones

| Tema | Regla |
|------|-------|
| Base URL | El frontend usa la instancia axios de `src/services/axiosConfig.ts` (`baseURL = VITE_API_URL`, por defecto `http://localhost:3000`). Las rutas de auth cuelgan de `/auth`; las demás, de `/api/<recurso en singular>`. |
| CORS | Central en `SecurityConfig` para `http://localhost:5173` (sustituye a los `@CrossOrigin` sueltos; `Reservation/Payment/ReviewController` hoy no lo tienen). |
| Autenticación | Cabecera `Authorization: Bearer <jwt>`. El token se guarda en `localStorage['token']` (código actual de `authService.ts`). |
| Fechas de estancia | `LocalDate` ISO `YYYY-MM-DD`. En la entidad siguen llamándose `startDate`/`endDate`; en los endpoints **nuevos** el JSON usa `checkIn` (= `startDate`) y `checkOut` (= `endDate`). Noches = `checkOut − checkIn`. |
| Marcas de tiempo | `LocalDateTime` ISO sin zona (`YYYY-MM-DDTHH:mm:ss`), en la zona del hotel ([ADR-006](adr/ADR-006-reloj-zona-horaria-check-in.md)). |
| Dinero | **Entero** en unidades de USD (como `Room.pricePerNight` y `Payment.amount` en `developer`). Sin decimales. |
| Tasas | Número entre 0 y 1 con 4 decimales (`0.6900`). |
| Nulos | En respuestas de recursos, los campos anulables van **presentes con `null`**. En el sobre de error, `code` y `errors` se omiten si no aplican. |
| Listas | Endpoints **nuevos**: `200 []` si están vacías. Endpoints `get-all` existentes: conservan su `204` (lo verifica la caracterización de HU-00). |
| Recursos ajenos | Responden **404** (no 403) para no revelar su existencia. |
| Mensajes | `message` en español para mostrar. El frontend decide por `code`, nunca por `message`. |
| Roles | Autoridades `ADMIN` / `USER` **sin** prefijo `ROLE_` (`User.getAuthorities()`); en Spring se usa `hasAuthority(...)`. |

### 0.1 Sobre de error único (`ApiResponse` extendido)

`response/ApiResponse` conserva `message` y añade `code` y `errors` opcionales (`@JsonInclude(NON_NULL)`). Es compatible con el `{message}` actual que leen `RegisterPage` y los formularios.

```json
{ "message": "Datos inválidos", "code": "VALIDATION_ERROR",
  "errors": { "username": "debe ser un email válido", "card.cardNumber": "es obligatorio" } }
```

- Lo produce **solo** `exception/GlobalExceptionHandler` (que hoy no existe) y, para 401/403, el `AuthenticationEntryPoint` y el `AccessDeniedHandler` de `SecurityConfig`. Los controladores dejan de tener `try/catch(Exception)` → 500 (HU-04).
- `/error` se permite en `SecurityConfig` para que un error en un endpoint público no se convierta en 401/403 al re-despacharse.

### 0.2 Catálogo de códigos de error

| HTTP | `code` | Cuándo |
|------|--------|--------|
| 400 | `VALIDATION_ERROR` | Falla Bean Validation del body. Incluye `errors`. |
| 400 | `MALFORMED_REQUEST` | JSON mal formado, fecha/enum inválido o tipo incorrecto en el body. |
| 400 | `MISSING_PARAMETER` | Falta un query param obligatorio. |
| 400 | `INVALID_PARAMETER` | Query/path param con tipo o rango inválido (`id=abc`, `horizon=13`). |
| 400 | `INVALID_DATE_RANGE` | Disponibilidad: `to < from` o rango > 366 días. |
| 400 | `INVALID_PAYMENT_SOURCE` | Reserva sin `paymentMethodId` ni `card`, con ambos, o `saveCard=true` sin `card`. |
| 401 | `UNAUTHORIZED` | Ruta protegida sin token, o con token mal firmado, caducado o de un usuario que ya no existe. |
| 401 | `INVALID_CREDENTIALS` | Login fallido (mismo mensaje exista o no el usuario). |
| 402 | `PAYMENT_DECLINED` | La pasarela simulada rechaza el cobro (tarjeta terminada en `0002`). |
| 403 | `FORBIDDEN` | Autenticado sin la autoridad requerida. |
| 404 | `NOT_FOUND` | Ruta inexistente. |
| 404 | `HOTEL_NOT_FOUND` / `ROOM_NOT_FOUND` / `USER_NOT_FOUND` / `RESERVATION_NOT_FOUND` / `PAYMENT_NOT_FOUND` / `PAYMENT_METHOD_NOT_FOUND` / `REVIEW_NOT_FOUND` | Recurso inexistente o ajeno. |
| 405 | `METHOD_NOT_ALLOWED` | Verbo no soportado. |
| 409 | `USERNAME_ALREADY_EXISTS` | Registro con un email (`username`) ya usado, sin distinguir mayúsculas. |
| 409 | `ROOM_NOT_AVAILABLE` | Alguna noche del rango ya está ocupada por una reserva `CONFIRMED`. |
| 409 | `DUPLICATE_PAYMENT_METHOD` | Misma marca, últimos 4 y expiración que otra tarjeta del usuario. |
| 409 | `RESERVATION_ALREADY_CANCELLED` | Cancelar una reserva ya cancelada. |
| 409 | `RESERVATION_NOT_CANCELLABLE` | Cancelar cuando ya pasó la hora de check-in. |
| 422 | `CHECKIN_IN_PAST`, `INVALID_STAY_DATES`, `MAX_NIGHTS_EXCEEDED`, `BOOKING_TOO_FAR`, `GUESTS_EXCEED_CAPACITY` | Reglas de estancia (§6.1). |
| 422 | `CARD_INVALID`, `CARD_EXPIRED`, `CARD_BRAND_UNSUPPORTED` | Validación de tarjeta ([ADR-003](adr/ADR-003-pasarela-simulada-tokenizacion.md)). |
| 422 | `INSUFFICIENT_HISTORY` | Analítica sin habitaciones activas o sin ocupación en la ventana. |
| 500 | `INTERNAL_ERROR` | Cualquier otra excepción. Mensaje genérico, sin traza. |

Mapeo de excepciones de framework: `HttpMessageNotReadableException`→`MALFORMED_REQUEST`; `MissingServletRequestParameterException`→`MISSING_PARAMETER`; `MethodArgumentTypeMismatchException`, `ConstraintViolationException` y `HandlerMethodValidationException`→`INVALID_PARAMETER`; `MethodArgumentNotValidException`→`VALIDATION_ERROR`; `NoResourceFoundException`→`NOT_FOUND`; `HttpRequestMethodNotSupportedException`→`METHOD_NOT_ALLOWED`; `ResourceNotFoundException`/`BusinessException`→su estado y `code`; `Exception`→`INTERNAL_ERROR`.

Orden de evaluación: 401/403 → 400 → 404 → 422 → 409 → 402.

---

## 1. Tipos TypeScript

Se **amplían** las interfaces existentes de `src/services/*.ts`; no se crea un segundo juego de tipos ni otro cliente HTTP.

```ts
// services/userService.ts — existente; solo se estrecha el tipo de role
export type Role = 'ADMIN' | 'USER';
export interface UserDTO {
  id: number; username: string;            // username = email
  fullName?: string; profileImageUrl?: string; createdAt?: string;
  role?: Role;
}

// services/roomService.ts — RoomDTO existente sin cambios + tipo nuevo
// RoomDTO.availability pasa a significar "habitación activa / a la venta" (no disponibilidad por fechas)
export interface RoomAvailability {
  roomId: number;
  from: string; to: string;          // YYYY-MM-DD, ambos inclusive
  today: string;                     // "hoy" según la zona del hotel
  checkInTime: string;               // 'HH:mm'
  maxNights: number;                 // 30
  occupiedDates: string[];           // noches ocupadas, orden ascendente
}

// services/apiError.ts — NUEVO (helper sobre axios, no es otro cliente)
export type ErrorCode = string;      // valores de §0.2
export interface ApiError { status: number; message: string; code?: ErrorCode; errors?: Record<string, string> }
export function toApiError(err: unknown): ApiError; // lee error.response.status y error.response.data

// services/paymentMethodService.ts — NUEVO
export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX';
export interface CardInput { cardNumber: string; expMonth: number; expYear: number; cvv: string; holderName: string }
export interface CreatePaymentMethodRequest extends CardInput { makeDefault?: boolean }
export interface PaymentMethod {
  id: number; brand: CardBrand; last4: string; expMonth: number; expYear: number;
  holderName: string; isDefault: boolean; expired: boolean; createdAt: string;
}

// services/reservationService.ts — NUEVO
export type ReservationStatus = 'CONFIRMED' | 'CANCELLED';
export type ReservationPhase = 'UPCOMING' | 'IN_STAY' | 'COMPLETED' | 'CANCELLED';
export type PaymentStatus = 'COMPLETED' | 'PENDING' | 'CANCELLED' | 'REFUNDED';
export interface BookReservationRequest {
  roomId: number; checkIn: string; checkOut: string; guests: number;
  paymentMethodId?: number | null; card?: CardInput | null; saveCard?: boolean;
}
export interface PaymentSummary {
  id: number; status: PaymentStatus; amount: number; refundedAmount: number;
  cardBrand: CardBrand; cardLast4: string; transactionId: string; paymentDate: string;
}
export interface CancellationInfo {
  cancellable: boolean; refundable: boolean; refundDeadline: string; refundAmountIfCancelledNow: number;
}
export interface MyReservation {
  id: number; status: ReservationStatus; phase: ReservationPhase;
  checkIn: string; checkOut: string; nights: number; guests: number;
  totalAmount: number; createdAt: string; cancelledAt: string | null;
  room: { id: number; roomNumber: string; type: string; image: string | null };
  hotel: { id: number; name: string; location: string };
  payment: PaymentSummary; cancellation: CancellationInfo;
}
export interface BookReservationResponse extends MyReservation { savedPaymentMethodId: number | null }

// services/analyticsService.ts — NUEVO
export type InventoryAction = 'INCREASE' | 'KEEP' | 'REDUCE';
export interface DemandForecast {
  generatedAt: string; hotelId: number | null; horizon: number;
  activeRooms: number; averagePricePerNight: number;   // media con 2 decimales (informativa)
  historyFrom: string; historyTo: string;              // YYYY-MM
  model: { type: 'LINEAR_TREND_MONTHLY_SEASONALITY'; intercept: number; slope: number;
           seasonalIndices: { month: number; index: number }[] };
  history: { month: string; occupiedRoomNights: number; availableRoomNights: number;
             occupancyRate: number; trendRate: number }[];
  forecast: { month: string; forecastOccupancyRate: number; forecastRoomNights: number;
              availableRoomNights: number; bookedRoomNights: number; bookedOccupancyRate: number;
              priceAdjustmentPct: number; suggestedPricePerNight: number;   // entero
              inventoryAction: InventoryAction; recommendedActiveRooms: number }[];
}
```

`axiosConfig.ts` añade un interceptor de **respuesta**: ante `401` con `code: "UNAUTHORIZED"` borra `localStorage['token']` y notifica al `AuthContext` (HU-04).

---

## 2. Autenticación

### 2.1 `POST /auth/login` — Público · **EXISTENTE A MODIFICAR**

Request (actual): `{ "username": "ana@example.com", "password": "Secreta123" }` (`username` = email).
Response `200` (sin cambios): `{ "token": "eyJhbGciOiJIUzI1NiJ9..." }`.

Cambios (solo en los errores):
- Credenciales inválidas → `401 {"message":"Usuario o contraseña incorrectos","code":"INVALID_CREDENTIALS"}`. Mismo cuerpo exista o no el usuario. Hoy la `BadCredentialsException` sale del controlador sin manejar (la caracterización de HU-00 registra el estado actual).
- Campos vacíos → `400 VALIDATION_ERROR`.
- El token: `sub` = `username`, `iat`, `exp` (24 h), firmado HS256 con `app.jwt.secret` (ya no con la constante de `JwtServiceImp`). El rol **no** va en el token: se lee de la BD en cada petición ([ADR-001](adr/ADR-001-autenticacion-jwt-stateless.md)).

### 2.2 `POST /auth/registration-user` — Público · **EXISTENTE A MODIFICAR**

Request (actual): `{ "username": "ana@example.com", "password": "Secreta123", "fullName": "Ana Pérez", "profileImageUrl": null }`.
Response `200` (sin cambios): `{ "token": "..." }`.

Cambios:
- Validación: `username` email válido ≤ 120 (se guarda en minúsculas); `password` 8–72 con ≥1 letra y ≥1 dígito; `fullName` opcional ≤ 100 (hoy la columna admite `null` y `RegisterPage` no lo exige). → `400 VALIDATION_ERROR` (el `message` lo muestra `RegisterPage` tal cual).
- Duplicado → `409 {"message":"El email ya está registrado","code":"USERNAME_ALREADY_EXISTS"}`. Hoy responde `400` con el mensaje dentro del campo `token`.
- `role`, `id` y `createdAt` del body se **ignoran**: siempre `USER`, id generado, fecha del `Clock`.

### 2.3 `GET /api/user/current` — Autenticado · **EXISTENTE A MODIFICAR**

Response `200` (sin cambios, `UserDTO`):
```json
{ "id": 7, "username": "ana@example.com", "fullName": "Ana Pérez", "createdAt": "2026-10-28T10:15:00",
  "profileImageUrl": null, "role": "USER", "reservationIds": [], "reviews": [] }
```
Cambio: sin token → `401 UNAUTHORIZED` (hoy es `GET` público y el usuario anónimo termina en 404/500). Es el endpoint que usa la SPA para conocer el rol tras el login.

Respuestas estándar de seguridad (todas las rutas):
- `401 {"message":"Debe iniciar sesión para acceder a este recurso","code":"UNAUTHORIZED"}`
- `403 {"message":"No tiene permisos para acceder a este recurso","code":"FORBIDDEN"}`
- Token inválido en una ruta **pública**: se ignora (petición anónima), no hay 401.

---

## 3. Hoteles

`HotelDTO` actual: `{ id, name, location, description, rating, image, province, roomIds, reviews }`. Todas **EXISTENTES**; cambian el acceso y los errores (`404 HOTEL_NOT_FOUND` en lugar de 500/204).

| Método | Ruta | Éxito actual | Acceso |
|--------|------|--------------|--------|
| GET | `/api/hotel/get-all` | 200 `HotelDTO[]` / 204 | Público |
| GET | `/api/hotel/get-hotel/{id}` | 200 `HotelDTO` | Público |
| POST | `/api/hotel/registration-hotel` | 200 `{message}` | ADMIN |
| PUT | `/api/hotel/update-hotel/{id}` | 200 `{message}` | ADMIN |
| DELETE | `/api/hotel/delete-hotel/{id}` | 200 `{message}` | ADMIN |

Nota frontend: `hotelService.ts` usa `fetch` sin token, así que crear o editar un hotel ya falla hoy. HU-04 lo migra a la instancia axios.

---

## 4. Habitaciones y disponibilidad (HU-02)

`RoomDTO` actual: `{ id, roomNumber, type, pricePerNight (entero), availability, description, capacity, image, amenities[], hotelId }`.
`availability` (columna `availbility`) se **redefine** como "habitación activa / a la venta". La disponibilidad por fechas se deriva de las reservas ([ADR-005](adr/ADR-005-disponibilidad-derivada-de-reservas.md)).

| Método | Ruta | Estado | Acceso |
|--------|------|--------|--------|
| GET | `/api/room/get-all?activeOnly=true` | **EXISTENTE A MODIFICAR**: nuevo query param opcional `activeOnly` (por defecto `false` = comportamiento actual, lo usa `RoomTable` de admin). La vista pública `RoomsPages` envía `activeOnly=true`. | Público |
| GET | `/api/room/get-room/{id}` | EXISTENTE | Público |
| POST | `/api/room/registration-room` | EXISTENTE | ADMIN |
| PUT | `/api/room/update-room/{id}` | EXISTENTE | ADMIN |
| DELETE | `/api/room/delete-room/{id}` | EXISTENTE | ADMIN |
| GET | `/api/room/get-availability/{id}?from=YYYY-MM-DD&to=YYYY-MM-DD` | **NUEVO** | Público |

### 4.1 `GET /api/room/get-availability/{id}` — **NUEVO**

**`from` y `to` son ambos INCLUSIVE** y se refieren a **noches**. Una fecha `d` está en `occupiedDates` si existe una reserva `CONFIRMED` de la habitación con `startDate <= d < endDate`. El día de check-out de una reserva **no** está ocupado. Máximo 366 fechas. `from` puede estar en el pasado.

Ejemplo — reserva existente del `2026-11-10` al `2026-11-13`:

`GET /api/room/get-availability/3?from=2026-11-01&to=2026-11-30` → `200`
```json
{ "roomId": 3, "from": "2026-11-01", "to": "2026-11-30", "today": "2026-10-28",
  "checkInTime": "15:00", "maxNights": 30,
  "occupiedDates": ["2026-11-10", "2026-11-11", "2026-11-12"] }
```

| Intento | Noches | Resultado (misma regla en §6.1) |
|---------|--------|----------------------------------|
| 08 → 10 | 08, 09 | Válido: el check-out cae en un día ocupado, pero esa noche no se usa. |
| 13 → 15 | 13, 14 | Válido. |
| 09 → 11 | 09, **10** | `409 ROOM_NOT_AVAILABLE` |

Errores: `400 MISSING_PARAMETER | INVALID_PARAMETER | INVALID_DATE_RANGE`, `404 ROOM_NOT_FOUND` (inexistente o `availability=false`).

---

## 5. Métodos de pago guardados (HU-01)

Todo **NUEVO**. Requiere autenticación y opera solo sobre los métodos del usuario del token. El token simulado (`tok_...`) **nunca** sale en una respuesta.

| Método | Ruta | Éxito |
|--------|------|-------|
| GET | `/api/payment-method/get-all` | `200 PaymentMethod[]` (predeterminada primero, luego `createdAt DESC`) |
| POST | `/api/payment-method/registration-payment-method` | `201 PaymentMethod` |
| PUT | `/api/payment-method/set-default/{id}` | `200 PaymentMethod` (idempotente; desmarca las demás) |
| DELETE | `/api/payment-method/delete-payment-method/{id}` | `200 {"message":"El método de pago se eliminó correctamente"}` |

Ejemplo `PaymentMethod`:
```json
{ "id": 5, "brand": "VISA", "last4": "4242", "expMonth": 12, "expYear": 2028,
  "holderName": "ANA PEREZ", "isDefault": true, "expired": false, "createdAt": "2026-10-28T10:20:00" }
```
Request de alta: `{ "cardNumber": "4242424242424242", "expMonth": 12, "expYear": 2028, "cvv": "123", "holderName": "Ana Pérez", "makeDefault": false }`.

Reglas (sin cambios respecto a v1):
- Se normaliza el número (sin espacios ni guiones), se detecta la marca, se valida Luhn, longitud y CVV por marca, y la expiración. Se persisten **solo** token, marca, últimos 4, mes/año y titular (en mayúsculas).
- La primera tarjeta queda predeterminada. Si se borra la predeterminada, pasa a serlo la más reciente.
- Formato (400): `cardNumber` 13–19 dígitos; `expMonth` 1–12; `expYear` 2000–2099; `cvv` `^\d{3,4}$`; `holderName` 2–60.

Errores: `400 VALIDATION_ERROR`, `404 PAYMENT_METHOD_NOT_FOUND`, `409 DUPLICATE_PAYMENT_METHOD`, `422 CARD_BRAND_UNSUPPORTED | CARD_INVALID | CARD_EXPIRED`.
Nota backend: el JSON debe llamarse `isDefault` (`@JsonProperty("isDefault")`).

---

## 6. Reservas (HU-01, HU-05)

`ReservationDTO` actual: `{ id, startDate, endDate, status, createdAt, userId, roomId, payments[] }`.

| Método | Ruta | Estado | Acceso |
|--------|------|--------|--------|
| GET | `/api/reservation/get-all` | EXISTENTE A MODIFICAR (tipo de fecha) | ADMIN |
| GET | `/api/reservation/get-reservation/{id}` | EXISTENTE A MODIFICAR (tipo de fecha) | ADMIN |
| POST | `/api/reservation/registration-reservation` | EXISTENTE A MODIFICAR | ADMIN |
| PUT | `/api/reservation/update-reservation/{id}` | EXISTENTE A MODIFICAR | ADMIN |
| DELETE | `/api/reservation/delete-reservation/{id}` | EXISTENTE | ADMIN |
| POST | `/api/reservation/book` | **NUEVO** (§6.1) | Autenticado |
| GET | `/api/reservation/my-reservations` | **NUEVO** (§6.2) | Autenticado |
| GET | `/api/reservation/my-reservations/{id}` | **NUEVO** (§6.3) | Dueño |
| PUT | `/api/reservation/cancel-reservation/{id}` | **NUEVO** (§6.4) | Dueño |

Cambios en los existentes:
- `startDate`/`endDate` pasan de `LocalDateTime` a `LocalDate`: el JSON cambia de `"2026-12-20T00:00:00"` a `"2026-12-20"`. **Ninguna pantalla los consume** (verificado: `BookingTable.tsx` y `Bookings.tsx` usan arrays fijos).
- `status` solo acepta `CONFIRMED` o `CANCELLED` (`400 MALFORMED_REQUEST` si no).
- `registration-reservation` y `update-reservation` validan el solapamiento (`409 ROOM_NOT_AVAILABLE`) y quedan solo para ADMIN (reserva manual de recepción, sin cobro). Hoy cualquier autenticado puede crear reservas para cualquier `userId`.

### 6.1 `POST /api/reservation/book` — Autenticado · **NUEVO**

Exactamente una fuente de pago: **A)** `paymentMethodId` (tarjeta guardada, sin CVV) o **B)** `card` (+ `saveCard` opcional). El usuario sale del token, nunca del body.

```json
{ "roomId": 3, "checkIn": "2026-12-20", "checkOut": "2026-12-23", "guests": 2, "paymentMethodId": 5 }
```
```json
{ "roomId": 3, "checkIn": "2026-12-20", "checkOut": "2026-12-23", "guests": 2,
  "card": { "cardNumber": "5555555555554444", "expMonth": 6, "expYear": 2029, "cvv": "321", "holderName": "Ana Pérez" },
  "saveCard": true }
```
Reglas, en orden:
1. Formato (`guests` 1–10) → `400 VALIDATION_ERROR`. Fuente de pago ambigua → `400 INVALID_PAYMENT_SOURCE`.
2. Habitación inexistente o inactiva → `404 ROOM_NOT_FOUND`. `paymentMethodId` inexistente o ajeno → `404 PAYMENT_METHOD_NOT_FOUND`.
3. `checkIn < hoy` → `CHECKIN_IN_PAST`; `checkOut <= checkIn` → `INVALID_STAY_DATES`; > 30 noches → `MAX_NIGHTS_EXCEEDED`; `checkIn > hoy+365` → `BOOKING_TOO_FAR`; `guests > capacity` → `GUESTS_EXCEED_CAPACITY`; tarjeta → `CARD_*` (todos 422).
4. Con la fila de la habitación bloqueada, solapamiento con una `CONFIRMED` → `409 ROOM_NOT_AVAILABLE`.
5. Cobro simulado de `totalAmount = noches × pricePerNight`. Últimos 4 = `0002` → `402 PAYMENT_DECLINED` y **no se persiste nada**.
6. Se persisten reserva (`CONFIRMED`) y pago (`COMPLETED`, `transactionId = "txn_" + 24 alfanuméricos`) en una transacción. Con `saveCard=true` se guarda la tarjeta; si ya existía, se reutiliza.

Response `201`, cabecera `Location: /api/reservation/my-reservations/42`:
```json
{
  "id": 42, "status": "CONFIRMED", "phase": "UPCOMING",
  "checkIn": "2026-12-20", "checkOut": "2026-12-23", "nights": 3, "guests": 2,
  "totalAmount": 450, "createdAt": "2026-10-28T10:30:00", "cancelledAt": null,
  "room": { "id": 3, "roomNumber": "203", "type": "DOBLE", "image": "https://img.example.com/203.jpg" },
  "hotel": { "id": 1, "name": "Hotel Mar Azul", "location": "Ciudad de Panamá" },
  "payment": { "id": 77, "status": "COMPLETED", "amount": 450, "refundedAmount": 0,
               "cardBrand": "MASTERCARD", "cardLast4": "4444", "transactionId": "txn_8f3k2m9q1z7x4c6v0b5n2a1s",
               "paymentDate": "2026-10-28T10:30:00" },
  "cancellation": { "cancellable": true, "refundable": true,
                    "refundDeadline": "2026-12-18T15:00:00", "refundAmountIfCancelledNow": 450 },
  "savedPaymentMethodId": 6
}
```
Un segundo POST idéntico (doble clic) responde `409 ROOM_NOT_AVAILABLE`.

### 6.2 `GET /api/reservation/my-reservations` — Autenticado · **NUEVO**

`200 MyReservation[]` del usuario del token, orden `checkIn DESC, id DESC`, sin paginación.

`phase` (con `hoy` del `Clock`): `CANCELLED` si está cancelada; `UPCOMING` si `hoy < checkIn`; `IN_STAY` si `checkIn <= hoy < checkOut`; `COMPLETED` si `checkOut <= hoy`.

`cancellation` (`checkInAt` = `checkIn` a las `app.reservation.check-in-time`):
- `refundDeadline = checkInAt − 48 h`
- `cancellable = status == CONFIRMED && ahora < checkInAt`
- `refundable = cancellable && ahora <= refundDeadline`
- `refundAmountIfCancelledNow = refundable ? totalAmount : 0`

### 6.3 `GET /api/reservation/my-reservations/{id}` — Dueño · **NUEVO**

`200 MyReservation`. Ajena o inexistente → `404 RESERVATION_NOT_FOUND`.

### 6.4 `PUT /api/reservation/cancel-reservation/{id}` — Dueño · **NUEVO**

Sin body. Aplica §6.2 en el instante de la petición:
- Reembolsable → `status=CANCELLED`, `cancelledAt=ahora`, `payment.status=REFUNDED`, `payment.refundedAmount=totalAmount`.
- No reembolsable → `status=CANCELLED`, `cancelledAt=ahora`, `payment.status=COMPLETED`, `refundedAmount=0`.
- Las noches quedan libres al instante.

Response `200 MyReservation` (con `cancellation.cancellable=false`). Errores: `404 RESERVATION_NOT_FOUND`, `409 RESERVATION_ALREADY_CANCELLED | RESERVATION_NOT_CANCELLABLE`.

---

## 7. Pagos (administración)

`PaymentDTO` actual: `{ id, amount, paymentDate, status, idReservation }`. Todo **solo ADMIN**.

| Método | Ruta | Estado |
|--------|------|--------|
| GET | `/api/payment/get-all` | EXISTENTE A MODIFICAR (campos añadidos) |
| GET | `/api/payment/get-payment-details/{id}` | EXISTENTE A MODIFICAR (campos añadidos) |
| POST | `/api/payment/registration-payment` | EXISTENTE (alta manual por recepción) |
| PUT | `/api/payment/cancel-payment/{id}` | EXISTENTE |

Cambios aditivos en `PaymentDTO`: `cardBrand`, `cardLast4`, `transactionId`, `refundedAmount` (entero, por defecto 0). `status` añade `REFUNDED` a `COMPLETED | PENDING | CANCELLED`.

---

## 8. Reseñas

`ReviewDTO`: `{ id, rating, comment, createdAt, userId, hotelId }`.

| Método | Ruta | Estado | Acceso |
|--------|------|--------|--------|
| GET | `/api/review/get-all` | EXISTENTE | Público |
| GET | `/api/review/get-review/{id}` | EXISTENTE | Público |
| POST | `/api/review/registration-review` | EXISTENTE A MODIFICAR: `userId` se toma del token e ignora el del body | Autenticado |
| PUT | `/api/review/update-review/{id}` | EXISTENTE | ADMIN |
| DELETE | `/api/review/delete-review/{id}` | EXISTENTE | ADMIN |

---

## 9. Usuarios

`UserDTO` según §1. Ningún endpoint de esta sección permite cambiar `role` (`updateUser` ya lo ignora; se fija con un test). En `update-user`, un **USER** no puede cambiar `username` (es la identidad del login y el `sub` del JWT): el campo se ignora. Un ADMIN sí puede; si el email ya existe → `409 USERNAME_ALREADY_EXISTS` (no 500), y el usuario afectado debe volver a iniciar sesión.

| Método | Ruta | Estado | Acceso |
|--------|------|--------|--------|
| GET | `/api/user/current` | EXISTENTE A MODIFICAR (§2.3) | Autenticado |
| GET | `/api/user/get-all` | EXISTENTE | ADMIN |
| GET | `/api/user/get-user/{id}` | EXISTENTE | ADMIN o dueño |
| POST | `/api/user/registration-user` | EXISTENTE (crea siempre `USER`) | ADMIN |
| PUT | `/api/user/update-user/{id}` | EXISTENTE | ADMIN o dueño |
| DELETE | `/api/user/delete-user/{id}` | EXISTENTE | ADMIN |

---

## 10. Analítica de previsión (HU-03)

### 10.1 `GET /api/analytics/demand-forecast?horizon=6&hotelId={id}` — ADMIN · **NUEVO**

- `horizon` 1–12 (por defecto 6); fuera de rango → `400 INVALID_PARAMETER`. `hotelId` opcional (sin él, todos).
- Ventana: los 24 meses completos anteriores al mes actual. El pronóstico empieza en el mes actual.
- Algoritmo, redondeos y reglas: **[ADR-004](adr/ADR-004-algoritmo-prevision-demanda.md)** (normativo). `suggestedPricePerNight` es entero (HALF_UP).

Response `200` (reloj fijo `2027-01-15T10:00`, caso de prueba del ADR-004; valores de `history[0]` y `bookedRoomNights` ilustrativos):
```json
{
  "generatedAt": "2027-01-15T10:00:00", "hotelId": null, "horizon": 6,
  "activeRooms": 10, "averagePricePerNight": 100.00,
  "historyFrom": "2025-01", "historyTo": "2026-12",
  "model": { "type": "LINEAR_TREND_MONTHLY_SEASONALITY", "intercept": 0.5350, "slope": 0.0100,
             "seasonalIndices": [ { "month": 1, "index": -0.0950 }, "..." ] },
  "history": [ { "month": "2025-01", "occupiedRoomNights": 140, "availableRoomNights": 310,
                 "occupancyRate": 0.4516, "trendRate": 0.5450 }, "..." ],
  "forecast": [
    { "month": "2027-01", "forecastOccupancyRate": 0.6900, "forecastRoomNights": 214, "availableRoomNights": 310,
      "bookedRoomNights": 35, "bookedOccupancyRate": 0.1129, "priceAdjustmentPct": 0,
      "suggestedPricePerNight": 100, "inventoryAction": "KEEP", "recommendedActiveRooms": 10 },
    { "month": "2027-05", "forecastOccupancyRate": 0.8900, "forecastRoomNights": 276, "availableRoomNights": 310,
      "bookedRoomNights": 0, "bookedOccupancyRate": 0.0000, "priceAdjustmentPct": 15,
      "suggestedPricePerNight": 115, "inventoryAction": "INCREASE", "recommendedActiveRooms": 12 },
    "..."
  ]
}
```
Errores: `400 INVALID_PARAMETER`, `401`, `403`, `404 HOTEL_NOT_FOUND`, `422 INSUFFICIENT_HISTORY`.

---

## 11. Tarjetas de prueba (pasarela simulada)

| Número | Marca | CVV | Resultado |
|--------|-------|-----|-----------|
| 4242 4242 4242 4242 | VISA | 3 díg. | Aprobada |
| 5555 5555 5555 4444 | MASTERCARD | 3 díg. | Aprobada |
| 3782 822463 10005 | AMEX | 4 díg. | Aprobada |
| 4000 0000 0000 0002 | VISA | 3 díg. | Se tokeniza; el **cobro** responde `402 PAYMENT_DECLINED` |
| 4242 4242 4242 4241 | — | — | `422 CARD_INVALID` (Luhn) |
| 6011 1111 1111 1117 | Discover | — | `422 CARD_BRAND_UNSUPPORTED` |

Marca: VISA = empieza por `4`, longitud 13/16/19; MASTERCARD = `51`–`55` o `2221`–`2720`, 16; AMEX = `34`/`37`, 15.

---

## 12. Resumen de endpoints

Existentes sin cambios de forma (solo acceso y errores): `/api/hotel/*`, `/api/room/{get-room,registration-room,update-room,delete-room}`, `/api/reservation/delete-reservation`, `/api/payment/{registration-payment,cancel-payment}`, `/api/review/*` (salvo `registration-review`), `/api/user/*` (salvo `current`).

| Ruta | Estado | HU |
|------|--------|----|
| `POST /auth/login` | MODIFICAR (401/400 con código; secreto externo) | HU-04 |
| `POST /auth/registration-user` | MODIFICAR (validación, 409, rol forzado) | HU-04 |
| `GET /api/user/current` | MODIFICAR (401 sin token) | HU-04 |
| `GET /api/room/get-all` | MODIFICAR (`activeOnly`) | HU-02 |
| `GET /api/room/get-availability/{id}` | NUEVO | HU-02 |
| `/api/reservation/{get-all,get-reservation,registration-reservation,update-reservation}` | MODIFICAR (`LocalDate`, solapamiento, ADMIN) | HU-02 |
| `GET /api/payment-method/get-all` | NUEVO | HU-01 |
| `POST /api/payment-method/registration-payment-method` | NUEVO | HU-01 |
| `PUT /api/payment-method/set-default/{id}` | NUEVO | HU-01 |
| `DELETE /api/payment-method/delete-payment-method/{id}` | NUEVO | HU-01 |
| `POST /api/reservation/book` | NUEVO | HU-01 |
| `GET /api/reservation/my-reservations/{id}` | NUEVO | HU-01 |
| `/api/payment/{get-all,get-payment-details}` | MODIFICAR (campos aditivos) | HU-01 |
| `POST /api/review/registration-review` | MODIFICAR (`userId` del token) | HU-04 |
| `GET /api/analytics/demand-forecast` | NUEVO | HU-03 |
| `GET /api/reservation/my-reservations` | NUEVO | HU-05 |
| `PUT /api/reservation/cancel-reservation/{id}` | NUEVO | HU-05 |

---

## 13. Matriz RBAC

Leyenda: ✔ permitido · ✘ denegado (anónimo → 401, autenticado → 403) · **D** = solo el dueño (ajeno → 404). "USER" significa cualquier autenticado; ADMIN hereda todo lo de USER.

| Método y ruta | Público | USER | ADMIN |
|---------------|:------:|:----:|:-----:|
| `POST /auth/login`, `POST /auth/registration-user` | ✔ | ✔ | ✔ |
| `GET /api/hotel/get-all`, `GET /api/hotel/get-hotel/{id}` | ✔ | ✔ | ✔ |
| `POST/PUT/DELETE /api/hotel/*` | ✘ | ✘ | ✔ |
| `GET /api/room/get-all`, `GET /api/room/get-room/{id}`, `GET /api/room/get-availability/{id}` | ✔ | ✔ | ✔ |
| `POST/PUT/DELETE /api/room/*` | ✘ | ✘ | ✔ |
| `GET /api/review/get-all`, `GET /api/review/get-review/{id}` | ✔ | ✔ | ✔ |
| `POST /api/review/registration-review` | ✘ | ✔ | ✔ |
| `PUT/DELETE /api/review/*` | ✘ | ✘ | ✔ |
| `GET /api/user/current` | ✘ | ✔ | ✔ |
| `GET /api/user/get-user/{id}`, `PUT /api/user/update-user/{id}` | ✘ | D | ✔ |
| `GET /api/user/get-all`, `POST /api/user/registration-user`, `DELETE /api/user/delete-user/{id}` | ✘ | ✘ | ✔ |
| `/api/payment-method/*` | ✘ | D | D |
| `POST /api/reservation/book`, `GET /api/reservation/my-reservations` | ✘ | ✔ | ✔ |
| `GET /api/reservation/my-reservations/{id}`, `PUT /api/reservation/cancel-reservation/{id}` | ✘ | D | D |
| `GET /api/reservation/get-all`, `GET /api/reservation/get-reservation/{id}`, `POST registration-reservation`, `PUT update-reservation/{id}`, `DELETE delete-reservation/{id}` | ✘ | ✘ | ✔ |
| `/api/payment/*` | ✘ | ✘ | ✔ |
| `GET /api/analytics/demand-forecast` | ✘ | ✘ | ✔ |
| `OPTIONS /**`, `/error`, `/h2-console/**` (solo perfil `dev`) | ✔ | ✔ | ✔ |
| Cualquier otra ruta | ✘ | ✔ (→ 404) | ✔ (→ 404) |

En `SecurityConfig` cada ruta pública se lista explícitamente (se elimina el `requestMatchers(HttpMethod.GET).permitAll()` actual). "D" se implementa en el servicio comparando con el usuario del token, no con `@PreAuthorize`.
