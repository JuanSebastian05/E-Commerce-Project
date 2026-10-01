import { Link } from 'react-router'

export function NoAccess() {
  return (
    <section className="space-y-3">
      <h1 className="text-2xl font-bold">Sin acceso</h1>
      <p className="text-gray-600">Tu cuenta no tiene permiso para ver esta sección.</p>
      <Link to="/" className="text-sm font-medium text-blue-600 hover:underline">
        Volver a la tienda
      </Link>
    </section>
  )
}
