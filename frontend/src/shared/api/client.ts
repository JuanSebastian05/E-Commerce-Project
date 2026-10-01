const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export interface FieldError {
  field: string
  message: string
}

/** Error de la API, construido a partir del Problem Details (RFC 9457) del backend. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]
  readonly retryAfterSeconds: number | null

  constructor(
    status: number,
    message: string,
    fieldErrors: FieldError[] = [],
    retryAfterSeconds: number | null = null,
  ) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
    this.retryAfterSeconds = retryAfterSeconds
  }
}

/**
 * Hooks que instala el módulo de sesión: dan el access token actual y, ante un 401,
 * intentan renovarlo. Así el cliente HTTP no depende del store de sesión.
 */
export interface AuthHooks {
  getAccessToken: () => string | null
  refreshAccessToken: () => Promise<string | null>
}

let authHooks: AuthHooks | null = null

export function setAuthHooks(hooks: AuthHooks) {
  authHooks = hooks
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** Envía el access token y, si caducó, renueva la sesión una vez y repite la petición. */
  auth?: boolean
}

/** Cliente HTTP común para la API REST (/api/v1/...). */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { auth = false } = options
  let response = await send(path, options, auth ? authHooks?.getAccessToken() : null)

  if (response.status === 401 && auth && authHooks) {
    const renewedToken = await authHooks.refreshAccessToken()
    if (renewedToken) {
      response = await send(path, options, renewedToken)
    }
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export function apiGet<T>(path: string): Promise<T> {
  return apiRequest<T>(path)
}

function send(path: string, { method = 'GET', body }: RequestOptions, accessToken?: string | null) {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  return fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    // La cookie del refresh token solo viaja a /api/v1/auth; 'include' la envía también
    // cuando el frontend se sirve desde otro origen que la API.
    credentials: 'include',
  })
}

async function toApiError(response: Response): Promise<ApiError> {
  let detail = `La API respondió ${response.status}`
  let fieldErrors: FieldError[] = []
  try {
    const problem = (await response.json()) as { detail?: string; errors?: FieldError[] }
    if (problem.detail) {
      detail = problem.detail
    }
    fieldErrors = problem.errors ?? []
  } catch {
    // Respuesta sin cuerpo JSON: se usa el mensaje genérico.
  }
  const retryAfter = Number(response.headers.get('Retry-After'))
  return new ApiError(response.status, detail, fieldErrors, Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : null)
}
