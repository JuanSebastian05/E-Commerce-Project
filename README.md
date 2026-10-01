# TechStore — E-commerce de productos tecnológicos

Plataforma e-commerce construida por módulos sobre una arquitectura **Modular Monolith + REST API**.

| Capa | Tecnología |
|------|------------|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Bean Validation, Flyway, OpenAPI |
| Frontend | React, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS |
| Base de datos | PostgreSQL 16 |
| Pruebas | JUnit 5, Mockito, Testcontainers |

## Estructura del repositorio

```
.
├── backend/             API REST (Spring Boot)
├── frontend/            Storefront (/) y Backoffice (/admin) en React
├── docs/                Documentación técnica y ADRs
└── docker-compose.yml   PostgreSQL para desarrollo local
```

## Requisitos

- Java 21
- Node.js 20 o superior
- Docker y Docker Compose

Maven no hace falta instalarlo: el backend incluye el wrapper `./mvnw`.

## Cómo ejecutarlo en local

1. **Configuración y base de datos.** Desde la raíz del repositorio:

   ```bash
   cp .env.example .env
   # Edita .env: rellena JWT_SECRET (openssl rand -base64 48) y ADMIN_PASSWORD
   docker compose up -d
   ```

2. **Backend** (puerto 8080). Flyway aplica las migraciones al arrancar.

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   - Estado: <http://localhost:8080/api/v1/health>
   - Swagger UI: <http://localhost:8080/swagger-ui.html>

   El backend no arranca si falta `JWT_SECRET`. En el primer arranque crea el usuario ADMIN con `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Para probarlo, haz login en `POST /api/v1/auth/login` y pulsa **Authorize** en Swagger UI con el `accessToken`.

3. **Frontend** (puerto 5173). En otra terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   - Storefront: <http://localhost:5173/>
   - Backoffice: <http://localhost:5173/admin>

   Si todo está bien, ambas páginas muestran `API: UP · Base de datos: UP`.

### Variables de entorno del backend

| Variable | Valor por defecto |
|----------|-------------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME` | `ecommerce` |
| `DB_PASSWORD` | `ecommerce` |
| `SERVER_PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `JWT_SECRET` | sin valor: obligatoria, mínimo 32 caracteres |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | sin valor: si faltan, no se crea el ADMIN inicial |
| `REFRESH_COOKIE_SECURE` | `true` |

Las variables se leen del entorno o del archivo `.env` de la raíz. Los valores por defecto son solo para desarrollo.

## Pruebas

```bash
cd backend && ./mvnw verify        # requiere Docker (Testcontainers levanta PostgreSQL)
cd frontend && npm run lint && npm run build
```

## Flujo de trabajo (Git Flow)

| Rama | Uso |
|------|-----|
| `main` | Código estable y entregado. Solo recibe merges desde `develop` (o `hotfix/*`). |
| `develop` | Integración de funcionalidades terminadas. Base de todas las ramas de trabajo. |
| `feature/<nombre>` | Una funcionalidad pequeña. Sale de `develop` y vuelve a `develop` mediante PR. |
| `hotfix/<nombre>` | Corrección urgente sobre `main`; se fusiona en `main` y en `develop`. |

Reglas:

- Nunca se hace commit directo en `main` ni en `develop`: todo entra por pull request con la CI en verde.
- Cada PR contiene una funcionalidad pequeña y su documentación actualizada.
- Cuando `develop` tiene un conjunto de módulos estable, se abre un PR de `develop` a `main`.

## Documentación

- [Arquitectura](docs/architecture/architecture.md)
- [Decisiones de arquitectura (ADRs)](docs/architecture/decisions/)
- [Historias de usuario](docs/requirements/user-stories.md) y [reglas de negocio](docs/requirements/business-rules.md)
- [API](docs/backend/api.md) y [seguridad](docs/backend/security.md)
- [Diseño de la base de datos](docs/database/database-design.md) y [ERD](docs/database/erd.md)
- [Migraciones de base de datos](docs/database/migrations.md)

## Estado de los módulos

| Módulo | Estado |
|--------|--------|
| 0. Base del proyecto | ✅ |
| Catalog | Pendiente |
| Auth / Users | En curso: falta la administración de roles y el frontend |
| Inventory, Cart, Orders, Payments, Shipping | Pendiente |
| Notifications, Reviews, Administration, Audit | Pendiente |
