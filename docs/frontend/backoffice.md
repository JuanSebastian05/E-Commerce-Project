# Backoffice

Administración en `/admin`. Cubre la HU-06 (usuarios), la HU-07 (roles y permisos) y la HU-08 (proteger el Backoffice).

## Protección (HU-08)

- **Guardia de `/admin`** (`BackofficeLayout`): sin sesión, lleva al login con `?redirect=` para volver a la misma página. Con sesión pero sin `backoffice:access`, muestra "Sin acceso".
- **Guardia por página** (`RequirePermission`): `/admin/usuarios` pide `users:read` y `/admin/roles` pide `roles:read`.
- **Menú y botones:** solo aparecen las secciones y acciones que permiten los permisos del usuario.

Todo esto es comodidad para el usuario. La protección real está en el backend, que responde `403` aunque alguien fuerce una petición.

## Pantallas

| Ruta | Permiso para ver | Qué se puede hacer | Permiso para cambiar |
|------|------------------|--------------------|----------------------|
| `/admin` | `backoffice:access` | Inicio con los roles del usuario y el estado de la API | — |
| `/admin/usuarios` | `users:read` | Listado paginado con filtros por email, rol y estado (en la URL, se pueden recargar y compartir) | — |
| | | Crear usuarios, activarlos o desactivarlos y cambiar sus roles | `users:manage` (y `roles:read` para elegir roles) |
| `/admin/roles` | `roles:read` | Roles con sus permisos y el catálogo de permisos | — |
| | | Crear roles, cambiar sus permisos y borrar roles que no sean de sistema | `roles:manage` |

Los errores de negocio del backend (`409`) se muestran tal cual llegan: por ejemplo, desactivarse a uno mismo, un nombre de rol repetido o borrar un rol con usuarios. En el rol ADMIN, `users:manage` y `roles:manage` aparecen bloqueados porque siempre los conserva.

**Limitación:** elegir roles (al crear un usuario, cambiar sus roles o filtrar por rol) necesita `roles:read`, porque la lista de roles sale de `GET /roles`. SUPPORT ve el listado de usuarios sin el filtro por rol.

## Código

```
backoffice/
├── BackofficeLayout.tsx   guardia, menú por permisos y cierre de sesión
├── api/                   hooks de TanStack Query para usuarios y roles
├── components/            RequirePermission, NoAccess, CheckboxGroup
└── pages/
    ├── DashboardPage.tsx
    ├── users/             UsersPage, UserRow, CreateUserPanel
    └── roles/             RolesPage, RoleCard, CreateRolePanel
```

Tras cada cambio se invalidan las consultas de usuarios o de roles, así el listado se vuelve a pedir al backend.

## Pruebas

`frontend/e2e/backoffice.spec.ts` (ver [e2e.md](../testing/e2e.md)) cubre:

- **Guardia:** un visitante va al login y vuelve a la página que pidió; un cliente ve "Sin acceso".
- **Usuarios, como ADMIN:** crear un usuario, desactivarlo y reactivarlo, cambiarle los roles, y el `409` al desactivarse a sí mismo.
- **Roles, como ADMIN:** crear un rol, cambiar sus permisos y borrarlo; un nombre repetido; los permisos bloqueados del rol ADMIN.
- **SUPPORT:** ve los usuarios pero no puede gestionarlos ni entrar en Roles.
