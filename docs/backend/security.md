# Seguridad

Decisiones en [ADR-003](../architecture/decisions/ADR-003-autenticacion-y-autorizacion.md).

## Autenticación

- `POST /api/v1/auth/login` comprueba email y contraseña y devuelve un access token JWT (HS256, 15 minutos).
- El token lleva `iss`, `sub` (id del usuario), `iat`, `exp` y `permissions`. Se rechaza si la firma, el emisor o la expiración no son válidos.
- La clave de firma llega por `JWT_SECRET` (mínimo 32 caracteres). Sin ella la aplicación no arranca.
- Las contraseñas se guardan con BCrypt (coste 12). Política: mínimo 8 caracteres, máximo 72 bytes, al menos una letra y un número (RN-02).
- Credenciales incorrectas, email inexistente y usuario desactivado devuelven el mismo `401` con el mismo mensaje. Si el email no existe, igualmente se calcula un BCrypt, para que el tiempo de respuesta no revele qué emails están registrados.

## Autorización

- Por permisos, nunca por nombre de rol (RN-05): `@PreAuthorize("hasAuthority('users:read')")`.
- Los permisos del token son la unión de los de todos los roles del usuario en el momento del login.
- `/users/me` lee roles y permisos de la base de datos, no del token, para mostrar siempre el estado actual.

## Primer administrador

Al arrancar, si no existe ningún usuario con rol ADMIN y están definidas `ADMIN_EMAIL` y `ADMIN_PASSWORD`, se crea uno. Si ya existe, no se modifica nada.

## Otras medidas

- API stateless, sin sesiones ni cookies de sesión; CSRF desactivado por ese motivo.
- CORS limitado a `CORS_ALLOWED_ORIGINS`.
- Las respuestas de error nunca incluyen trazas ni mensajes internos; las respuestas de usuario nunca incluyen el hash de la contraseña.

## Pruebas de seguridad (`AuthApiIntegrationTest`)

- Acceso sin token, con token manipulado o con un texto que no es JWT → `401`.
- Token firmado con otra clave que declara permisos de ADMIN → `401`.
- Mismo `401` para contraseña incorrecta, email inexistente y usuario desactivado.
- El registro nunca devuelve la contraseña ni su hash, y el hash guardado es BCrypt.

## Pendiente

Refresh token en cookie `httpOnly` con rotación, logout y rate limit del login (PR 3).
