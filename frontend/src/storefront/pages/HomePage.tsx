import { ApiStatus } from '../../shared/components/ApiStatus'

export function HomePage() {
  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-bold">Bienvenido a TechStore</h1>
      <p className="text-gray-600">El catálogo de productos llegará en el próximo módulo.</p>
      <ApiStatus />
    </section>
  )
}
