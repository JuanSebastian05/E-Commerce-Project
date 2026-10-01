# Diseño de la base de datos

Motor: PostgreSQL 16 (ver [ADR-002](../architecture/decisions/ADR-002-postgresql.md)). Diagrama en [erd.md](erd.md).

## Convenciones

- Claves primarias `UUID`, generadas por la aplicación (o con `gen_random_uuid()` en los datos semilla).
- Fechas en `TIMESTAMPTZ` (UTC).
- Tablas principales con `created_at`, `updated_at` y `version` para bloqueo optimista.
- Nombres de constraints: `uq_` única, `ck_` check, `ix_` índice, `ux_` índice único.
- Los registros con historia (usuarios) no se borran: se desactivan.

## Módulo Users + Auth

| Tabla | Descripción |
|-------|-------------|
| `users` | Cuentas de clientes y personal. |
| `roles` | Agrupaciones de permisos. `system = true` marca ADMIN, CUSTOMER, SUPPORT y WAREHOUSE. |
| `permissions` | Catálogo fijo de permisos con formato `modulo:accion`. |
| `user_roles` | Roles de cada usuario (N:M). |
| `role_permissions` | Permisos de cada rol (N:M). |
| `refresh_tokens` | Refresh tokens emitidos, guardados solo como hash. |

### Constraints

| Tabla | Constraint | Motivo |
|-------|-----------|--------|
| `users` | `ux_users_email` + `ck_users_email_lowercase` | Email único sin distinguir mayúsculas (RN-01). |
| `users` | `ck_users_first_name_not_blank`, `ck_users_last_name_not_blank` | Nombres obligatorios. |
| `roles` | `uq_roles_name`, `ck_roles_name_format` | Nombre único en MAYÚSCULAS_CON_GUIONES. |
| `permissions` | `uq_permissions_code`, `ck_permissions_code_format` | Código único con formato `modulo:accion`. |
| `user_roles` | FK a `roles` con `ON DELETE RESTRICT` | No se puede borrar un rol que tiene usuarios. |
| `role_permissions` | FK a `permissions` con `ON DELETE RESTRICT` | No se puede borrar un permiso en uso. |
| `refresh_tokens` | `uq_refresh_tokens_token_hash`, `ck_refresh_tokens_hash_format` | Hash SHA-256 en hexadecimal, único. |
| `refresh_tokens` | `ck_refresh_tokens_expiry` | La expiración es posterior a la emisión. |

### Índices

| Índice | Uso |
|--------|-----|
| `ux_users_email` | Login y comprobación de email repetido. |
| `ix_user_roles_role_id` | Usuarios de un rol (filtro del listado, borrado de roles). |
| `ix_role_permissions_permission_id` | Roles que incluyen un permiso. |
| `ix_refresh_tokens_user_id` | Revocar todas las sesiones de un usuario. |
| `ix_refresh_tokens_family_id` | Revocar una familia de tokens al detectar reutilización. |

### Decisiones

- Las relaciones N:M son tablas puras, sin columnas extra, mapeadas con `@ManyToMany`.
- `refresh_tokens` referencia a `users` por FK en la base de datos, pero la entidad `RefreshToken` solo guarda el `user_id`. Así el módulo Auth no depende de las entidades del módulo Users.
- Los permisos y roles de sistema se insertan por migración (V3). No se inserta ningún usuario.
