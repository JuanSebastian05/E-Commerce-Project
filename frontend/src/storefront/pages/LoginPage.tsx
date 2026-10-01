import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router'
import { errorMessage } from '../../shared/api/errorMessage'
import { safeRedirect } from '../../shared/auth/redirect'
import { login } from '../../shared/auth/session'
import { useSessionStore } from '../../shared/auth/sessionStore'
import { FormAlert } from '../../shared/components/FormAlert'
import { FormField } from '../../shared/components/FormField'

export function LoginPage() {
  const [searchParams] = useSearchParams()
  const status = useSessionStore((state) => state.status)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const redirectTo = safeRedirect(searchParams.get('redirect'))

  const mutation = useMutation({
    mutationFn: () => login(email.trim(), password),
  })

  // Tras el login (o si ya había sesión) se va al destino pedido.
  if (status === 'authenticated') {
    return <Navigate to={redirectTo} replace />
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate()
  }

  return (
    <section className="mx-auto max-w-sm space-y-6">
      <h1 className="text-2xl font-bold">Iniciar sesión</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <FormAlert message={mutation.isError ? errorMessage(mutation.error) : null} />
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <FormField
          label="Contraseña"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <button
          type="submit"
          disabled={mutation.isPending || !email.trim() || !password}
          className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <p className="text-sm text-gray-600">
        ¿No tienes cuenta?{' '}
        <Link to="/registro" className="font-medium text-blue-600 hover:underline">
          Crea una
        </Link>
      </p>
    </section>
  )
}
