import type { ReactNode } from 'react'
import type { Permission } from '../../shared/auth/permissions'
import { useHasPermission } from '../../shared/auth/sessionStore'
import { NoAccess } from './NoAccess'

/**
 * Muestra la página solo con el permiso indicado. Es comodidad para el usuario:
 * la protección real está en el backend, que responde 403.
 */
export function RequirePermission({ permission, children }: { permission: Permission; children: ReactNode }) {
  const allowed = useHasPermission(permission)
  return allowed ? children : <NoAccess />
}
