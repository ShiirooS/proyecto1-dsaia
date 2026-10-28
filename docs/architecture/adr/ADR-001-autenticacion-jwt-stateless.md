# ADR-001 — Autenticación stateless con JWT y autorización por roles

- **Estado**: Aceptado (2026-10-28), revisado tras [ADR-007](ADR-007-base-developer-en-lugar-de-main.md). La autenticación **ya existe** en `developer`; este ADR fija cómo se endurece y cómo se autoriza.
- **HU**: HU-04 (Control de acceso por roles); habilita HU-01, HU-03 y HU-05.

## Contexto

`developer` ya implementa Spring Security stateless con JWT:
- `jwt/JwtAuthenticationFilter` y `jwt/JwtServiceImp` (jjwt 0.11.5, HS256, `sub` = `username`, 24 h);
- `configuration/ApplicationConfig` (`DaoAuthenticationProvider` + BCrypt);
- `/auth/login` y `/auth/registration-user`;
- el frontend guarda el token en `localStorage['token']` y lo envía con axios.

Pero el secreto está fijo en el código, todo GET es público, no hay reglas por rol, el anónimo recibe 403 y los tokens caducados no se manejan (ARCHITECTURE §2.1, B1–B7).

## Decisión

1. **Se mantiene** el esquema de `developer`: stateless, CSRF deshabilitado, Bearer en cabecera, jjwt 0.11.5 (subir a 0.12 no aporta al sprint), `sub` = `username` (email), sin claim de rol.
2. **Rol desde la BD en cada petición.** El filtro carga el `User` por `username`; las autoridades son `ADMIN`/`USER` **sin prefijo `ROLE_`**. Por eso se usa `hasAuthority("ADMIN")` en `SecurityConfig` y `@WithMockUser(authorities = "ADMIN")` en los tests. Un token con un claim de rol añadido o alterado no concede nada.
3. `RoleType` se persiste con `@Enumerated(EnumType.STRING)` (hoy es ORDINAL).
4. **Secreto externo**: `app.jwt.secret` ← `JWT_SECRET` (Base64, ≥ 256 bits). Los perfiles `dev`/`test` traen un valor fijo no productivo; `postgres` no tiene valor por defecto y la app no arranca sin él. Expiración en `app.jwt.expiration-minutes` (1440). `JwtServiceImp` recibe el `Clock` (`setClock` del parser) para probar la caducidad.
5. **Filtro tolerante**: captura `JwtException` (caducado, firma o formato inválidos) y `UsernameNotFoundException`, no autentica y deja seguir la cadena. En ruta protegida → 401; en ruta pública → petición anónima.
6. **Respuestas**: `AuthenticationEntryPoint` → 401 `UNAUTHORIZED`; `AccessDeniedHandler` → 403 `FORBIDDEN`; ambos con el sobre `ApiResponse`. `/error` permitido.
7. **Autorización**: matriz de [API-CONTRACT §13](../API-CONTRACT.md#13-matriz-rbac). Rutas públicas listadas una a una; ADMIN por ruta con `hasAuthority`; "dueño" en el servicio (ajeno → 404).
8. **Alta de ADMIN** solo por el seed `dev` o por `AdminBootstrap` (`ADMIN_USERNAME`/`ADMIN_PASSWORD`) en `postgres`. Registro y alta de usuarios fuerzan `USER`.
9. **Frontend**: `AuthContext` obtiene el usuario con `GET /api/user/current`; `ProtectedRoute` acepta un `role`; `AdminLoginPage` rechaza cuentas no ADMIN; el interceptor de axios cierra la sesión ante un 401.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Añadir el rol como claim y confiar en él | Un cambio de rol no surtiría efecto hasta caducar el token; no evita la consulta del usuario, que el filtro ya hace. |
| Prefijo `ROLE_` + `hasRole` | Obliga a tocar `User.getAuthorities()` y los datos; `hasAuthority` funciona con lo que hay. |
| `@PreAuthorize` en cada método | La matriz quedaría repartida por los controladores; en `SecurityConfig` se revisa de un vistazo y se prueba con un test parametrizado. |
| JWT en cookie `httpOnly` | Exige CSRF y cambiar `authService`/`axiosConfig`; fuera de alcance. |
| Sesión de servidor, OAuth2/Keycloak | Contradicen la base existente o son desproporcionados. |

## Consecuencias

- (+) Se cierra la fuga de datos por GET públicos y la escalada vía rutas de escritura, sin cambiar las rutas que usa el frontend.
- (+) Los tests negativos (caducado, otra firma, claim de rol manipulado, usuario borrado) quedan deterministas gracias al `Clock`.
- (−) No hay revocación inmediata del token salvo borrar el usuario; logout = el cliente borra `localStorage['token']`.
- (−) `localStorage` es legible ante un XSS (mitigación: React escapa por defecto y no se usa `dangerouslySetInnerHTML`).
- (−) Cambiar ORDINAL→STRING obliga a recrear el volumen de Postgres o a migrar la columna `role` a mano.
