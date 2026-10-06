import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

const ACCESS_COOKIE = "accessToken"
const AUTH_HINT_COOKIE = "finsight_auth"

function isProtectedPath(pathname: string): boolean {
  if (pathname === "/myinfo" || pathname.startsWith("/myinfo/")) return true
  if (pathname === "/admin" || pathname.startsWith("/admin/")) return true
  if (/^\/community\/[^/]+\/write\/?$/.test(pathname)) return true
  if (/^\/community\/[^/]+\/[^/]+\/edit\/?$/.test(pathname)) return true
  return false
}

function hasSessionCookie(req: NextRequest): boolean {
  const access = req.cookies.get(ACCESS_COOKIE)?.value
  if (access && access.trim()) return true
  const hint = req.cookies.get(AUTH_HINT_COOKIE)?.value
  return hint === "1"
}

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl
  if (!isProtectedPath(pathname)) {
    return NextResponse.next()
  }
  if (hasSessionCookie(req)) {
    return NextResponse.next()
  }
  const login = new URL("/login", req.url)
  login.searchParams.set("next", `${pathname}${req.nextUrl.search}`)
  return NextResponse.redirect(login)
}

export const config = {
  matcher: ["/myinfo/:path*", "/admin/:path*", "/community/:path*"],
}
