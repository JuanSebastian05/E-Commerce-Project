import { useState, type FormEvent } from 'react'
import { ApiError } from '../../../shared/api/client'
import { errorMessage } from '../../../shared/api/errorMessage'
import { passwordPolicyError } from '../../../shared/auth/passwordPolicy'
import { Button } from '../../../shared/components/Button'
import { FormAlert } from '../../../shared/components/FormAlert'
import { FormField } from '../../../shared/components/FormField'
import { useCreateUser, type CreateUserData } from '../../api/users'
import { CheckboxGroup } from '../../components/CheckboxGroup'

type Field = keyof CreateUserData
type FieldErrors = Partial<Record<Field, string>>

const EMPTY_FORM: CreateUserData = { firstName: '', lastName: '', email: '', password: '', roles: [] }

function validate(form: CreateUserData): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.firstName.trim()) errors.firstName = 'Escribe el nombre'
  if (!form.lastName.trim()) errors.lastName = 'Escribe el apellido'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errors.email = 'Escribe un email válido'
  const passwordError = passwordPolicyError(form.password)
  if (passwordError) errors.password = passwordError
  if (form.roles.length === 0) errors.roles = 'Elige al menos un rol'
  return errors
}

interface CreateUserPanelProps {
  roleNames: string[]
  onClose: () => void
}

export function CreateUserPanel({ roleNames, onClose }: CreateUserPanelProps) {
  const [form, setForm] = useState<CreateUserData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const mutation = useCreateUser()

  function update<K extends Field>(field: K, value: CreateUserData[K]) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }
    mutation.mutate(
      { ...form, firstName: form.firstName.trim(), lastName: form.lastName.trim(), email: form.email.trim() },
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
      aria-label="Nuevo usuario"
      className="space-y-4 rounded-lg border border-gray-200 bg-white p-4"
    >
      <h2 className="font-semibold">Nuevo usuario</h2>
      <FormAlert message={mutation.isError ? errorMessage(mutation.error) : null} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField
          label="Nombre"
          maxLength={100}
          value={form.firstName}
          error={fieldErrors.firstName}
          onChange={(event) => update('firstName', event.target.value)}
        />
        <FormField
          label="Apellido"
          maxLength={100}
          value={form.lastName}
          error={fieldErrors.lastName}
          onChange={(event) => update('lastName', event.target.value)}
        />
        <FormField
          label="Email"
          type="email"
          maxLength={254}
          autoComplete="off"
          value={form.email}
          error={fieldErrors.email}
          onChange={(event) => update('email', event.target.value)}
        />
        <FormField
          label="Contraseña inicial"
          type="password"
          autoComplete="new-password"
          value={form.password}
          error={fieldErrors.password}
          onChange={(event) => update('password', event.target.value)}
        />
      </div>
      <CheckboxGroup
        legend="Roles"
        options={roleNames.map((name) => ({ value: name, label: name }))}
        selected={form.roles}
        onChange={(roles) => update('roles', roles)}
        error={fieldErrors.roles}
      />
      <div className="flex gap-2">
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creando…' : 'Crear usuario'}
        </Button>
        <Button variant="secondary" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}
