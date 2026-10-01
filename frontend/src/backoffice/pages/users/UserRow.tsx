import { useState } from 'react'
import { Button } from '../../../shared/components/Button'
import { useChangeUserStatus, useReplaceUserRoles, type UserView } from '../../api/users'
import { CheckboxGroup } from '../../components/CheckboxGroup'

interface UserRowProps {
  user: UserView
  isCurrentUser: boolean
  canManage: boolean
  /** Roles que se pueden asignar; vacío si el usuario no puede consultar los roles. */
  roleNames: string[]
  /** Recibe el error de una acción, o null al empezar otra. */
  onError: (error: unknown) => void
}

const dateFormat = new Intl.DateTimeFormat('es', { dateStyle: 'medium' })

export function UserRow({ user, isCurrentUser, canManage, roleNames, onError }: UserRowProps) {
  const [editingRoles, setEditingRoles] = useState(false)
  const [roles, setRoles] = useState<string[]>(user.roles)
  const changeStatus = useChangeUserStatus()
  const replaceRoles = useReplaceUserRoles()
  const fullName = `${user.firstName} ${user.lastName}`

  function toggleStatus() {
    onError(null)
    changeStatus.mutate({ id: user.id, enabled: !user.enabled }, { onError })
  }

  function startEditingRoles() {
    setRoles(user.roles)
    setEditingRoles(true)
  }

  function saveRoles() {
    onError(null)
    replaceRoles.mutate({ id: user.id, roles }, { onSuccess: () => setEditingRoles(false), onError })
  }

  return (
    <>
      <tr className="border-t border-gray-200">
        <td className="px-3 py-2">
          {user.email}
          {isCurrentUser && <span className="ml-2 text-xs text-gray-500">(tú)</span>}
        </td>
        <td className="px-3 py-2">{fullName}</td>
        <td className="px-3 py-2">{[...user.roles].sort().join(', ')}</td>
        <td className="px-3 py-2">
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
              user.enabled ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600'
            }`}
          >
            {user.enabled ? 'Activo' : 'Desactivado'}
          </span>
        </td>
        <td className="px-3 py-2 text-gray-600">{dateFormat.format(new Date(user.createdAt))}</td>
        {canManage && (
          <td className="space-x-3 px-3 py-2 whitespace-nowrap">
            <Button variant="link" onClick={toggleStatus} disabled={changeStatus.isPending}>
              {user.enabled ? 'Desactivar' : 'Activar'}
            </Button>
            {roleNames.length > 0 && (
              <Button variant="link" onClick={startEditingRoles} disabled={editingRoles}>
                Cambiar roles
              </Button>
            )}
          </td>
        )}
      </tr>
      {editingRoles && (
        <tr className="bg-gray-50">
          <td colSpan={6} className="space-y-3 px-3 py-3">
            <CheckboxGroup
              legend={`Roles de ${user.email}`}
              options={roleNames.map((name) => ({ value: name, label: name }))}
              selected={roles}
              onChange={setRoles}
            />
            <p className="text-xs text-gray-500">
              Al cambiar los roles se cierran todas las sesiones de este usuario.
            </p>
            <div className="flex gap-2">
              <Button onClick={saveRoles} disabled={replaceRoles.isPending || roles.length === 0}>
                Guardar roles
              </Button>
              <Button variant="secondary" onClick={() => setEditingRoles(false)}>
                Cancelar
              </Button>
            </div>
          </td>
        </tr>
      )}
    </>
  )
}
