-- Migración base del proyecto (Módulo 0).
--
-- Solo deja la línea base del historial de Flyway. Cada módulo funcional
-- (Users, Catalog, Inventory, ...) añadirá sus propias tablas en migraciones
-- posteriores: V2__..., V3__..., nunca editando una migración ya aplicada.

COMMENT ON SCHEMA public IS 'Esquema principal del e-commerce';
