# Migraciones (Flyway)

Las migraciones viven en `backend/src/main/resources/db/migration` y se aplican automáticamente al arrancar el backend.

## Convención de nombres

```
V<número>__<descripcion_en_snake_case>.sql
```

Ejemplo: `V2__create_categories_table.sql`.

## Reglas

- Nunca editar una migración ya aplicada en algún entorno; crear una nueva.
- Una migración por cambio lógico, con el nombre del módulo o tabla que afecta.
- Toda tabla nueva define su PK, FKs, constraints e índices en la misma migración.
- Hibernate valida el esquema (`ddl-auto: validate`); no lo genera.

## Historial

| Versión | Descripción | Módulo |
|---------|-------------|--------|
| V1 | Línea base, sin tablas | 0. Base |
