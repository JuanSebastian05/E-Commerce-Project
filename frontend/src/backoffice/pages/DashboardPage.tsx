import { ApiStatus } from '../../shared/components/ApiStatus'
import { useSessionStore } from '../../shared/auth/sessionStore'

export function DashboardPage() {
  const user = useSessionStore((state) => state.user)

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-bold">Hola, {user?.firstName}</h1>
      <p className="text-gray-600">
        Roles: {user?.roles.join(', ')}. El menú muestra solo las secciones que permiten tus permisos.
      </p>
      <ApiStatus />
    </section>
  )
}
