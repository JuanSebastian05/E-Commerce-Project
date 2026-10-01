import { useState } from 'react'
import { errorMessage } from '../../../shared/api/errorMessage'
import { Button } from '../../../shared/components/Button'
import { FormAlert } from '../../../shared/components/FormAlert'
import {
  ADMIN_REQUIRED_PERMISSIONS,
  ADMIN_ROLE,
  useDeleteRole,
  useReplaceRolePermissions,
  type PermissionView,
  type RoleView,
} from '../../api/roles'
import { CheckboxGroup } from '../../components/CheckboxGroup'

interface RoleCardProps {
  role: RoleView
  permissions: PermissionView[]
  canManage: boolean
}

export function RoleCard({ role, permissions, canManage }: RoleCardProps) {
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [selected, setSelected] = useState<string[]>(role.permissions)
  const replacePermissions = useReplaceRolePermissions()
  const deleteRole = useDeleteRole()
  const error = replacePermissions.error ?? deleteRole.error

  const options = permissions.map((permission) => {
    // El rol ADMIN no puede perder estos permisos (HU-07): se muestran bloqueados.
    const locked = role.name === ADMIN_ROLE && ADMIN_REQUIRED_PERMISSIONS.includes(permission.code)
    return {
      value: permission.code,
      label: permission.code,
      hint: locked ? 'El rol ADMIN siempre conserva este permiso' : permission.description,
      disabled: locked,
    }
  })

  function startEditing() {
    replacePermissions.reset()
    setSelected(role.permissions)
    setEditing(true)
  }

  function save() {
    replacePermissions.mutate({ id: role.id, permissions: selected }, { onSuccess: () => setEditing(false) })
  }

  return (
    <article aria-label={`Rol ${role.name}`} className="space-y-3 rounded-lg border border-gray-200 bg-white p-4">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h2 className="font-semibold">
            {role.name}
            {role.system && (
              <span className="ml-2 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                Sistema
              </span>
            )}
          </h2>
          {role.description && <p className="text-sm text-gray-600">{role.description}</p>}
        </div>
        {canManage && !editing && (
          <div className="flex gap-3">
            <Button variant="link" onClick={startEditing}>
              Editar permisos
            </Button>
            {!role.system && !confirmingDelete && (
              <Button variant="link" className="text-red-600" onClick={() => setConfirmingDelete(true)}>
                Borrar
              </Button>
            )}
          </div>
        )}
      </header>

      <FormAlert message={error ? errorMessage(error) : null} />

      {editing ? (
        <CheckboxGroup legend="Permisos" options={options} selected={selected} onChange={setSelected} />
      ) : (
        <ul aria-label={`Permisos de ${role.name}`} className="flex flex-wrap gap-2">
          {[...role.permissions].sort().map((code) => (
            <li
              key={code}
              className="rounded-full bg-blue-50 px-2.5 py-0.5 font-mono text-xs text-blue-800"
              title={permissions.find((permission) => permission.code === code)?.description}
            >
              {code}
            </li>
          ))}
          {role.permissions.length === 0 && <li className="text-sm text-gray-500">Sin permisos</li>}
        </ul>
      )}

      {editing && (
        <div className="flex gap-2">
          <Button onClick={save} disabled={replacePermissions.isPending}>
            Guardar permisos
          </Button>
          <Button variant="secondary" onClick={() => setEditing(false)}>
            Cancelar
          </Button>
        </div>
      )}

      {confirmingDelete && (
        <div className="flex items-center gap-3 rounded-md bg-red-50 px-3 py-2 text-sm">
          <span>¿Borrar el rol {role.name}? Solo se puede si no tiene usuarios.</span>
          <Button variant="danger" onClick={() => deleteRole.mutate(role.id)} disabled={deleteRole.isPending}>
            Sí, borrar
          </Button>
          <Button variant="secondary" onClick={() => setConfirmingDelete(false)}>
            Cancelar
          </Button>
        </div>
      )}
    </article>
  )
}
