# ADR-001 — Autenticación stateless con JWT

- **Estado**: Aceptado (2026-10-28)
- **HU**: HU-04 (habilita HU-01, HU-03, HU-05)

## Contexto

El backend no tiene autenticación: `SecurityConfig` está vacío y Spring Security está comentado en `pom.xml`. Las HU-01 y HU-05 manejan datos personales (tarjetas guardadas, reservas) y la HU-03 es solo para administradores. El frontend es una SPA servida por Vite en otro puerto (5173) y llama a la API mediante proxy.

## Decisión

1. Spring Security en modo **stateless** (`SessionCreationPolicy.STATELESS`, CSRF deshabilitado, sin `JSESSIONID`).
2. **JWT HS256** con jjwt 0.12.x (`Jwts.builder()...signWith(key)` / `Jwts.parser().verifyWith(key).build().parseSignedClaims(...)`).
   - `sub` = id del usuario; claims `username` y `role`; `exp` = emisión + `app.jwt.expiration-minutes` (1440).
   - Secreto `app.jwt.secret` (Base64, ≥ 256 bits) desde la variable `JWT_SECRET`. Los perfiles `dev` y `test` traen un valor fijo no productivo; `postgres` no tiene valor por defecto.
3. `JwtAuthenticationFilter` (`OncePerRequestFilter`) valida el token y **carga el usuario desde la BD** en cada petición. El rol efectivo es el de la BD, no el del claim. Si el usuario ya no existe → 401.
4. Contraseñas con **BCrypt** (coste 10), longitud 8–72.
5. Roles `USER` y `ADMIN` (`entity/Role`, guardado como texto). El registro siempre crea `USER`; los `ADMIN` solo vienen del seed.
6. 401 y 403 devuelven el sobre `ApiResponse` con `code` (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`).
7. El frontend guarda el token en `localStorage` (clave `hotelmn.token`), recupera el perfil con `GET /api/auth/me` al cargar y envía `Authorization: Bearer`.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Sesión de servidor + cookie `JSESSIONID` | Requiere CSRF y estado en servidor; el enunciado del repo original ya plantea JWT. |
| JWT en cookie `httpOnly` | Mejor contra XSS, pero obliga a gestionar CSRF y `SameSite` a través del proxy. Coste alto para el sprint. |
| OAuth2 / Keycloak / Google | Infraestructura externa desproporcionada para el alcance académico. |
| RS256 (par de claves) | Solo aporta si otros servicios verifican el token; aquí hay un único backend. |
| Refresh tokens | Más superficie y endpoints; se acepta re-login tras 24 h. |

## Consecuencias

- (+) Escala horizontalmente sin sesión compartida y se prueba con MockMvc enviando la cabecera.
- (+) Al leer el rol de la BD, un cambio de rol o el borrado de un usuario surten efecto en la siguiente petición.
- (−) No hay revocación inmediata del token salvo borrar al usuario. Logout = el cliente descarta el token.
- (−) `localStorage` es legible ante un XSS. Mitigación: React escapa por defecto, se prohíbe `dangerouslySetInnerHTML` y no se cargan scripts de terceros. CSP fuera de alcance.
- (−) Una consulta extra por petición autenticada (carga de usuario). Aceptable a esta escala.
