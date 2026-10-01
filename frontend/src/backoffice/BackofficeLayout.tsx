import { Link, Outlet } from 'react-router'

export function BackofficeLayout() {
  return (
    <div className="flex min-h-screen bg-gray-50 text-gray-900">
      <aside className="w-56 border-r border-gray-200 bg-white p-4">
        <Link to="/admin" className="font-semibold">
          Backoffice
        </Link>
      </aside>
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  )
}
