import {
  PublicClientApplication,
  type Configuration,
  type AuthenticationResult,
  type PopupRequest,
  LogLevel,
} from '@azure/msal-browser'
import type { DevIdentity, UserProfile, UserRole } from '@/types/auth'
import { removeStoredToken, setStoredToken } from './api'

export const DEV_IDENTITIES: Record<string, DevIdentity> = {
  admin: {
    key: 'admin',
    label: 'Alex Thorne (Admin)',
    name: 'Alex Thorne',
    email: 'alex.thorne@hibana.tk',
    roles: ['ROLE_ADMIN', 'ROLE_OPERATOR'],
    badgeColor: 'bg-primary/20 text-primary border-primary/30',
    roleTitle: 'Lead Platform Administrator',
  },
  operator: {
    key: 'operator',
    label: 'Operador de Guardia',
    name: 'Carlos Méndez',
    email: 'operator@hibana.tk',
    roles: ['ROLE_OPERATOR'],
    badgeColor: 'bg-secondary/20 text-secondary border-secondary/30',
    roleTitle: 'Security & Operations Operator',
  },
  user: {
    key: 'user',
    label: 'Usuario Regular (B2C)',
    name: 'Lucía Rossi',
    email: 'user@hibana.tk',
    roles: ['ROLE_USER'],
    badgeColor: 'bg-muted/20 text-muted-light border-muted/30',
    roleTitle: 'Regular Buyer (No Admin Access)',
  },
}

const PROFILE_KEY = 'hibana_admin_profile'
const ACTIVE_IDENTITY_KEY = 'hibana_active_identity'

const tenantId = import.meta.env.VITE_ENTRA_TENANT_ID || '00000000-0000-0000-0000-000000000002'
const clientId = import.meta.env.VITE_ENTRA_CLIENT_ID || 'hibana-admin-portal'
const redirectUri = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:5174'
const authority =
  import.meta.env.VITE_ENTRA_AUTHORITY ||
  (import.meta.env.PROD
    ? `https://login.microsoftonline.com/${tenantId}`
    : `http://localhost:4577/${tenantId}/v2.0`)

export const msalConfig: Configuration = {
  auth: {
    clientId,
    authority,
    redirectUri,
    knownAuthorities: ['localhost:4577', 'login.microsoftonline.com'],
  },
  cache: {
    cacheLocation: 'sessionStorage',
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message, containsPii) => {
        if (!containsPii) {
          if (level === LogLevel.Error) console.error('[MSAL]', message)
          else if (level === LogLevel.Warning) console.warn('[MSAL]', message)
        }
      },
      logLevel: LogLevel.Warning,
    },
  },
}

let msalInstance: PublicClientApplication | null = null

export async function getMsalInstance(): Promise<PublicClientApplication> {
  if (!msalInstance) {
    msalInstance = new PublicClientApplication(msalConfig)
    await msalInstance.initialize()
  }
  return msalInstance
}

export const loginRequest: PopupRequest = {
  scopes: ['openid', 'profile', 'email'],
}

export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    return null
  }
}

function extractUserProfile(authResult: AuthenticationResult, token: string): UserProfile {
  const claims = (authResult.idTokenClaims as Record<string, unknown>) || decodeJwtPayload(token) || {}
  const rawRoles = (claims.roles as string[]) || (claims.groups as string[]) || []
  const roles: UserRole[] = rawRoles.map((r) => {
    const clean = r.startsWith('ROLE_') ? r : `ROLE_${r.toUpperCase()}`
    return clean as UserRole
  })

  return {
    id: (claims.sub as string) || (claims.oid as string) || authResult.uniqueId || 'unknown-user',
    email:
      (claims.preferred_username as string) ||
      (claims.email as string) ||
      authResult.account?.username ||
      'admin@hibana.tk',
    name: (claims.name as string) || authResult.account?.name || 'Administrator',
    roles: roles.length > 0 ? roles : ['ROLE_ADMIN'],
    avatarUrl: `https://api.dicebear.com/7.x/bottts/svg?seed=${authResult.account?.username || 'admin'}`,
  }
}

/**
 * Standard Microsoft Entra ID Authorization Code Flow with PKCE.
 * Replaces insecure legacy ROPC and avoids any static credentials.
 */
export async function loginWithPkce(loginHint?: string): Promise<{ token: string; profile: UserProfile }> {
  const msal = await getMsalInstance()
  const request: PopupRequest = {
    ...loginRequest,
    loginHint: loginHint || undefined,
  }

  let authResult: AuthenticationResult
  try {
    authResult = await msal.loginPopup(request)
  } catch (popupErr) {
    console.warn('[MSAL-PKCE] Popup login failed or was blocked, trying silent fallback:', popupErr)
    const accounts = msal.getAllAccounts()
    if (accounts.length > 0) {
      authResult = await msal.acquireTokenSilent({
        ...request,
        account: accounts[0],
      })
    } else {
      throw popupErr
    }
  }

  const token = authResult.accessToken || authResult.idToken
  const profile = extractUserProfile(authResult, token)
  setStoredToken(token)
  setStoredProfile(profile)
  return { token, profile }
}

/**
 * Handles redirect response when using Authorization Code Flow with PKCE via redirect.
 */
export async function handleMsalRedirect(): Promise<{ token: string; profile: UserProfile } | null> {
  const msal = await getMsalInstance()
  const result = await msal.handleRedirectPromise()
  if (result) {
    const token = result.accessToken || result.idToken
    const profile = extractUserProfile(result, token)
    setStoredToken(token)
    setStoredProfile(profile)
    return { token, profile }
  }
  return null
}

/**
 * Login flow for dev identity switching.
 * Eliminates all static passwords (dev-pass-123) and ROPC grants.
 * In production builds, this strictly delegates to loginWithPkce.
 */
export async function loginWithDevIdentity(
  identityKey: 'admin' | 'operator' | 'user'
): Promise<{ token: string; profile: UserProfile }> {
  const identity = DEV_IDENTITIES[identityKey]
  if (!identity) throw new Error(`Unknown identity: ${identityKey}`)

  // In production builds, always enforce genuine MSAL PKCE login
  if (import.meta.env.PROD) {
    return loginWithPkce(identity.email)
  }

  // In development, attempt MSAL PKCE silent account lookup
  try {
    const msal = await getMsalInstance()
    const accounts = msal.getAllAccounts()
    const matchedAccount = accounts.find((a) => a.username?.toLowerCase() === identity.email.toLowerCase())
    if (matchedAccount) {
      const silentRes = await msal.acquireTokenSilent({
        ...loginRequest,
        account: matchedAccount,
      })
      const token = silentRes.accessToken || silentRes.idToken
      const profile = extractUserProfile(silentRes, token)
      setStoredToken(token)
      setStoredProfile(profile)
      if (typeof sessionStorage !== 'undefined') {
        sessionStorage.setItem(ACTIVE_IDENTITY_KEY, identityKey)
      }
      return { token, profile }
    }
  } catch (msalErr) {
    console.debug('[MSAL-PKCE] Local silent account check skipped:', msalErr)
  }

  // In non-production preview when interactive MSAL popup is bypassed,
  // generate standard RS256 token representation without hardcoded passwords or ROPC.
  const token = createLocalDevToken(identity)
  const claims = decodeJwtPayload(token)
  const roles: UserRole[] =
    (claims?.roles as UserRole[]) ||
    (claims?.groups as UserRole[]) ||
    identity.roles

  const profile: UserProfile = {
    id: (claims?.sub as string) || `usr-${identity.key}`,
    email: (claims?.preferred_username as string) || (claims?.email as string) || identity.email,
    name: (claims?.name as string) || identity.name,
    roles,
    avatarUrl: `https://api.dicebear.com/7.x/bottts/svg?seed=${identity.email}`,
  }

  setStoredToken(token)
  setStoredProfile(profile)
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem(ACTIVE_IDENTITY_KEY, identityKey)
  }

  return { token, profile }
}

export function getStoredProfile(): UserProfile | null {
  if (typeof sessionStorage === 'undefined') return null
  const raw = sessionStorage.getItem(PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

export function setStoredProfile(profile: UserProfile): void {
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
  }
}

export function getStoredIdentityKey(): 'admin' | 'operator' | 'user' {
  if (typeof sessionStorage === 'undefined') return 'admin'
  return (sessionStorage.getItem(ACTIVE_IDENTITY_KEY) as 'admin' | 'operator' | 'user') || 'admin'
}

export function logout(): void {
  removeStoredToken()
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.removeItem(PROFILE_KEY)
    sessionStorage.removeItem(ACTIVE_IDENTITY_KEY)
  }
  if (msalInstance) {
    try {
      msalInstance.setActiveAccount(null)
    } catch {
      // ignore
    }
  }
}

function createLocalDevToken(identity: DevIdentity): string {
  const header = btoa(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid: 'floci-local-key' }))
  const payload = btoa(
    JSON.stringify({
      sub: identity.key === 'admin' ? '00000000-0000-0000-0000-000000000001' : `usr-${identity.key}-id`,
      preferred_username: identity.email,
      name: identity.name,
      roles: identity.roles,
      groups: identity.roles,
      iss: `http://localhost:4577/${tenantId}/v2.0`,
      aud: clientId,
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
  )
  return `${header}.${payload}.simulated_signature`
}
