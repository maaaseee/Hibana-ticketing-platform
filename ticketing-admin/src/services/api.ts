export interface ApiProblemDetails {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
}

export class ApiError extends Error {
  status: number
  problem?: ApiProblemDetails

  constructor(status: number, message: string, problem?: ApiProblemDetails) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

const TOKEN_KEY = 'hibana_admin_token'

let inMemoryToken: string | null = null

export function getStoredToken(): string | null {
  if (inMemoryToken) return inMemoryToken
  if (typeof sessionStorage !== 'undefined') {
    return sessionStorage.getItem(TOKEN_KEY)
  }
  return null
}

export function setStoredToken(token: string): void {
  inMemoryToken = token
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

export function removeStoredToken(): void {
  inMemoryToken = null
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

export async function apiClient<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(options.headers || {})

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const url = endpoint.startsWith('/') ? endpoint : `/api/v1/${endpoint}`

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let problem: ApiProblemDetails | undefined
    let errorMessage = `HTTP error ${response.status}: ${response.statusText}`

    try {
      const errorJson = await response.json()
      problem = errorJson
      errorMessage = errorJson.detail || errorJson.title || errorMessage
    } catch {
      // not JSON
    }

    if (response.status === 401) {
      removeStoredToken()
      // dispatch unauthorized event or redirect
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }

    throw new ApiError(response.status, errorMessage, problem)
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return {} as T
  }

  const contentType = response.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T
  }

  return (await response.text()) as unknown as T
}
