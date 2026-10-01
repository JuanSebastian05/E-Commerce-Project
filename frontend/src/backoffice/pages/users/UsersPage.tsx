import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { errorMessage } from '../../../shared/api/errorMessage'
import { Permissions } from '../../../shared/auth/permissions'
import { useHasPermission, useSessionStore } from '../../../shared/auth/sessionStore'
import { Button } from '../../../shared/components/Button'
import { FormAlert } from '../../../shared/components/FormAlert'
import { useRoles } from '../../api/roles'
import { useUsers, type UserFilters } from '../../api/users'
import { CreateUserPanel } from './CreateUserPanel'
import { UserRow } from './UserRow'

/** Los filtros y la página viven en la URL, así se pueden recargar y compartir. */
function filtersFrom(params: URLSearchParams): UserFilters {
  const enabled = params.get('estado')
  return {
    email: params.get('email') ?? undefined,
    role: params.get('rol') ?? undefined,
    enabled: enabled === 'activos' ? true : enabled === 'desactivados' ? false : undefined,
    page: Math.max(0, Number(params.get('pagina') ?? 0) || 0),
  }
}

export function UsersPage() {
  const [params, setParams] = useSearchParams()
  const filters = filtersFrom(params)
  const currentUserId = useSessionStore((state) => state.user?.id)
  const canManage = useHasPermission(Permissions.USERS_MANAGE)
  const canReadRoles = useHasPermission(Permissions.ROLES_READ)
  const users = useUsers(filters)
  const roles = useRoles(canReadRoles)
  const roleNames = roles.data?.map((role) => role.name) ?? []
  const [creating, setCreating] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    for (const key of ['email', 'rol', 'estado']) {
      const value = String(data.get(key) ?? '').trim()
      if (value) next.set(key, value)
    }
    setParams(next)
  }

  function goToPage(page: number) {
    const next = new URLSearchParams(params)
    next.set('pagina', String(page))
    setParams(next)
  }

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Usuarios</h1>
        {canManage && canReadRoles && !creating && (
          <Button onClick={() => setCreating(true)}>Nuevo usuario</Button>
        )}
      </div>

      {creating && <CreateUserPanel roleNames={roleNames} onClose={() => setCreating(false)} />}

      <form
        key={params.toString()}
        onSubmit={applyFilters}
        role="search"
        className="flex flex-wrap items-end gap-3 text-sm"
      >
        <label className="space-y-1">
          <span className="block font-medium text-gray-700">Email contiene</span>
          <input
            name="email"
            defaultValue={filters.email}
            className="rounded-md border border-gray-300 px-3 py-1.5"
          />
        </label>
        {canReadRoles && (
          <label className="space-y-1">
            <span className="block font-medium text-gray-700">Rol</span>
            <select name="rol" defaultValue={filters.role ?? ''} className="rounded-md border border-gray-300 px-3 py-1.5">
              <option value="">Todos</option>
              {roleNames.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="space-y-1">
          <span className="block font-medium text-gray-700">Estado</span>
          <select
            name="estado"
            defaultValue={params.get('estado') ?? ''}
            className="rounded-md border border-gray-300 px-3 py-1.5"
          >
            <option value="">Todos</option>
            <option value="activos">Activos</option>
            <option value="desactivados">Desactivados</option>
          </select>
        </label>
        <Button type="submit" variant="secondary">
          Buscar
        </Button>
      </form>

      <FormAlert message={actionError} />
      {users.isError && <FormAlert message={errorMessage(users.error)} />}
      {users.isPending && <p className="text-sm text-gray-500">Cargando usuarios…</p>}

      {users.data && (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-3 py-2 font-medium">Email</th>
                  <th className="px-3 py-2 font-medium">Nombre</th>
                  <th className="px-3 py-2 font-medium">Roles</th>
                  <th className="px-3 py-2 font-medium">Estado</th>
                  <th className="px-3 py-2 font-medium">Alta</th>
                  {canManage && <th className="px-3 py-2 font-medium">Acciones</th>}
                </tr>
              </thead>
              <tbody>
                {users.data.content.map((user) => (
                  <UserRow
                    key={user.id}
                    user={user}
                    isCurrentUser={user.id === currentUserId}
                    canManage={canManage}
                    roleNames={roleNames}
                    onError={(error) => setActionError(error ? errorMessage(error) : null)}
                  />
                ))}
                {users.data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-3 py-6 text-center text-gray-500">
                      No hay usuarios con esos filtros.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <Pagination
            page={users.data.page}
            totalPages={users.data.totalPages}
            totalElements={users.data.totalElements}
            onChange={goToPage}
          />
        </>
      )}
    </section>
  )
}

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}

function Pagination({ page, totalPages, totalElements, onChange }: PaginationProps) {
  return (
    <nav aria-label="Paginación" className="flex items-center justify-between text-sm text-gray-600">
      <span>
        {totalElements} {totalElements === 1 ? 'usuario' : 'usuarios'} · página {totalPages === 0 ? 0 : page + 1} de{' '}
        {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>
          Anterior
        </Button>
        <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Siguiente
        </Button>
      </div>
    </nav>
  )
}
