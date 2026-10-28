# Revisión de PR — HU-04 Control de acceso por roles (backend)

- **Revisor:** agente `senior-architect-backend` (Claude Code), con pruebas de penetración sobre una copia del backend.
- **Rango revisado:** `6ade24a..9e37f4a`; re-sondeo sobre `8d250d6`.
- **Veredicto inicial:** RECHAZAR (3 críticos explotables).
- **Estado tras el fix:** corregidos en `5859d12` y `d4b2dc3`.

| # | Severidad | Hallazgo (comprobado con pruebas) | Resolución | Commit / test |
|---|-----------|-----------------------------------|------------|---------------|
| 1 | Crítico | Mass assignment: el `id` del body en los `create*` hacía merge sobre filas ajenas (un USER sobrescribió la reseña de otro, se adjudicó una reserva y movió un pago) | Los endpoints de alta ignoran el `id` | `d4b2dc3` · `AccessControlTest.elIdDelBodySeIgnoraAlCrear…` |
| 2 | Crítico | Un USER podía usar `registration-payment` y `get-payment-details`, eligiendo `status` y `amount` | `/api/payment/**` solo ADMIN (matriz §13) | `5859d12` · `AccessControlTest` |
| 3 | Crítico | Un USER podía cambiar su `username`; con `sub = username`, un token viejo servía para otra cuenta registrada con ese email (toma de cuenta); con duplicado devolvía 500 con el texto SQL | Un USER no cambia su username; un ADMIN que duplica recibe 409; el JWT lleva `uid` y se valida | `5859d12`, `d4b2dc3` · `unTokenViejoNoSirve…`, `unUsuarioNoPuedeCambiarSuUsername…` |
| 4 | Advertencia | Reservas, reseñas y pagos heredados quedaban "al dueño" en lugar de solo ADMIN como dice §13 | Alineado con §13 | `5859d12` |
| 5 | Advertencia | Matchers con rutas distintas al contrato (`/api/room/*/availability`, `/api/admin/**`); el test comprobaba una ruta inexistente | Rutas del contrato (`get-availability`, `/api/analytics/**`) | `5859d12` |
| 6 | Advertencia | `get-payment-details` de un pago inexistente devolvía 500 | 404 `PAYMENT_NOT_FOUND` | `d4b2dc3` · `detalleDePagoInexistenteResponde404` |
| 7 | Advertencia | Faltaban tests de: pagos, mass assignment, arranque sin secreto, timing del login, secreto heredado | Añadidos (`JwtSecretConfigTest`, `LoginEnumerationTest`, `AdminBootstrapTest`, regresiones) | `77581a8`, `d4b2dc3` |
| 8 | Sugerencia | `spring.profiles.default=dev` arranca con secreto de desarrollo si no se indica perfil | **Aceptado como riesgo (decisión):** el perfil dev usa H2 en memoria y no expone datos reales; el README documenta las variables obligatorias de producción | README |
| 9 | Sugerencia | Emails semilla distintos de la HU | Alineados (`admin@hotel.com`, `demo@hotel.com`) | `d4b2dc3` |

**Comprobado correcto por el revisor:**
- sin `JWT_SECRET` con el perfil `postgres` la app no arranca;
- un token firmado con la constante heredada da 401;
- el login no enumera usuarios (una sola verificación BCrypt);
- `role` en `update-user` se ignora;
- los trucos de ruta (`//`, `%2D`, `;`, `..`, mayúsculas) nunca dan 200;
- sin BOLA en hoteles, habitaciones ni `/current`.
