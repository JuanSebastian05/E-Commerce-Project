/** Catálogo fijo de permisos del backend (ADR-003). Debe coincidir con la migración V3. */
export const Permissions = {
  BACKOFFICE_ACCESS: 'backoffice:access',
  USERS_READ: 'users:read',
  USERS_MANAGE: 'users:manage',
  ROLES_READ: 'roles:read',
  ROLES_MANAGE: 'roles:manage',
} as const

export type Permission = (typeof Permissions)[keyof typeof Permissions]
