"use client"

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react"
import { fetchCurrentUser, type AuthUser } from "@/lib/authSession"
import {
  authHeadersJson,
  clearAuthSession,
  FINSIGHT_AUTH_CHANGED_EVENT,
  readUsableAccessToken,
} from "@/lib/finsightToken"

type AuthSessionValue = {
  user: AuthUser | null
  ready: boolean
  hasToken: boolean
  refresh: () => Promise<void>
  logout: () => Promise<void>
}

const AuthSessionContext = createContext<AuthSessionValue | null>(null)

export function useAuthSession(): AuthSessionValue {
  const ctx = useContext(AuthSessionContext)
  if (!ctx) {
    return {
      user: null,
      ready: false,
      hasToken: false,
      refresh: async () => undefined,
      logout: async () => undefined,
    }
  }
  return ctx
}

export default function AuthSessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [ready, setReady] = useState(false)
  const [hasToken, setHasToken] = useState(false)

  const refresh = useCallback(async () => {
    const token = readUsableAccessToken()
    setHasToken(Boolean(token))
    if (!token) {
      setUser(null)
      setReady(true)
      return
    }
    try {
      const next = await fetchCurrentUser()
      setUser(next)
      setHasToken(Boolean(next) || Boolean(readUsableAccessToken()))
    } catch {
      setUser(null)
    } finally {
      setReady(true)
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      if (readUsableAccessToken()) {
        await fetch("/api/v1/auth/logout", {
          method: "POST",
          headers: { ...authHeadersJson(), "Content-Type": "application/json" },
          body: "{}",
        })
      }
    } catch {
      void 0
    }
    clearAuthSession()
    setUser(null)
    setHasToken(false)
  }, [])

  useEffect(() => {
    void refresh()
    const onChange = () => {
      void refresh()
    }
    window.addEventListener(FINSIGHT_AUTH_CHANGED_EVENT, onChange)
    window.addEventListener("storage", onChange)
    return () => {
      window.removeEventListener(FINSIGHT_AUTH_CHANGED_EVENT, onChange)
      window.removeEventListener("storage", onChange)
    }
  }, [refresh])

  const value = useMemo(
    () => ({ user, ready, hasToken, refresh, logout }),
    [user, ready, hasToken, refresh, logout],
  )

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>
}
