# Backlog del sprint — Proyecto 1 (Desarrollo asistido por IA)

Sistema de reservas de hotel (fork de `natanael-lima/hotel-reservation-system`). **Base: `upstream/developer`** integrada en `develop` (`950bff7`) — [ADR-007](../architecture/adr/ADR-007-base-developer-en-lugar-de-main.md).
Orden de implementación: **HU-00 → HU-04 → HU-02 → HU-01 → HU-03 → HU-05**.
Arquitectura: [ARCHITECTURE.md](../architecture/ARCHITECTURE.md) · Contrato y matriz RBAC: [API-CONTRACT.md](../architecture/API-CONTRACT.md) · Decisiones: [ADRs](../architecture/adr/)

## Backlog

| Orden | ID | Título | Origen | Prioridad | Puntos | Dependencias | Rama | Estado |
|-------|----|--------|--------|-----------|--------|--------------|------|--------|
| 1 | [HU-00](HU-00.md) | Base técnica sobre `developer`: build, perfiles, CI, errores y caracterización | Habilitadora | Must | 5 | — | `feature/HU-00-base-tecnica-v2` | Pendiente |
| 2 | [HU-04](HU-04.md) | Control de acceso por roles | Extra (equipo) | Must | 8 | HU-00 | `feature/HU-04-control-acceso-roles` | Pendiente |
| 3 | [HU-02](HU-02.md) | Calendario dinámico de disponibilidad | Obligatoria | Must | 5 | HU-00, HU-04 | `feature/HU-02-calendario-disponibilidad-v2` | Pendiente |
| 4 | [HU-01](HU-01.md) | Reservar con mis datos de pago guardados | Obligatoria | Must | 8 | HU-04, HU-02 | `feature/HU-01-reserva-datos-guardados` | Pendiente |
| 5 | [HU-03](HU-03.md) | Gráficos de previsión de demanda | Obligatoria | Must | 8 | HU-00, HU-04, HU-02 | `feature/HU-03-prevision-demanda` | Pendiente |
| 6 | [HU-05](HU-05.md) | Historial de mis reservas y cancelación | Extra (equipo) | Should | 5 | HU-04, HU-01, HU-02 | `feature/HU-05-mis-reservas-cancelacion` | Pendiente |
| | | | | **Total** | **39** | | | |

Historia descartada: la HU-04 original ("registro e inicio de sesión JWT") ya existe en `developer`. Su trabajo sobre `main` está en `origin/archive/main-HU-04` y se reutiliza en parte (sobre de error) en HU-00.

## Trazabilidad

### Endpoints → HU

Detalle y estado (EXISTENTE / MODIFICAR / NUEVO) en [API-CONTRACT §12](../architecture/API-CONTRACT.md#12-resumen-de-endpoints).

| Endpoint | Estado | HU |
|----------|--------|----|
| Todos los existentes (caracterización + sobre de error) | EXISTENTE | HU-00 |
| `POST /auth/login`, `POST /auth/registration-user`, `GET /api/user/current`, `POST /api/review/registration-review` + reglas de acceso de todos los endpoints | MODIFICAR | HU-04 |
| `GET /api/room/get-all?activeOnly`, `/api/reservation/{get-all,get-reservation,registration-reservation,update-reservation}` | MODIFICAR | HU-02 |
| `GET /api/room/get-availability/{id}` | NUEVO | HU-02 |
| `/api/payment-method/*` (4), `POST /api/reservation/book`, `GET /api/reservation/my-reservations/{id}` | NUEVO | HU-01 |
| `/api/payment/{get-all,get-payment-details}` (campos aditivos) | MODIFICAR | HU-01 |
| `GET /api/analytics/demand-forecast` | NUEVO | HU-03 |
| `GET /api/reservation/my-reservations`, `PUT /api/reservation/cancel-reservation/{id}` | NUEVO | HU-05 |

### Cambios de modelo → HU

Cada campo lo añade la primera HU que lo necesita ([ARCHITECTURE §4.2](../architecture/ARCHITECTURE.md#42-cambios-por-entidad-y-hu)).

| HU | Entidades |
|----|-----------|
| HU-00 | Ninguna (`ApiResponse` + `code`/`errors`, `Clock`, seed base, CORS) |
| HU-04 | `User.role` → `@Enumerated(STRING)` |
| HU-02 | `Reservation.startDate/endDate` → `LocalDate`; `Reservation.status` → `ReservationStatus`; `Room.availability` redefinido como "activa" (sin cambio de esquema) |
| HU-01 | `PaymentMethod` (nueva); `Reservation.guests`, `totalAmount`; `Payment.cardBrand`, `cardLast4`, `transactionId`, `refundedAmount`; `PaymentStatus` + `REFUNDED` |
| HU-03 | Ninguna (consultas + histórico semilla) |
| HU-05 | `Reservation.cancelledAt` |

### Pantallas de `developer` → HU

| Pantalla | HU |
|----------|----|
| `AdminLoginPage`, `ProtectedRoute`, `AuthContext`, `Header` (modal de login), `services/hotelService.ts`, `services/axiosConfig.ts` | HU-04 |
| `RoomsPages`, `RoomDetailsPage` (+ `AvailabilityCalendar`), `CardRoomWithHotel` | HU-02 |
| `ConfirmBookingModal`, `PaymentMethodsPage` (nueva), `ProfileCircle` | HU-01 |
| `Admin/Dashboard/Dashboard.tsx`, `StatOverview.tsx` | HU-03 |
| `Bookings` | HU-05 |

## Flujo de trabajo por HU

1. **Developer** crea la rama `feature/HU-0X-<slug>` desde `develop`. Si hay que cambiar el contrato, primero edita `API-CONTRACT.md` en un PR.
2. Implementa con IA y registra en `docs/ai-log/` los prompts relevantes, los errores de la IA y los cambios rechazados.
3. Abre un PR a `develop` con descripción, `Refs: HU-0X`, tabla CA → test, capturas y entradas del AI log.
4. **Reviewer** revisa contra el contrato, los ADR y ARCHITECTURE §5. **QA** ejecuta el checklist manual con el perfil `dev`.
5. Merge solo con CI en verde y aprobación. Los roles rotan en cada HU.

## Definición de Hecho común

- [ ] Todos los criterios de aceptación tienen al menos un test automatizado nombrado en el "Plan de pruebas" y en verde en CI (`backend` y/o `frontend`).
- [ ] `./mvnw test`, `npm test` y `npm run build` en verde en local y en GitHub Actions.
- [ ] Las respuestas de la API coinciden con [API-CONTRACT.md](../architecture/API-CONTRACT.md) (estado HTTP, forma y `code`) y con la matriz RBAC (§13).
- [ ] Ninguna ruta ni campo que use el frontend de `developer` cambió sin estar marcado como MODIFICAR en el contrato; los tests de caracterización afectados se actualizaron en el mismo PR, con justificación.
- [ ] Un solo cliente HTTP en el frontend (axios); sin `LocalDate.now()` sin `Clock`; sin SQL nativo; sin datos de tarjeta, contraseñas ni tokens en logs o respuestas.
- [ ] Mensajes al usuario en español; estados de carga, vacío y error en cada pantalla nueva o modificada.
- [ ] PR revisado por el Reviewer y checklist manual de QA, con evidencia en el PR.
- [ ] Commits identificables (`tipo(HU-0X): ...`) y entrada en el AI Development Log si la IA influyó de forma significativa.
- [ ] Documentación actualizada si cambió el contrato, el modelo o una decisión (ADR nuevo o actualizado).
