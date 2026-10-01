import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../shared/api/client'

export interface RoleView {
  id: string
  name: string
  description: string | null
  system: boolean
  permissions: string[]
}

export interface PermissionView {
  code: string
  description: string
}

export interface CreateRoleData {
  name: string
  description: string
  permissions: string[]
}

/** El rol ADMIN siempre conserva estos permisos (HU-07); el backend responde 409 si se quitan. */
export const ADMIN_ROLE = 'ADMIN'
export const ADMIN_REQUIRED_PERMISSIONS = ['users:manage', 'roles:manage']

const rolesKey = ['roles'] as const

export function useRoles(enabled = true) {
  return useQuery({
    queryKey: rolesKey,
    queryFn: () => apiRequest<RoleView[]>('/api/v1/roles', { auth: true }),
    enabled,
  })
}

export function usePermissions() {
  return useQuery({
    queryKey: ['permissions'],
    queryFn: () => apiRequest<PermissionView[]>('/api/v1/permissions', { auth: true }),
    // El catálogo es fijo: solo cambia con una migración.
    staleTime: Infinity,
  })
}

function useInvalidateRoles() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: rolesKey })
}

export function useCreateRole() {
  const invalidate = useInvalidateRoles()
  return useMutation({
    mutationFn: (data: CreateRoleData) =>
      apiRequest<RoleView>('/api/v1/roles', { method: 'POST', body: data, auth: true }),
    onSuccess: invalidate,
  })
}

export function useReplaceRolePermissions() {
  const invalidate = useInvalidateRoles()
  return useMutation({
    mutationFn: ({ id, permissions }: { id: string; permissions: string[] }) =>
      apiRequest<RoleView>(`/api/v1/roles/${id}/permissions`, { method: 'PUT', body: { permissions }, auth: true }),
    onSuccess: invalidate,
  })
}

export function useDeleteRole() {
  const invalidate = useInvalidateRoles()
  return useMutation({
    mutationFn: (id: string) => apiRequest<void>(`/api/v1/roles/${id}`, { method: 'DELETE', auth: true }),
    onSuccess: invalidate,
  })
}
