const STALE_SW_PATHS = ["/sw.js", "/service-worker.js", "/serviceworker.js"]

const STALE_SW_SCRIPT_HINTS = [/workbox/i, /serwist/i, /next-pwa/i]

const STALE_CACHE_HINTS = [/^workbox/i, /serwist/i, /next-pwa/i, /^ngsw/i]

function warnDev(message: string, err?: unknown) {
  if (process.env.NODE_ENV !== "development") return
  if (err === undefined) {
    console.warn(message)
    return
  }
  console.warn(message, err)
}

function sameOrigin(url: string): boolean {
  try {
    return new URL(url, window.location.href).origin === window.location.origin
  } catch {
    return false
  }
}

function scriptPathname(scriptURL: string): string {
  try {
    return new URL(scriptURL, window.location.href).pathname
  } catch {
    return ""
  }
}

function isStaleServiceWorker(registration: ServiceWorkerRegistration): boolean {
  if (!sameOrigin(registration.scope)) return false

  const scriptURL =
    registration.active?.scriptURL ||
    registration.waiting?.scriptURL ||
    registration.installing?.scriptURL ||
    ""
  if (!scriptURL || !sameOrigin(scriptURL)) return false

  const pathname = scriptPathname(scriptURL)
  if (STALE_SW_PATHS.some((path) => pathname === path || pathname.endsWith(path))) {
    return true
  }
  return STALE_SW_SCRIPT_HINTS.some((hint) => hint.test(scriptURL))
}

function isStaleCacheKey(key: string): boolean {
  return STALE_CACHE_HINTS.some((hint) => hint.test(key))
}

async function unregisterStaleRegistrations(
  registrations: readonly ServiceWorkerRegistration[],
): Promise<void> {
  for (const registration of registrations) {
    if (!isStaleServiceWorker(registration)) continue
    try {
      await registration.unregister()
    } catch (err) {
      warnDev(
        `오래된 서비스 워커 해제에 실패했습니다. scope=${registration.scope}`,
        err,
      )
    }
  }
}

async function deleteStaleCaches(): Promise<void> {
  if (!("caches" in window)) return
  let keys: string[]
  try {
    keys = await caches.keys()
  } catch (err) {
    warnDev("서비스 워커 캐시 목록을 가져오지 못했습니다. 페이지는 그대로 진행합니다.", err)
    return
  }

  for (const key of keys) {
    if (!isStaleCacheKey(key)) continue
    try {
      await caches.delete(key)
    } catch (err) {
      warnDev(`서비스 워커 캐시 삭제에 실패했습니다. key=${key}`, err)
    }
  }
}

async function runCleanup(): Promise<void> {
  let registrations: readonly ServiceWorkerRegistration[] = []
  try {
    registrations = await navigator.serviceWorker.getRegistrations()
  } catch (err) {
    warnDev("오래된 서비스 워커 목록을 가져오지 못했습니다. 페이지는 그대로 진행합니다.", err)
    return
  }

  await unregisterStaleRegistrations(registrations)
  await deleteStaleCaches()
}

export function cleanupStaleServiceWorkers(): void {
  if (typeof window === "undefined") return
  if (!("serviceWorker" in navigator)) return
  if (typeof navigator.serviceWorker.getRegistrations !== "function") return

  void runCleanup().catch((err) => {
    warnDev("서비스 워커 정리 중 오류가 발생했습니다. 페이지는 그대로 진행합니다.", err)
  })
}
