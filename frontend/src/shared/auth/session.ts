import { ApiError, setAuthHooks } from '../api/client'
import { authApi } from './authApi'
import { useSessionStore } from './sessionStore'

const REFRESH_LOCK = 'techstore-auth-refresh'

let refreshInFlight: Promise<string | null> | null = null
let restoreInFlight: Promise<void> | null = null

/**
 * Pide un access token nuevo con la cookie del refresh token.
 *
 * El backend rota el refresh token y, si recibe uno ya usado, revoca todas las sesiones.
 * Por eso nunca hay dos renovaciones a la vez: dentro de la pestaña se comparte la misma
 * promesa, y entre pestañas se usa un Web Lock, de modo que la segunda pestaña envía la
 * cookie que la primera acaba de recibir.
 */
export function refreshAccessToken(): Promise<string | null> {
  refreshInFlight ??= withRefreshLock(doRefresh).finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

async function doRefresh(): Promise<string | null> {
  try {
    const { accessToken } = await authApi.refresh()
    useSessionStore.setState({ accessToken })
    return accessToken
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      clearSession()
      return null
    }
    throw error
  }
}

function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request(REFRESH_LOCK, task)
  }
  return task()
}

/** Al cargar la aplicación, recupera la sesión a partir de la cookie, si existe. */
export function restoreSession(): Promise<void> {
  restoreInFlight ??= (async () => {
    try {
      const token = await refreshAccessToken()
      if (token) {
        await loadProfile()
      }
    } catch {
      // API caída o error inesperado: se continúa sin sesión.
      clearSession()
    }
  })()
  return restoreInFlight
}

export async function login(email: string, password: string): Promise<void> {
  const { accessToken } = await authApi.login(email, password)
  useSessionStore.setState({ accessToken })
  await loadProfile()
}

/** Cierra la sesión en el backend y en memoria. Si la API falla, la sesión local se borra igual. */
export async function logout(): Promise<void> {
  try {
    await authApi.logout()
  } finally {
    clearSession()
  }
}

async function loadProfile(): Promise<void> {
  const user = await authApi.me()
  useSessionStore.setState({ user, status: 'authenticated' })
}

function clearSession() {
  useSessionStore.setState({ status: 'anonymous', accessToken: null, user: null })
}

setAuthHooks({
  getAccessToken: () => useSessionStore.getState().accessToken,
  refreshAccessToken,
})
