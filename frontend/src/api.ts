const TOKEN_KEY = 'finova_access_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  details: string[]

  constructor(status: number, message: string, details: string[] = []) {
    super(message)
    this.status = status
    this.details = details
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(path, { ...options, headers })
  if (!res.ok) {
    let message = res.statusText
    let details: string[] = []
    try {
      const body = await res.json()
      message = body.message ?? message
      details = body.details ?? []
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, message, details)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export type LoginResponse = {
  mfaRequired: boolean
  mfaToken: string | null
  accessToken: string | null
  refreshToken: string | null
  tokenType: string | null
}

export type UserProfile = {
  id: string
  username: string
  email: string
  fullName: string
  role: string
  mfaEnabled: boolean
}

export type Account = {
  id: string
  maskedAccountNumber: string
  accountType: string
  currency: string
  balance: number
  status: string
  createdAt: string
}

export type Transaction = {
  id: string
  reference: string
  type: string
  status: string
  fromAccountId: string | null
  toAccountId: string | null
  amount: number
  currency: string
  description: string | null
  createdAt: string
}

export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}
