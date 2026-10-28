# HU-04 — Control de acceso por roles (extra 1): registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · mergeada** |
| Issue | #5 |
| Pull request | #9 (fix #10) |
| Rama | `feature/HU-04-control-acceso-roles` |
| Especificación | [../user-stories/HU-04.md](../user-stories/HU-04.md) |

## Qué se implementó
- `SecurityConfig` con la matriz RBAC (API-CONTRACT §13); recurso ajeno → 404.
- JWT con claim `uid` y secreto Base64 por configuración.
- `CurrentUserService`, `AdminBootstrap`, `DevDataSeeder` (admin/demo).
- Frontend: `AuthContext`, `ProtectedRoute requiredRole`, `AdminLoginPage`, interceptor 401/403.
- Fix SonarCloud S7044/S8476: `toPathId` para ids en URLs.

## Pruebas
- AccessControlTest, LoginEnumerationTest, JwtSecretConfigTest, AdminBootstrapTest, pathId.test.

## Pendiente
- Ninguno.
