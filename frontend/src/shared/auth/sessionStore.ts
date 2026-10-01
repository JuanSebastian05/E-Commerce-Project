import { create } from 'zustand'
import type { UserProfile } from './authApi'

export type SessionStatus = 'loading' | 'authenticated' | 'anonymous'

interface SessionState {
  status: SessionStatus
  /** Solo en memoria (decisión D1): no se guarda en localStorage para que un XSS no lo pueda leer. */
  accessToken: string | null
  user: UserProfile | null
}

export const useSessionStore = create<SessionState>()(() => ({
  status: 'loading',
  accessToken: null,
  user: null,
}))

export function useHasPermission(permission: string) {
  return useSessionStore((state) => state.user?.permissions.includes(permission) ?? false)
}
