import { ApiStatus } from '../../shared/components/ApiStatus'

export function DashboardPage() {
  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <p className="text-gray-600">
        El acceso se protegerá con autenticación y permisos en el módulo Auth.
      </p>
      <ApiStatus />
    </section>
  )
}
