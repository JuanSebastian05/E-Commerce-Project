import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { ApiError } from '../../shared/api/client'
import { errorMessage } from '../../shared/api/errorMessage'
import { authApi, type RegisterData } from '../../shared/auth/authApi'
import { passwordPolicyError } from '../../shared/auth/passwordPolicy'
import { login } from '../../shared/auth/session'
import { useSessionStore } from '../../shared/auth/sessionStore'
import { FormAlert } from '../../shared/components/FormAlert'
import { FormField } from '../../shared/components/FormField'

type Field = keyof RegisterData
type FieldErrors = Partial<Record<Field, string>>

class AccountCreatedButNotLoggedIn extends Error {
  constructor() {
    super('Tu cuenta se creó, pero no pudimos iniciar sesión. Entra desde "Iniciar sesión".')
  }
}

const EMPTY_FORM: RegisterData = { firstName: '', lastName: '', email: '', password: '' }

function validate(form: RegisterData): FieldErrors {
  const errors: FieldErrors = {}
  if (!form.firstName.trim()) errors.firstName = 'Escribe tu nombre'
  if (!form.lastName.trim()) errors.lastName = 'Escribe tu apellido'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errors.email = 'Escribe un email válido'
  const passwordError = passwordPolicyError(form.password)
  if (passwordError) errors.password = passwordError
  return errors
}

export function RegisterPage() {
  const status = useSessionStore((state) => state.status)
  const [form, setForm] = useState<RegisterData>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const mutation = useMutation({
    mutationFn: async (data: RegisterData) => {
      await authApi.register(data)
      // La cuenta nueva entra directamente, sin volver a pedir la contraseña.
      try {
        await login(data.email, data.password)
      } catch {
        throw new AccountCreatedButNotLoggedIn()
      }
    },
    onError: (error) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        setFieldErrors(Object.fromEntries(error.fieldErrors.map((e) => [e.field, e.message])))
      }
    },
  })

  // Tras registrarse y entrar (o si ya había sesión) se vuelve a la tienda.
  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  function update(field: Field, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length === 0) {
      mutation.mutate({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        password: form.password,
      })
    }
  }

  return (
    <section className="mx-auto max-w-sm space-y-6">
      <h1 className="text-2xl font-bold">Crear cuenta</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <FormAlert
          message={
            mutation.error instanceof AccountCreatedButNotLoggedIn
              ? mutation.error.message
              : mutation.isError
                ? errorMessage(mutation.error)
                : null
          }
        />
        <div className="grid grid-cols-2 gap-3">
          <FormField
            label="Nombre"
            autoComplete="given-name"
            maxLength={100}
            value={form.firstName}
            error={fieldErrors.firstName}
            onChange={(event) => update('firstName', event.target.value)}
          />
          <FormField
            label="Apellido"
            autoComplete="family-name"
            maxLength={100}
            value={form.lastName}
            error={fieldErrors.lastName}
            onChange={(event) => update('lastName', event.target.value)}
          />
        </div>
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          maxLength={254}
          value={form.email}
          error={fieldErrors.email}
          onChange={(event) => update('email', event.target.value)}
        />
        <FormField
          label="Contraseña"
          type="password"
          autoComplete="new-password"
          value={form.password}
          error={fieldErrors.password}
          onChange={(event) => update('password', event.target.value)}
        />
        <p className="text-xs text-gray-500">Mínimo 8 caracteres, con al menos una letra y un número.</p>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>
      <p className="text-sm text-gray-600">
        ¿Ya tienes cuenta?{' '}
        <Link to="/login" className="font-medium text-blue-600 hover:underline">
          Inicia sesión
        </Link>
      </p>
    </section>
  )
}
