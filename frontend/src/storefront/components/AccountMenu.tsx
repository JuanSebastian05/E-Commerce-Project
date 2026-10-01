import { Link } from 'react-router'
import { Permissions } from '../../shared/auth/permissions'
import { useHasPermission, useSessionStore } from '../../shared/auth/sessionStore'
import { useLogout } from '../../shared/auth/useLogout'

export function AccountMenu() {
  const status = useSessionStore((state) => state.status)
  const user = useSessionStore((state) => state.user)
  const canOpenBackoffice = useHasPermission(Permissions.BACKOFFICE_ACCESS)
  const { logout, loggingOut } = useLogout()

  if (status === 'loading') {
    return null
  }

  if (status === 'anonymous' || !user) {
    return (
      <div className="flex items-center gap-4 text-sm">
        <Link to="/login" className="hover:underline">
          Iniciar sesión
        </Link>
        <Link to="/registro" className="rounded-md bg-blue-600 px-3 py-1.5 font-medium text-white hover:bg-blue-700">
          Crear cuenta
        </Link>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-4 text-sm">
      <span className="text-gray-600">Hola, {user.firstName}</span>
      {canOpenBackoffice && (
        <Link to="/admin" className="hover:underline">
          Backoffice
        </Link>
      )}
      <button type="button" onClick={logout} disabled={loggingOut} className="hover:underline disabled:opacity-50">
        Cerrar sesión
      </button>
    </div>
  )
}
