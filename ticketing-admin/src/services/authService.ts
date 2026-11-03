import type { DevIdentity, TokenResponse, UserProfile, UserRole } from '@/types/auth'
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

export async function loginWithDevIdentity(identityKey: 'admin' | 'operator' | 'user'): Promise<{ token: string; profile: UserProfile }> {
  const identity = DEV_IDENTITIES[identityKey]
  if (!identity) throw new Error(`Unknown identity: ${identityKey}`)

  const tenantId = import.meta.env.VITE_ENTRA_TENANT_ID || '00000000-0000-0000-0000-000000000002'
  const tokenEndpoint = `/floci-entra/${tenantId}/oauth2/v2.0/token`

  const bodyParams = new URLSearchParams()
  bodyParams.append('client_id', 'hibana-admin-portal')
  bodyParams.append('grant_type', 'password')
  bodyParams.append('username', identity.email)
  bodyParams.append('password', 'dev-pass-123')
  bodyParams.append('scope', 'openid profile email')

  let token = ''
  try {
    const response = await fetch(tokenEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: bodyParams.toString(),
    })

    if (response.ok) {
      const data: TokenResponse = await response.json()
      token = data.access_token
    } else {
      console.warn(`Floci-AZ ROPC responded with ${response.status}. Using simulated identity token for local preview.`)
      token = createMockJwt(identity)
    }
  } catch (err) {
    console.warn('Could not contact Floci-AZ directly. Using simulated identity token:', err)
    token = createMockJwt(identity)
  }

  // Parse claims or fall back to preset
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
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
  localStorage.setItem(ACTIVE_IDENTITY_KEY, identityKey)

  return { token, profile }
}

export function getStoredProfile(): UserProfile | null {
  const raw = localStorage.getItem(PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

export function getStoredIdentityKey(): 'admin' | 'operator' | 'user' {
  return (localStorage.getItem(ACTIVE_IDENTITY_KEY) as 'admin' | 'operator' | 'user') || 'admin'
}

export function logout(): void {
  removeStoredToken()
  localStorage.removeItem(PROFILE_KEY)
  localStorage.removeItem(ACTIVE_IDENTITY_KEY)
}

function createMockJwt(identity: DevIdentity): string {
  const header = btoa(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid: 'floci-local-key' }))
  const payload = btoa(
    JSON.stringify({
      sub: identity.key === 'admin' ? '00000000-0000-0000-0000-000000000001' : `usr-${identity.key}-id`,
      preferred_username: identity.email,
      name: identity.name,
      roles: identity.roles,
      groups: identity.roles,
      iss: 'http://localhost:4577/00000000-0000-0000-0000-000000000002/v2.0',
      aud: 'hibana-admin-portal',
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
  )
  return `${header}.${payload}.simulated_signature`
}
