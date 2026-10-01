import { useState, type FormEvent } from 'react'
import { ApiError } from '../../../shared/api/client'
import { errorMessage } from '../../../shared/api/errorMessage'
import { Button } from '../../../shared/components/Button'
import { FormAlert } from '../../../shared/components/FormAlert'
import { FormField } from '../../../shared/components/FormField'
import { useCreateRole, type CreateRoleData, type PermissionView } from '../../api/roles'
import { CheckboxGroup } from '../../components/CheckboxGroup'

/** Misma regla que el backend: letras, números y guiones bajos; de 2 a 50 caracteres. */
const ROLE_NAME = /^[A-Za-z][A-Za-z0-9_]{1,49}$/

type FieldErrors = Partial<Record<keyof CreateRoleData, string>>

interface CreateRolePanelProps {
  permissions: PermissionView[]
  onClose: () => void
}

export function CreateRolePanel({ permissions, onClose }: CreateRolePanelProps) {
  const [form, setForm] = useState<CreateRoleData>({ name: '', description: '', permissions: [] })
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const mutation = useCreateRole()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!ROLE_NAME.test(form.name.trim())) {
      setFieldErrors({ name: 'Solo letras, números y guiones bajos; entre 2 y 50 caracteres, empezando por letra' })
      return
    }
    setFieldErrors({})
    mutation.mutate(
      { ...form, name: form.name.trim(), description: form.description.trim() },
      {
        onSuccess: onClose,
        onError: (error) => {
          if (error instanceof ApiError && error.fieldErrors.length > 0) {
            setFieldErrors(Object.fromEntries(error.fieldErrors.map((e) => [e.field, e.message])))
          }
        },
      },
    )
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      aria-label="Nuevo rol"
      className="space-y-4 rounded-lg border border-gray-200 bg-white p-4"
    >
      <h2 className="font-semibold">Nuevo rol</h2>
      <FormAlert message={mutation.isError ? errorMessage(mutation.error) : null} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField
          label="Nombre del rol"
          maxLength={50}
          placeholder="AUDITOR"
          value={form.name}
          error={fieldErrors.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })}
        />
        <FormField
          label="Descripción"
          maxLength={255}
          value={form.description}
          error={fieldErrors.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })}
        />
      </div>
      <CheckboxGroup
        legend="Permisos"
        options={permissions.map((permission) => ({
          value: permission.code,
          label: permission.code,
          hint: permission.description,
        }))}
        selected={form.permissions}
        onChange={(selected) => setForm({ ...form, permissions: selected })}
      />
      <div className="flex gap-2">
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creando…' : 'Crear rol'}
        </Button>
        <Button variant="secondary" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}
