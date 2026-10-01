import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { logout } from '../../shared/auth/session'
import { useHasPermission, useSessionStore } from '../../shared/auth/sessionStore'

export function AccountMenu() {
  const status = useSessionStore((state) => state.status)
  const user = useSessionStore((state) => state.user)
  const canOpenBackoffice = useHasPermission('backoffice:access')
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)

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

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } catch {
      // La sesión local ya se borró; si la API falló, la cookie caduca sola.
    } finally {
      // Los datos en caché pertenecían al usuario que sale.
      queryClient.clear()
      setLoggingOut(false)
      navigate('/')
    }
  }

  return (
    <div className="flex items-center gap-4 text-sm">
      <span className="text-gray-600">Hola, {user.firstName}</span>
      {canOpenBackoffice && (
        <Link to="/admin" className="hover:underline">
          Backoffice
        </Link>
      )}
      <button type="button" onClick={handleLogout} disabled={loggingOut} className="hover:underline disabled:opacity-50">
        Cerrar sesión
      </button>
    </div>
  )
}
