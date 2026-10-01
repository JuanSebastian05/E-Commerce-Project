# Sesión en el frontend

Cómo el Storefront y el Backoffice inician, mantienen y cierran la sesión. Las decisiones de fondo están en [ADR-003](../architecture/decisions/ADR-003-autenticacion-y-autorizacion.md) y el contrato de la API en [api.md](../backend/api.md).

## Dónde vive cada token (decisión D1)

| Token | Dónde | Por qué |
|-------|-------|---------|
| Access token (15 min) | En memoria, en el store de Zustand (`shared/auth/sessionStore.ts`) | Nunca se guarda en `localStorage`: un script inyectado (XSS) no lo puede leer después. |
| Refresh token (7 días) | Cookie `refresh_token` `HttpOnly`, la gestiona el navegador | JavaScript no puede leerla; solo viaja a `/api/v1/auth`. |

Como el access token está en memoria, al recargar la página se pierde. Por eso, al arrancar, la aplicación llama a `POST /auth/refresh`: si la cookie es válida recibe un access token nuevo y carga el perfil con `GET /users/me`; si no, queda sin sesión.

## Piezas

| Archivo | Qué hace |
|---------|----------|
| `shared/api/client.ts` | Cliente HTTP. Con `auth: true` envía el access token y, ante un `401`, renueva la sesión una vez y repite la petición. Convierte los Problem Details del backend en `ApiError`. |
| `shared/auth/session.ts` | `restoreSession`, `login`, `logout` y `refreshAccessToken`. |
| `shared/auth/sessionStore.ts` | Estado `loading` / `authenticated` / `anonymous`, el access token y el perfil. `useHasPermission` para mostrar u ocultar opciones. |
| `shared/auth/useLogout.ts` | Cierra la sesión, vacía la caché de TanStack Query y vuelve a la tienda. Lo usan la tienda y el Backoffice. |
| `shared/auth/permissions.ts` | Constantes del catálogo de permisos (deben coincidir con la migración V3). |
| `shared/auth/passwordPolicy.ts` | La misma política de contraseña que el backend (RN-02), para avisar antes de enviar. |
| `shared/auth/redirect.ts` | Solo deja volver a rutas internas después del login (evita redirecciones abiertas). |

## Renovaciones sin reutilizar el refresh token

El backend rota el refresh token en cada renovación y, si recibe uno ya usado, revoca todas las sesiones del usuario (HU-03). Dos renovaciones simultáneas con la misma cookie cerrarían la sesión, así que el frontend nunca las hace:

- **En la misma pestaña**, todas las peticiones que reciben un `401` comparten una sola renovación.
- **Entre pestañas**, la renovación se hace dentro de un [Web Lock](https://developer.mozilla.org/docs/Web/API/Web_Locks_API). La segunda pestaña espera a que termine la primera y envía la cookie que esta acaba de recibir.

## Pantallas

- `/login`: email y contraseña. Muestra el mensaje genérico del backend para credenciales incorrectas y, con `429`, cuántos minutos esperar. Acepta `?redirect=/ruta` para volver a donde estaba el usuario.
- `/registro`: nombre, apellido, email y contraseña. Tras crear la cuenta entra directamente.
- Cabecera de la tienda: con sesión muestra el nombre, el enlace al Backoffice si el usuario tiene `backoffice:access` y el botón para cerrar sesión. Ocultar el enlace es solo comodidad: la protección real está en el backend. La guardia del Backoffice está descrita en [backoffice.md](backoffice.md).

## Pruebas

Las pruebas E2E de `frontend/e2e/auth.spec.ts` cubren el registro, el login, la recarga de la página, el cierre de sesión, la renovación tras un `401`, dos pestañas cargando a la vez y la redirección segura. Cómo ejecutarlas: [e2e.md](../testing/e2e.md).
