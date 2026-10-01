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
| GET | `/users` | `users:read` | HU-06 | Listado paginado y ordenado por email. Filtros: `email` (contiene), `role`, `enabled`. Paginación: `page` (desde 0) y `size` (1 a 100, por defecto 20). |
| GET | `/users/{id}` | `users:read` | HU-06 | Detalle de un usuario. |
| POST | `/users` | `users:manage` | HU-06 | Crea un usuario con `roles` (al menos uno). Responde `201` con `Location`. |
| PATCH | `/users/{id}/status` | `users:manage` | HU-06 | `{"enabled": false}` desactiva y revoca sus sesiones; `true` reactiva. |
| PUT | `/users/{id}/roles` | `users:manage` | HU-06 | `{"roles": ["SUPPORT"]}` reemplaza los roles y revoca sus sesiones. |

Las páginas tienen la forma `{content, page, size, totalElements, totalPages}`. Los usuarios se devuelven como `{id, email, firstName, lastName, enabled, roles, createdAt}`, nunca con la contraseña ni su hash.

Errores de negocio (`409`): desactivarse a uno mismo, quitarse a uno mismo el permiso `users:manage`, o dejar el sistema sin ningún usuario activo con `roles:manage` (RN-08). Un rol inexistente devuelve `400`.

## Roles y permisos

| Método | Ruta | Acceso | HU | Descripción |
|--------|------|--------|----|-------------|
| GET | `/roles` | `roles:read` | HU-07 | Roles ordenados por nombre: `{id, name, description, system, permissions}`. |
| POST | `/roles` | `roles:manage` | HU-07 | `{"name": "AUDITOR", "description": "...", "permissions": ["users:read"]}`. El nombre se guarda en mayúsculas. Responde `201`. |
| PUT | `/roles/{id}/permissions` | `roles:manage` | HU-07 | `{"permissions": [...]}` reemplaza los permisos del rol. |
| DELETE | `/roles/{id}` | `roles:manage` | HU-07 | Borra un rol que no sea de sistema y no tenga usuarios. Responde `204`. |
| GET | `/permissions` | `roles:read` | HU-07 | Catálogo fijo de permisos: `{code, description}`. |

Errores de negocio (`409`): nombre de rol repetido, quitar al rol ADMIN `users:manage` o `roles:manage`, borrar un rol de sistema o con usuarios, y dejar el sistema sin nadie con `roles:manage` (RN-08). Un permiso inexistente devuelve `400`.

Los permisos no se crean por API: cada uno corresponde a una comprobación en el código y se añade con una migración (ADR-003).

## Pendiente en el módulo Auth + Users

Frontend: pantallas del Backoffice (PR 7). El login, el registro y la sesión del Storefront ya están; ver [sesión en el frontend](../frontend/session.md).
