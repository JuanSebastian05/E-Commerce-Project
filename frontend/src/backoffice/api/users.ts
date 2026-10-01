import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../shared/api/client'

export interface UserView {
  id: string
  email: string
  firstName: string
  lastName: string
  enabled: boolean
  roles: string[]
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserFilters {
  email?: string
  role?: string
  enabled?: boolean
  page: number
}

export interface CreateUserData {
  email: string
  password: string
  firstName: string
  lastName: string
  roles: string[]
}

export const PAGE_SIZE = 20

const usersKey = ['users'] as const

export function useUsers(filters: UserFilters) {
  return useQuery({
    queryKey: [...usersKey, filters],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(filters.page), size: String(PAGE_SIZE) })
      if (filters.email) params.set('email', filters.email)
      if (filters.role) params.set('role', filters.role)
      if (filters.enabled !== undefined) params.set('enabled', String(filters.enabled))
      return apiRequest<Page<UserView>>(`/api/v1/users?${params}`, { auth: true })
    },
    // Al cambiar de página se sigue viendo la anterior hasta que llega la nueva.
    placeholderData: keepPreviousData,
  })
}

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: usersKey })
}

export function useCreateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (data: CreateUserData) =>
      apiRequest<UserView>('/api/v1/users', { method: 'POST', body: data, auth: true }),
    onSuccess: invalidate,
  })
}

export function useChangeUserStatus() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      apiRequest<UserView>(`/api/v1/users/${id}/status`, { method: 'PATCH', body: { enabled }, auth: true }),
    onSuccess: invalidate,
  })
}

export function useReplaceUserRoles() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, roles }: { id: string; roles: string[] }) =>
      apiRequest<UserView>(`/api/v1/users/${id}/roles`, { method: 'PUT', body: { roles }, auth: true }),
    onSuccess: invalidate,
  })
}
