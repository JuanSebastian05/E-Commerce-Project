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
├── storefront/   tienda para clientes, ruta /
├── backoffice/   administración, ruta /admin
└── shared/       cliente HTTP y componentes comunes
```

## Seguridad (estado actual)

- API stateless: sin sesiones, sin formulario de login.
- Por defecto **todo endpoint exige autenticación**; solo son públicos `/api/v1/health` y la documentación OpenAPI.
- Sin credenciales se responde `401`. La autenticación JWT llega con el módulo Auth.
- CORS restringido a los orígenes de `CORS_ALLOWED_ORIGINS`.
- Los errores no exponen trazas ni mensajes internos.
