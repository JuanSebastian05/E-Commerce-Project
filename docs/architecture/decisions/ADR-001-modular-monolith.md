# ADR-001 — Modular Monolith

- **Estado:** Aceptada
- **Fecha:** 2026-10-01

## Contexto

La plataforma empieza con alcance académico, pero debe poder evolucionar hacia un entorno productivo. Tiene muchos dominios (catálogo, pedidos, pagos, envíos, ...) y un equipo pequeño.

## Problema

Elegir un estilo de arquitectura que mantenga los dominios separados sin pagar desde el inicio el coste operativo de un sistema distribuido.

## Alternativas consideradas

1. **Monolito por capas** (controllers / services / repositories globales). Simple, pero los dominios se mezclan y extraer uno después es costoso.
2. **Microservicios.** Aislamiento total, pero exige despliegue, red, consistencia eventual y observabilidad distribuidas que no aportan valor al alcance actual.
3. **Modular Monolith.** Un único despliegue con módulos de fronteras explícitas.

## Decisión

Modular Monolith con API REST versionada (`/api/v1`). Cada módulo es un paquete con sus capas internas y expone solo servicios o interfaces públicas a los demás.

## Consecuencias

- Un solo despliegue, una sola base de datos y transacciones locales: sencillo de operar y probar.
- Las fronteras dependen de disciplina y revisión; si crecen las dependencias cruzadas se podrá añadir una verificación automática (por ejemplo, tests de arquitectura).
- Un módulo podrá extraerse como servicio en el futuro, porque solo se comunica mediante contratos.
