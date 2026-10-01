import { apiRequest } from '../api/client'

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface UserProfile {
  id: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
  permissions: string[]
}

export interface RegisterData {
  email: string
  password: string
  firstName: string
  lastName: string
}

export const authApi = {
  register: (data: RegisterData) =>
    apiRequest<UserProfile>('/api/v1/auth/register', { method: 'POST', body: data }),
  login: (email: string, password: string) =>
    apiRequest<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: { email, password } }),
  refresh: () => apiRequest<TokenResponse>('/api/v1/auth/refresh', { method: 'POST' }),
  logout: () => apiRequest<void>('/api/v1/auth/logout', { method: 'POST' }),
  me: () => apiRequest<UserProfile>('/api/v1/users/me', { auth: true }),
}
