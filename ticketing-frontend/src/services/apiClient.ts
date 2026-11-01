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

export async function request<T>(
  url: string,
  options: RequestInit = {},
  timeoutMs = 6000
): Promise<T> {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  const traceId = generateTraceId()

  const storedToken =
    typeof sessionStorage !== 'undefined'
      ? sessionStorage.getItem('waiting_room_checkout_token')
      : null

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Trace-ID': traceId,
    'X-Trace-Id': traceId,
    ...(storedToken ? { 'X-Checkout-Token': storedToken } : {}),
    ...(options.headers as Record<string, string>),
  }

  try {
    const response = await fetch(url, {
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
