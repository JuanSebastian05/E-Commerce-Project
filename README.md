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

1. **Base de datos.** Desde la raíz del repositorio:

   ```bash
   cp .env.example .env      # opcional: ajusta usuario, contraseña o puerto
   docker compose up -d
   ```

2. **Backend** (puerto 8080). Flyway aplica las migraciones al arrancar.

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   - Estado: <http://localhost:8080/api/v1/health>
   - Swagger UI: <http://localhost:8080/swagger-ui.html>

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

Los valores por defecto son solo para desarrollo. En cualquier otro entorno se deben definir por variable de entorno.

## Pruebas

```bash
cd backend && ./mvnw verify        # requiere Docker (Testcontainers levanta PostgreSQL)
cd frontend && npm run lint && npm run build
```

## Documentación

- [Arquitectura](docs/architecture/architecture.md)
- [Decisiones de arquitectura (ADRs)](docs/architecture/decisions/)
- [Migraciones de base de datos](docs/database/migrations.md)

## Estado de los módulos

| Módulo | Estado |
|--------|--------|
| 0. Base del proyecto | ✅ |
| Catalog | Pendiente |
| Auth / Users | Pendiente |
| Inventory, Cart, Orders, Payments, Shipping | Pendiente |
| Notifications, Reviews, Administration, Audit | Pendiente |
