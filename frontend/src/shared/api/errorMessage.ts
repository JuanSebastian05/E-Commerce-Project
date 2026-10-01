import { ApiError } from './client'

/** Mensaje para mostrar al usuario a partir de un error de la API o de red. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429 && error.retryAfterSeconds) {
      const minutes = Math.ceil(error.retryAfterSeconds / 60)
      return `Demasiados intentos fallidos. Vuelve a intentarlo en ${minutes} ${minutes === 1 ? 'minuto' : 'minutos'}.`
    }
    if (error.status >= 500) {
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
    }
    return error.message
  }
  return 'No se pudo conectar con el servidor. Comprueba tu conexión.'
}
