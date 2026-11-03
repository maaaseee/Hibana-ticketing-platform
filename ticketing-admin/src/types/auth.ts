export type UserRole = 'ROLE_ADMIN' | 'ROLE_OPERATOR' | 'ROLE_USER'

export interface UserProfile {
  id: string
  email: string
  name: string
  roles: UserRole[]
  avatarUrl?: string
}

export interface DevIdentity {
  key: 'admin' | 'operator' | 'user'
  label: string
  name: string
  email: string
  roles: UserRole[]
  badgeColor: string
  roleTitle: string
  avatarUrl?: string
}

export interface TokenResponse {
  access_token: string
  token_type: string
  expires_in: number
  scope?: string
  id_token?: string
}
