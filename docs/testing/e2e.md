# Pruebas E2E (Playwright)

Decisión D6 del módulo Auth + Users: las pruebas de extremo a extremo usan [Playwright](https://playwright.dev) y están en `frontend/e2e/`. Recorren el frontend en un navegador real contra el backend y la base de datos de verdad, sin simular la API.

## Ejecutarlas en local

1. Arranca PostgreSQL y el backend como indica el [README](../../README.md#cómo-ejecutarlo-en-local).
2. La primera vez, instala el navegador:

   ```bash
   cd frontend
   npx playwright install chromium
   ```

3. Lanza las pruebas. Playwright arranca el frontend (`npm run dev`) si no está ya en marcha:

   ```bash
   npm run test:e2e
   ```

Las pruebas del ADMIN necesitan sus credenciales; sin ellas se omiten:

```bash
E2E_ADMIN_EMAIL=admin@techstore.local E2E_ADMIN_PASSWORD=... npm run test:e2e
```

| Archivo | Qué cubre |
|---------|-----------|
| `auth.spec.ts` | Registro, login, sesión, renovación y cierre de sesión en la tienda. |
| `backoffice.spec.ts` | Guardia del Backoffice y pantallas de Usuarios y Roles según los permisos. |

Cada prueba crea sus propios usuarios y roles con nombres únicos, así que se pueden repetir sobre la misma base de datos. Las pruebas del Backoffice que necesitan al ADMIN se omiten si faltan sus credenciales.

| Variable | Uso |
|----------|-----|
| `E2E_ADMIN_EMAIL` / `E2E_ADMIN_PASSWORD` | Credenciales del ADMIN inicial, para la prueba del acceso al Backoffice. |
| `PLAYWRIGHT_CHROMIUM_PATH` | Opcional: ruta a un Chromium ya instalado, en lugar del que descarga Playwright. |

## En la CI

El job `e2e` de `.github/workflows/ci.yml` levanta PostgreSQL como servicio, empaqueta y arranca el backend, instala Chromium y ejecuta las pruebas. Si fallan, sube el informe de Playwright (con las trazas) y el log del backend como artefactos del workflow.
