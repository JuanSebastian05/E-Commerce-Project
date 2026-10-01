# API REST

Base: `/api/v1`. La especificación completa y actualizada está en Swagger UI (`/swagger-ui.html`) y en `/v3/api-docs`.

## Convenciones

- JSON en peticiones y respuestas.
- Autenticación con `Authorization: Bearer <accessToken>`.
- Errores en formato Problem Details (RFC 9457): `status`, `title`, `detail`. Los errores de validación añaden `errors: [{field, message}]`.

| Código | Cuándo |
|--------|--------|
| 400 | Datos inválidos |
| 401 | Sin token, token inválido o caducado, o credenciales incorrectas |
| 403 | Autenticado pero sin el permiso necesario |
| 404 | Recurso inexistente |
| 409 | Conflicto (por ejemplo, email ya registrado) |
| 429 | Demasiados intentos fallidos de login; incluye la cabecera `Retry-After` en segundos |

## Auth

| Método | Ruta | Acceso | HU | Descripción |
|--------|------|--------|----|-------------|
| POST | `/auth/register` | público | HU-01 | Crea un cliente (rol CUSTOMER). Responde `201` con el perfil. |
| POST | `/auth/login` | público | HU-02 | Devuelve `{accessToken, tokenType, expiresIn}` y la cookie `refresh_token`. |
| POST | `/auth/refresh` | cookie `refresh_token` | HU-03 | Rota el refresh token y devuelve un access token nuevo. |
| POST | `/auth/logout` | cookie `refresh_token` | HU-04 | Revoca la sesión y borra la cookie. Responde `204`. |

La cookie `refresh_token` es `HttpOnly`, `Secure`, `SameSite=Strict`, dura 7 días y solo se envía a `/api/v1/auth`. Refresh y logout no necesitan el access token: se autentican con la cookie, así el cliente puede cerrar sesión aunque su access token haya caducado. Desde otro origen, el frontend debe llamar con `credentials: 'include'`.

Ejemplo:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email": "ana@example.com", "password": "Clave1234"}
```

```json
{"accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 900}
```

## Users

| Método | Ruta | Acceso | HU | Descripción |
|--------|------|--------|----|-------------|
| GET | `/users/me` | autenticado | HU-05 | Perfil, roles y permisos actuales del usuario. |

## Pendiente en el módulo Auth + Users

Administración de usuarios (PR 4) y de roles y permisos (PR 5). Ver el [diseño aprobado](../requirements/user-stories.md).
