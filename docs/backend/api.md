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

## Auth

| Método | Ruta | Acceso | HU | Descripción |
|--------|------|--------|----|-------------|
| POST | `/auth/register` | público | HU-01 | Crea un cliente (rol CUSTOMER). Responde `201` con el perfil. |
| POST | `/auth/login` | público | HU-02 | Devuelve `{accessToken, tokenType, expiresIn}`. |

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

Refresh y logout (PR 3), administración de usuarios (PR 4) y de roles y permisos (PR 5). Ver el [diseño aprobado](../requirements/user-stories.md).
