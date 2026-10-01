import { Link, Navigate, NavLink, Outlet, useLocation } from 'react-router'
import { Permissions, type Permission } from '../shared/auth/permissions'
import { useHasPermission, useSessionStore } from '../shared/auth/sessionStore'
import { useLogout } from '../shared/auth/useLogout'
import { NoAccess } from './components/NoAccess'

interface NavItem {
  to: string
  label: string
  permission: Permission
  end?: boolean
}

const NAV_ITEMS: NavItem[] = [
  { to: '/admin', label: 'Inicio', permission: Permissions.BACKOFFICE_ACCESS, end: true },
  { to: '/admin/usuarios', label: 'Usuarios', permission: Permissions.USERS_READ },
  { to: '/admin/roles', label: 'Roles', permission: Permissions.ROLES_READ },
]

/** Guardia del Backoffice (HU-08): exige sesión y el permiso backoffice:access. */
export function BackofficeLayout() {
  const status = useSessionStore((state) => state.status)
  const canAccess = useHasPermission(Permissions.BACKOFFICE_ACCESS)
  const location = useLocation()

  if (status === 'loading') {
    return <p className="p-8 text-sm text-gray-500">Cargando…</p>
  }
  if (status === 'anonymous') {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  if (!canAccess) {
    return (
      <main className="mx-auto max-w-6xl px-4 py-8">
        <NoAccess />
      </main>
    )
  }
  return <BackofficeShell />
}

function BackofficeShell() {
  const user = useSessionStore((state) => state.user)
  const { logout, loggingOut } = useLogout()

  return (
    <div className="flex min-h-screen bg-gray-50 text-gray-900">
      <aside className="flex w-56 flex-col border-r border-gray-200 bg-white p-4">
        <Link to="/admin" className="font-semibold">
          Backoffice
        </Link>
        <nav aria-label="Backoffice" className="mt-6 flex flex-col gap-1 text-sm">
          {NAV_ITEMS.filter((item) => user?.permissions.includes(item.permission)).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 ${isActive ? 'bg-blue-50 font-medium text-blue-700' : 'hover:bg-gray-100'}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto space-y-2 border-t border-gray-200 pt-4 text-sm">
          <p className="truncate text-gray-600" title={user?.email}>
            {user?.firstName} {user?.lastName}
          </p>
          <Link to="/" className="block hover:underline">
            Ir a la tienda
          </Link>
          <button type="button" onClick={logout} disabled={loggingOut} className="hover:underline disabled:opacity-50">
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-x-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
