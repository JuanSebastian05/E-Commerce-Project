import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { logout } from './session'

/** Cierra la sesión, borra los datos en caché del usuario que sale y vuelve a la tienda. */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } catch {
      // La sesión local ya se borró; si la API falló, la cookie caduca sola.
    } finally {
      queryClient.clear()
      setLoggingOut(false)
      navigate('/')
    }
  }

  return { logout: handleLogout, loggingOut }
}
