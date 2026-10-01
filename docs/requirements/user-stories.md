# Historias de usuario

Cada HU se relaciona con sus reglas en [business-rules.md](business-rules.md) y con sus endpoints en la documentación de la API.

## Módulo Auth + Users

**HU-01 Registro.** Como visitante quiero crear una cuenta para poder comprar.
- Email válido y único (sin distinguir mayúsculas), contraseña que cumpla la política, nombre y apellido obligatorios.
- La cuenta nueva queda activa y con el rol CUSTOMER.
- Si el email ya existe, se responde `409` sin revelar datos de la otra cuenta.

**HU-02 Login.** Como usuario quiero iniciar sesión con email y contraseña.
- Con credenciales correctas se recibe un access token y un refresh token.
- Si las credenciales son incorrectas o el usuario está desactivado, se responde `401` con el mismo mensaje genérico en ambos casos.
- Tras demasiados intentos fallidos se responde `429`.

**HU-03 Mantener la sesión.** Como usuario quiero que mi sesión se renueve sin volver a escribir la contraseña.
- Un refresh token válido devuelve un par nuevo de tokens, y el refresh token usado deja de valer (rotación).
- Reutilizar un refresh token ya usado revoca todas las sesiones de ese usuario.

**HU-04 Logout.** Como usuario quiero cerrar sesión. El refresh token queda revocado.
- Funciona con la cookie de la sesión aunque el access token ya haya caducado.

**HU-05 Mi perfil.** Como usuario autenticado quiero ver mis datos, mis roles y mis permisos.

**HU-06 Gestionar usuarios.** Como ADMIN quiero crear, consultar, desactivar y reactivar usuarios, y asignarles roles.
- El listado es paginado y se puede filtrar por email, rol y estado.
- Al desactivar un usuario se revocan todas sus sesiones.
- Un ADMIN no puede desactivarse a sí mismo ni quitarse el último rol con permisos de administración.

**HU-07 Gestionar roles y permisos.** Como ADMIN quiero crear roles y decidir qué permisos tiene cada uno.
- Los permisos forman un catálogo fijo definido por el sistema y solo se consultan (ver [ADR-003](../architecture/decisions/ADR-003-autenticacion-y-autorizacion.md)).
- Los roles de sistema (ADMIN, CUSTOMER, SUPPORT, WAREHOUSE) no se pueden borrar.
- El rol ADMIN siempre conserva los permisos de administrar roles y usuarios, para que nadie pierda el acceso.

**HU-08 Proteger el Backoffice.** Como sistema quiero que el Backoffice solo muestre lo que permiten los permisos del usuario, y que el backend rechace cualquier acción no permitida con `403`.

