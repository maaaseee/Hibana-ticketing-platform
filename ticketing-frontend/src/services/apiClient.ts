import type { ProblemDetail } from '../types'

export class ApiError extends Error {
  public status: number
  public problem?: ProblemDetail

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

function generateTraceId(): string {
  const timestamp = Date.now().toString(16)
  const random = Math.random().toString(16).substring(2, 10)
  return `trc-${timestamp}-${random}`
}

export function getCurrentUserId(): string {
  if (typeof sessionStorage !== 'undefined') {
    const existing =
      sessionStorage.getItem('waiting_room_user_id') ||
      sessionStorage.getItem('hibana_user_id')
    if (existing) return existing

    const generated = `usr-${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 7)}`
    sessionStorage.setItem('waiting_room_user_id', generated)
    return generated
  }
  return 'frontend-buyer'
}

export async function request<T>(
  url: string,
  options: RequestInit = {},
  timeoutMs = 6000
): Promise<T> {
  const resolvedUrl =
    typeof window !== 'undefined' && window.location?.origin && window.location.origin !== 'null'
      ? `${window.location.origin}${url}`
      : url.startsWith('/')
        ? `http://localhost${url}`
        : url

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  const traceId = generateTraceId()

  const storedToken =
    typeof sessionStorage !== 'undefined'
      ? sessionStorage.getItem('waiting_room_checkout_token')
      : null

  const currentUserId = getCurrentUserId()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Trace-ID': traceId,
    'X-Trace-Id': traceId,
    'X-Client-User-Id': currentUserId,
    ...(storedToken ? { 'X-Checkout-Token': storedToken } : {}),
    ...(options.headers as Record<string, string>),
  }

  try {
    const response = await fetch(resolvedUrl, {
      ...options,
      headers,
      signal: controller.signal,
    })

    if (!response.ok) {
      let problem: ProblemDetail | undefined
      let errorMessage = `HTTP Error ${response.status}: ${response.statusText}`
      try {
        const text = await response.text()
        if (text) {
          problem = JSON.parse(text)
          if (problem?.detail) {
            errorMessage = problem.detail
          } else if (problem?.title) {
            errorMessage = problem.title
          }
        }
      } catch {
        // Body was not JSON
      }
      throw new ApiError(response.status, errorMessage, problem)
    }

    if (response.status === 204) {
      return {} as T
    }

    return (await response.json()) as T
  } catch (err: unknown) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new ApiError(408, 'La solicitud al servidor excedió el tiempo límite.')
    }
    throw err
  } finally {
    clearTimeout(timeoutId)
  }
}

export const apiClient = {
  get: <T>(url: string, options?: RequestInit) => request<T>(url, { ...options, method: 'GET' }),
  post: <T>(url: string, body?: unknown, options?: RequestInit) =>
    request<T>(url, {
      ...options,
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
  delete: <T>(url: string, options?: RequestInit) =>
    request<T>(url, { ...options, method: 'DELETE' }),
}
