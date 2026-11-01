import { createContext, useContext, useEffect, useState } from 'react'
import http, { errorMessage, setTokenProvider } from '../api/http-common'
import { AUTH_MODE, getAccessToken, initKeycloak, keycloakLogout } from './keycloak'

const AuthContext = createContext(null)
const STORAGE_KEY = 'siga-user'
const isKeycloak = AUTH_MODE === 'keycloak'

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

/**
 * Exposes { user, loading, error, mode, login, logout } where user = { role, userId, name }.
 *  - keycloak mode: Keycloak login, then GET /api/me tells the role and the linked student/teacher.
 *  - dev mode: simulated login (the user picks a role); the backend must run with the dev profile.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (isKeycloak ? null : readStoredUser()))
  const [loading, setLoading] = useState(isKeycloak)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!isKeycloak) {
      setTokenProvider(async () => null)
      return undefined
    }
    let cancelled = false
    ;(async () => {
      try {
        await initKeycloak() // redirects to Keycloak when there is no session
        setTokenProvider(getAccessToken)
        const { data } = await http.get('/api/me')
        if (!cancelled) setUser(data)
      } catch (e) {
        if (!cancelled) setError(errorMessage(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  // Only used by the simulated login (dev mode)
  const login = (data) => {
    setUser(data)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }

  const logout = () => {
    if (isKeycloak) {
      keycloakLogout()
      return
    }
    setUser(null)
    localStorage.removeItem(STORAGE_KEY)
  }

  return (
      <AuthContext.Provider value={{ user, loading, error, mode: AUTH_MODE, login, logout }}>
        {children}
      </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)