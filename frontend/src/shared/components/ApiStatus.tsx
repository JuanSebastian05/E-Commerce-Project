import { useHealth } from '../api/health'

export function ApiStatus() {
  const { data, isPending, isError } = useHealth()

  if (isPending) {
    return <p className="text-sm text-gray-500">Comprobando la API…</p>
  }
  if (isError) {
    return <p className="text-sm text-red-600">La API no responde. ¿Está el backend en marcha?</p>
  }
  return (
    <p className="text-sm text-green-700">
      API: {data.status} · Base de datos: {data.database}
    </p>
  )
}
