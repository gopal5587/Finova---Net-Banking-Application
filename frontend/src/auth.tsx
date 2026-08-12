import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, getToken, setToken, type UserProfile } from './api'

type AuthState = {
  user: UserProfile | null
  loading: boolean
  loginWithTokens: (access: string) => Promise<void>
  logout: () => void
  refreshProfile: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshProfile = async () => {
    const profile = await api<UserProfile>('/api/v1/auth/me')
    setUser(profile)
  }

  const loginWithTokens = async (access: string) => {
    setToken(access)
    await refreshProfile()
  }

  const logout = () => {
    setToken(null)
    setUser(null)
  }

  useEffect(() => {
    const token = getToken()
    if (!token) {
      setLoading(false)
      return
    }
    refreshProfile()
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, loginWithTokens, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
