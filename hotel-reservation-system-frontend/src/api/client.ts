export const TOKEN_KEY = 'hotelmn.token'

export class ApiError extends Error {
  readonly status: number
  /** Código estable del backend (API-CONTRACT §0.2); la lógica del front se decide por él, no por el mensaje. */
  readonly code?: string
  /** Errores por campo cuando code = VALIDATION_ERROR */
  readonly errors?: Record<string, string>

  constructor(status: number, message: string, code?: string, errors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.errors = errors
  }
}

export const getStoredToken = (): string | null => localStorage.getItem(TOKEN_KEY)
export const storeToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token)
export const clearStoredToken = (): void => localStorage.removeItem(TOKEN_KEY)

type UnauthorizedListener = () => void
const unauthorizedListeners = new Set<UnauthorizedListener>()

/** Se invoca cuando una petición autenticada recibe 401 (token vencido o inválido). */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener)
  return () => {
    unauthorizedListeners.delete(listener)
  }
}

interface ErrorBody {
  message?: unknown
  code?: unknown
  errors?: unknown
}

function readError(body: unknown): { message?: string; code?: string; errors?: Record<string, string> } {
  if (typeof body !== 'object' || body === null) return {}
  const { message, code, errors } = body as ErrorBody
  return {
    message: typeof message === 'string' ? message : undefined,
    code: typeof code === 'string' ? code : undefined,
    errors: typeof errors === 'object' && errors !== null ? (errors as Record<string, string>) : undefined,
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(init.headers)
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(path, { ...init, headers })
  const text = await res.text()
  let body: unknown
  try {
    body = text ? JSON.parse(text) : undefined
  } catch {
    body = undefined
  }

  if (!res.ok) {
    if (res.status === 401 && token) {
      clearStoredToken()
      unauthorizedListeners.forEach((listener) => listener())
    }
    const { message, code, errors } = readError(body)
    throw new ApiError(res.status, message ?? `Error ${res.status}`, code, errors)
  }
  return body as T
}

const withBody = (method: string, body?: unknown): RequestInit => ({
  method,
  body: body === undefined ? undefined : JSON.stringify(body),
})

export const apiGet = <T>(path: string) => apiFetch<T>(path)
export const apiPost = <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody('POST', body))
export const apiPut = <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody('PUT', body))
export const apiPatch = <T>(path: string, body?: unknown) => apiFetch<T>(path, withBody('PATCH', body))
export const apiDelete = <T>(path: string) => apiFetch<T>(path, { method: 'DELETE' })
