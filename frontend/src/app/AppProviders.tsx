import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect } from 'react'
import { RouterProvider } from 'react-router'
import { restoreSession } from '../shared/auth/session'
import { router } from './router'

const queryClient = new QueryClient()

export function AppProviders() {
  useEffect(() => {
    // El access token solo vive en memoria: al recargar la página se recupera con la cookie.
    void restoreSession()
  }, [])

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
