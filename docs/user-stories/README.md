# Backlog del sprint — Proyecto 1 (Desarrollo asistido por IA)

Sistema de reservas de hotel (fork de `natanael-lima/hotel-reservation-system`). Orden de implementación: **HU-00 → HU-04 → HU-02 → HU-01 → HU-03 → HU-05**.
Arquitectura: [ARCHITECTURE.md](../architecture/ARCHITECTURE.md) · Contrato: [API-CONTRACT.md](../architecture/API-CONTRACT.md) · Decisiones: [ADRs](../architecture/adr/)

## Backlog

| Orden | ID | Título | Origen | Prioridad | Puntos | Dependencias | Rama | Estado |
|-------|----|--------|--------|-----------|--------|--------------|------|--------|
| 1 | [HU-00](HU-00.md) | Base técnica: build, perfiles, CI y manejo de errores | Habilitadora | Must | 5 | — | `feature/HU-00-base-tecnica` | Pendiente |
| 2 | [HU-04](HU-04.md) | Registro e inicio de sesión seguros | Extra (equipo) | Must | 5 | HU-00 | `feature/HU-04-autenticacion-jwt` | Pendiente |
| 3 | [HU-02](HU-02.md) | Calendario dinámico de disponibilidad | Obligatoria | Must | 5 | HU-00 | `feature/HU-02-calendario-disponibilidad` | Pendiente |
| 4 | [HU-01](HU-01.md) | Reservar con mis datos de pago guardados | Obligatoria | Must | 8 | HU-04, HU-02 | `feature/HU-01-pago-datos-guardados` | Pendiente |
| 5 | [HU-03](HU-03.md) | Gráficos de previsión de demanda | Obligatoria | Must | 8 | HU-04, HU-02 | `feature/HU-03-prevision-demanda` | Pendiente |
| 6 | [HU-05](HU-05.md) | Historial de mis reservas y cancelación | Extra (equipo) | Should | 5 | HU-04, HU-01, HU-02 | `feature/HU-05-mis-reservas-cancelacion` | Pendiente |
| | | | | **Total** | **36** | | | |

Nota: `feature/HU-00-base-tecnica` ya tiene 5 commits (ver [HU-00 § Estado de la rama](HU-00.md#estado-de-la-rama-2026-10-28)) y `feature/HU-04-autenticacion-jwt` tiene cambios en curso. El estado pasa a "Hecho" solo cuando el PR se fusiona a `develop` cumpliendo la DoD.

## Trazabilidad

### Endpoints → HU

| Endpoint | HU |
|----------|----|
| `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | HU-04 |
| `GET /api/rooms`, `GET /api/rooms/{id}`, `GET /api/rooms/{id}/availability` | HU-02 |
| `GET/POST /api/payment-methods`, `DELETE /api/payment-methods/{id}`, `PATCH /api/payment-methods/{id}/default`, `POST /api/reservations`, `GET /api/reservations/{id}` | HU-01 |
| `GET /api/admin/analytics/demand-forecast` | HU-03 |
| `GET /api/reservations/me`, `POST /api/reservations/{id}/cancel` | HU-05 |

### Cambios de modelo → HU

Cada campo lo añade la primera HU que lo necesita. Las HU posteriores no editan esa entidad salvo para añadir consultas ([ARCHITECTURE §4.2](../architecture/ARCHITECTURE.md#42-cambios-por-entidad-y-hu-que-los-introduce)).

| HU | Entidades |
|----|-----------|
| HU-00 | Ninguna (solo `ApiResponse` + `code`/`errors`, `Clock`, esqueleto del seeder) |
| HU-04 | `User.fullName`, `User.phone`, `User.role` (`Role`) |
| HU-02 | `Room.active` (reemplaza `availbility`), `capacity`, `description`, `imageUrl`, `pricePerNight` → `BigDecimal`; `Reservation.checkIn/checkOut` (`LocalDate`), `guests`, `totalAmount`, `status` (`ReservationStatus`) |
| HU-01 | `PaymentMethod` (nueva); `Payment.amount` → `BigDecimal`, `status` (`PaymentStatus`), `cardBrand`, `cardLast4`, `transactionId`, `refundedAmount` |
| HU-03 | Ninguna (solo consultas + histórico semilla) |
| HU-05 | `Reservation.cancelledAt` |

## Flujo de trabajo por HU

1. **Developer** crea la rama `feature/HU-0X-<slug>` desde `develop` y, si hace falta cambiar el contrato, primero edita `API-CONTRACT.md` en un PR.
2. Implementa con IA y registra los prompts relevantes, los errores de la IA y los cambios rechazados en `docs/ai-log/`.
3. Abre un PR a `develop` con: descripción, `Refs: HU-0X`, tabla CA → test, capturas y entradas del AI log.
4. **Reviewer** revisa el código contra el contrato, los ADR y las reglas de seguridad (ARCHITECTURE §5). **QA** ejecuta el checklist manual de la HU con el perfil `dev`.
5. Merge solo con CI en verde y aprobación. Los roles rotan en cada HU.

## Definición de Hecho común

- [ ] Todos los criterios de aceptación de la HU tienen al menos un test automatizado nombrado en su "Plan de pruebas" y en verde en CI (`backend` y/o `frontend`).
- [ ] `./mvnw test`, `npm test` y `npm run build` en verde en local y en GitHub Actions.
- [ ] Las respuestas de la API coinciden con [API-CONTRACT.md](../architecture/API-CONTRACT.md) (estado HTTP, forma y `code`).
- [ ] Sin `LocalDate.now()` sin `Clock`, sin SQL nativo, sin datos de tarjeta o contraseñas en logs ni en respuestas.
- [ ] Mensajes al usuario en español; estados de carga, vacío y error en cada pantalla nueva.
- [ ] PR revisado por el rol Reviewer y checklist manual ejecutado por QA, con evidencia en el PR.
- [ ] Commits identificables (`tipo(HU-0X): ...`) y entrada en el AI Development Log si la IA influyó de forma significativa.
- [ ] Documentación actualizada si cambió el contrato, el modelo o una decisión (ADR nuevo o actualizado).
