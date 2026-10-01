import { useState } from 'react'
import { errorMessage } from '../../../shared/api/errorMessage'
import { Permissions } from '../../../shared/auth/permissions'
import { useHasPermission } from '../../../shared/auth/sessionStore'
import { Button } from '../../../shared/components/Button'
import { FormAlert } from '../../../shared/components/FormAlert'
import { usePermissions, useRoles } from '../../api/roles'
import { CreateRolePanel } from './CreateRolePanel'
import { RoleCard } from './RoleCard'

export function RolesPage() {
  const canManage = useHasPermission(Permissions.ROLES_MANAGE)
  const roles = useRoles()
  const permissions = usePermissions()
  const [creating, setCreating] = useState(false)
  const error = roles.error ?? permissions.error

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Roles</h1>
        {canManage && !creating && permissions.data && <Button onClick={() => setCreating(true)}>Nuevo rol</Button>}
      </div>
      <p className="text-sm text-gray-600">
        Los permisos son un catálogo fijo del sistema: cada uno corresponde a una comprobación del backend. Aquí se
        decide qué permisos tiene cada rol. Los cambios llegan a sus usuarios en su siguiente renovación de sesión.
      </p>

      {creating && permissions.data && (
        <CreateRolePanel permissions={permissions.data} onClose={() => setCreating(false)} />
      )}

      {error && <FormAlert message={errorMessage(error)} />}
      {(roles.isPending || permissions.isPending) && !error && <p className="text-sm text-gray-500">Cargando roles…</p>}

      {roles.data && permissions.data && (
        <div className="space-y-4">
          {roles.data.map((role) => (
            <RoleCard key={role.id} role={role} permissions={permissions.data} canManage={canManage} />
          ))}
        </div>
      )}
    </section>
  )
}
