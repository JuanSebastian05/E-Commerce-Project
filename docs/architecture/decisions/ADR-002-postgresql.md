# ADR-002 — PostgreSQL y Flyway

- **Estado:** Aceptada
- **Fecha:** 2026-10-01

## Contexto

El dominio es transaccional (inventario, pedidos, pagos) y necesita integridad referencial y control de concurrencia. El catálogo, además, tendrá atributos distintos por tipo de producto.

## Problema

Elegir el motor de base de datos y la forma de evolucionar su esquema.

## Alternativas consideradas

1. **MySQL/MariaDB.** Relacional y conocido, pero con menos soporte para check constraints complejas y tipos como `jsonb`.
2. **MongoDB.** Flexible para atributos dinámicos, pero débil en integridad referencial y transacciones entre documentos, que son centrales en pedidos e inventario.
3. **PostgreSQL.** Relacional, ACID, constraints ricas, índices parciales y `jsonb` si un módulo lo justifica.

## Decisión

PostgreSQL 16 como única base de datos. El esquema evoluciona solo mediante migraciones Flyway versionadas; Hibernate se configura en `ddl-auto: validate` y nunca crea ni altera tablas.

## Consecuencias

- Integridad garantizada en la base de datos (PK, FK, UNIQUE, CHECK), no solo en el código.
- Las pruebas de integración usan PostgreSQL real con Testcontainers, no H2, para que el comportamiento coincida con producción.
- Una migración aplicada no se edita: cualquier cambio es una migración nueva.
