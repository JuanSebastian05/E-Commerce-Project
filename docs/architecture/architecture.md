# Arquitectura

## Estilo

**Modular Monolith + REST API** (ver [ADR-001](decisions/ADR-001-modular-monolith.md)).
Una sola aplicación Spring Boot desplegable, dividida en módulos funcionales con fronteras claras.

## Módulos previstos

Auth, Users, Catalog, Inventory, Cart, Orders, Payments, Shipping, Notifications, Reviews, Administration, Audit.

## Organización del backend

```
com.ecommerce
├── shared/                código transversal, sin lógica de negocio
│   ├── config/            seguridad, OpenAPI
│   ├── persistence/       entidad base (UUID, fechas, versión)
│   ├── web/               manejo global de errores (Problem Details, RFC 9457)
│   └── health/            GET /api/v1/health
└── <modulo>/              un paquete por módulo, añadido cuando se implemente
    ├── api/               controladores REST y DTOs
    ├── application/       servicios (casos de uso) y contratos públicos del módulo
    ├── domain/            entidades y reglas de negocio
    └── infrastructure/    repositorios y adaptadores
```

Reglas:

- Un módulo solo usa otro módulo a través de sus servicios o interfaces públicas, nunca de sus entidades o repositorios.
- `shared` no depende de ningún módulo.
- Todas las rutas de la API cuelgan de `/api/v1/`.

## Organización del frontend

```
src
├── app/          providers (TanStack Query) y router
├── storefront/   tienda para clientes, ruta / (login en /login, registro en /registro)
├── backoffice/   administración, ruta /admin
└── shared/       cliente HTTP, sesión (auth/) y componentes comunes
```

El estado global de la sesión vive en un store de Zustand; los datos del servidor, en TanStack Query. Detalles en [sesión en el frontend](../frontend/session.md).

## Seguridad

- API stateless con JWT: access token de 15 minutos y refresh token rotado en una cookie `httpOnly` ([ADR-003](decisions/ADR-003-autenticacion-y-autorizacion.md)).
- Por defecto **todo endpoint exige autenticación**; son públicos `/api/v1/health`, la documentación OpenAPI y registro, login, refresh y logout.
- Autorización por permisos con `@PreAuthorize`; el frontend oculta lo que no se puede hacer, pero quien decide es el backend.
- CORS restringido a los orígenes de `CORS_ALLOWED_ORIGINS`.
- Los errores no exponen trazas ni mensajes internos.

Detalles en [security.md](../backend/security.md).
