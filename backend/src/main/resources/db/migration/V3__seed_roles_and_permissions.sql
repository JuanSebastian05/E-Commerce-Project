-- Catálogo inicial de permisos y roles de sistema.
-- No crea usuarios: el primer ADMIN se crea al arrancar desde variables de entorno.

INSERT INTO permissions (code, description) VALUES
    ('backoffice:access', 'Entrar al Backoffice'),
    ('users:read',        'Consultar usuarios'),
    ('users:manage',      'Crear, activar, desactivar usuarios y asignarles roles'),
    ('roles:read',        'Consultar roles y permisos'),
    ('roles:manage',      'Crear y borrar roles y asignarles permisos');

INSERT INTO roles (name, description, system) VALUES
    ('ADMIN',     'Acceso administrativo completo',         TRUE),
    ('CUSTOMER',  'Cliente de la tienda',                   TRUE),
    ('SUPPORT',   'Atención al cliente',                    TRUE),
    ('WAREHOUSE', 'Operación de inventario y preparación',  TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON
       (r.name = 'ADMIN')
    OR (r.name = 'SUPPORT'   AND p.code IN ('backoffice:access', 'users:read'))
    OR (r.name = 'WAREHOUSE' AND p.code IN ('backoffice:access'));
