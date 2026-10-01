# Reglas de negocio

## Módulo Auth + Users

| ID | Regla |
|----|-------|
| RN-01 | El email es único sin distinguir mayúsculas y se guarda en minúsculas. |
| RN-02 | Contraseña: entre 8 y 72 caracteres, con al menos una letra y un número. 72 es el límite de BCrypt. |
| RN-03 | La contraseña nunca se guarda ni se registra en claro; solo su hash. |
| RN-04 | Un usuario desactivado no puede iniciar sesión ni renovar tokens. |
| RN-05 | La autorización se decide por permisos (`users:read`...), nunca por el nombre del rol. |
| RN-06 | Los permisos de un usuario son la unión de los permisos de todos sus roles. |
| RN-07 | Los roles de sistema no se pueden borrar ni renombrar. |
| RN-08 | No se puede dejar el sistema sin ningún usuario activo con `roles:manage`. |
| RN-09 | El registro público siempre asigna CUSTOMER; los demás roles solo los asigna un ADMIN. |

## Permisos iniciales

| Permiso | ADMIN | SUPPORT | WAREHOUSE | CUSTOMER |
|---------|:-----:|:-------:|:---------:|:--------:|
| `backoffice:access` | ✔ | ✔ | ✔ | |
| `users:read` | ✔ | ✔ | | |
| `users:manage` | ✔ | | | |
| `roles:read` | ✔ | | | |
| `roles:manage` | ✔ | | | |

Cada módulo nuevo añade sus permisos mediante una migración.
