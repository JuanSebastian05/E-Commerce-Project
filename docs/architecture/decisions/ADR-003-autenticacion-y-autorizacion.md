# ADR-003 — Autenticación y autorización

- **Estado:** Aceptada
- **Fecha:** 2026-10-01

## Contexto

La API es stateless (ver [ADR-001](ADR-001-modular-monolith.md)) y la consumen dos frontends: Storefront y Backoffice. El plan exige JWT, RBAC con permisos desacoplados de los roles, hash seguro de contraseñas y rate limiting donde corresponda.

## Problema

Definir cómo se autentican los usuarios, cómo se mantiene la sesión y cómo se decide qué puede hacer cada uno.

## Alternativas consideradas

| Tema | Elegida | Descartada |
|------|---------|------------|
| Sesión | Access token JWT corto + refresh token opaco con rotación | Solo un JWT de larga duración: no se puede revocar. Sesión en servidor: rompe el diseño stateless. |
| Almacenamiento en el navegador | Access token en memoria; refresh token en cookie `httpOnly`, `Secure`, `SameSite=Strict` | Ambos en `localStorage`: los puede leer cualquier script inyectado (XSS). |
| Hash de contraseñas | BCrypt, coste 12, incluido en Spring Security | Argon2id: algo más resistente, pero añade la dependencia BouncyCastle. |
| Autorización | Permisos (`users:read`) comprobados con `@PreAuthorize("hasAuthority(...)")` | Comprobar nombres de rol en el código: obliga a cambiar código para cambiar quién puede hacer qué. |
| Catálogo de permisos | Fijo, creado por migraciones | Editable desde el Backoffice: un permiso que el código no comprueba no tendría efecto. |
| Primer administrador | Se crea al arrancar si no existe ninguno, con `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Usuario fijo en una migración: obliga a dejar una contraseña en el repositorio. |
| Rate limit del login | Limitador en memoria (5 fallos por email e IP cada 15 minutos) | Bucket4j con almacenamiento compartido: añade una dependencia que todavía no hace falta. |

## Decisión

- **Access token:** JWT HS256 de 15 minutos con `sub` = id de usuario y la lista de permisos. El secreto viene de una variable de entorno y se valida con el resource server de Spring Security (Nimbus), sin librerías JWT de terceros.
- **Refresh token:** valor aleatorio de 256 bits, válido 7 días. En la base de datos solo se guarda su hash SHA-256 (`refresh_tokens.token_hash`). Cada uso lo rota; reutilizar uno ya rotado revoca toda su familia (`family_id`), porque indica que fue robado.
- Desactivar un usuario o cambiar sus roles revoca sus refresh tokens. El access token vigente caduca como mucho en 15 minutos.
- Los roles agrupan permisos (`role_permissions`) y los usuarios tienen roles (`user_roles`). Los permisos efectivos son la unión de los de todos sus roles.
- El módulo Auth guarda solo el `user_id` de cada token y accede a los usuarios a través de un contrato público del módulo Users, nunca a sus entidades.

## Consecuencias

- Revocar el acceso tarda como mucho lo que dura el access token (15 minutos). Es un compromiso aceptado frente a consultar la base de datos en cada petición.
- El limitador en memoria solo es correcto con una instancia del backend. Si se escala horizontalmente, hay que pasar a un almacenamiento compartido.
- Cada módulo nuevo añade sus permisos con una migración y los comprueba con `@PreAuthorize`.
