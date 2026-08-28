const SAFE_SW_PATH = "/finsight-sw.js"

const STALE_SW_PATHS = [
  "/sw.js",
  "/service-worker.js",
  "/serviceworker.js",
  "/firebase-messaging-sw.js",
]

const STALE_SW_SCRIPT_HINTS = [/workbox/i, /serwist/i, /next-pwa/i, /ngsw/i]

const STALE_CACHE_HINTS = [/^workbox/i, /serwist/i, /next-pwa/i, /^ngsw/i, /^firebase/i]

const RELOAD_FLAG = "finsight-sw-cleanup-reloaded"

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

function isLocalDevHost(): boolean {
  const host = window.location.hostname
  return host === "localhost" || host === "127.0.0.1" || host === "[::1]"
}

function registrationScriptURL(registration: ServiceWorkerRegistration): string {
  return (
    registration.active?.scriptURL ||
    registration.waiting?.scriptURL ||
    registration.installing?.scriptURL ||
    ""
  )
}

function isSafeServiceWorker(registration: ServiceWorkerRegistration): boolean {
  const scriptURL = registrationScriptURL(registration)
  if (!scriptURL || !sameOrigin(scriptURL)) return false
  return scriptPathname(scriptURL) === SAFE_SW_PATH
}

function isStaleServiceWorker(registration: ServiceWorkerRegistration): boolean {
  if (!sameOrigin(registration.scope)) return false
  if (isSafeServiceWorker(registration)) return false

  const scriptURL = registrationScriptURL(registration)
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

async function disableNavigationPreload(
  registration: ServiceWorkerRegistration,
): Promise<void> {
  try {
    const nav = registration.navigationPreload
    if (!nav) return
    if (typeof nav.disable === "function") {
      await nav.disable()
    }
  } catch {
  }
}

async function ensureSafeServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  try {
    const registration = await navigator.serviceWorker.register(SAFE_SW_PATH, {
      scope: "/",
      updateViaCache: "none",
    })
    await disableNavigationPreload(registration)
    try {
      await registration.update()
    } catch {
    }
    return registration
  } catch (err) {
    warnDev("안전 서비스 워커 등록에 실패했습니다.", err)
    return null
  }
}

async function unregisterForeignRegistrations(
  registrations: readonly ServiceWorkerRegistration[],
): Promise<boolean> {
  let removed = false
  const removeAllForeign =
    process.env.NODE_ENV === "development" || isLocalDevHost()

  for (const registration of registrations) {
    if (!sameOrigin(registration.scope)) continue
    await disableNavigationPreload(registration)

    if (isSafeServiceWorker(registration)) continue

    const shouldRemove = removeAllForeign || isStaleServiceWorker(registration)
    if (!shouldRemove) continue

    try {
      const ok = await registration.unregister()
      if (ok) removed = true
    } catch (err) {
      warnDev(
        `오래된 서비스 워커 해제에 실패했습니다. scope=${registration.scope}`,
        err,
      )
    }
  }

  return removed
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

  const shouldRemoveAll =
    process.env.NODE_ENV === "development" || isLocalDevHost()

  for (const key of keys) {
    const remove = shouldRemoveAll || isStaleCacheKey(key)
    if (!remove) continue
    try {
      await caches.delete(key)
    } catch (err) {
      warnDev(`서비스 워커 캐시 삭제에 실패했습니다. key=${key}`, err)
    }
  }
}

function controllerIsSafe(): boolean {
  const controller = navigator.serviceWorker.controller
  if (!controller) return true
  try {
    return scriptPathname(controller.scriptURL) === SAFE_SW_PATH
  } catch {
    return false
  }
}

function reloadOnceIfNeeded(shouldReload: boolean): void {
  if (!shouldReload) return
  if (!(process.env.NODE_ENV === "development" || isLocalDevHost())) return
  try {
    if (sessionStorage.getItem(RELOAD_FLAG) === "1") return
    sessionStorage.setItem(RELOAD_FLAG, "1")
  } catch {
    return
  }
  window.location.reload()
}

async function runCleanup(): Promise<void> {
  let registrations: readonly ServiceWorkerRegistration[] = []
  try {
    registrations = await navigator.serviceWorker.getRegistrations()
  } catch (err) {
    warnDev("오래된 서비스 워커 목록을 가져오지 못했습니다. 페이지는 그대로 진행합니다.", err)
    return
  }

  const hadForeignController =
    Boolean(navigator.serviceWorker.controller) && !controllerIsSafe()
  const hadAnyRegistration = registrations.length > 0 || hadForeignController

  for (const registration of registrations) {
    await disableNavigationPreload(registration)
  }

  const removed = await unregisterForeignRegistrations(registrations)
  await deleteStaleCaches()

  if (hadAnyRegistration || process.env.NODE_ENV === "development" || isLocalDevHost()) {
    await ensureSafeServiceWorker()
  }

  if (controllerIsSafe() || !navigator.serviceWorker.controller) {
    try {
      sessionStorage.removeItem(RELOAD_FLAG)
    } catch {
    }
  }

  reloadOnceIfNeeded(hadForeignController || removed)
}

export function cleanupStaleServiceWorkers(): void {
  if (typeof window === "undefined") return
  if (!("serviceWorker" in navigator)) return
  if (typeof navigator.serviceWorker.getRegistrations !== "function") return

  void runCleanup().catch((err) => {
    warnDev("서비스 워커 정리 중 오류가 발생했습니다. 페이지는 그대로 진행합니다.", err)
  })
}

export const STALE_SERVICE_WORKER_CLEANUP_SCRIPT = `(function () {
  if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return;
  if (typeof navigator.serviceWorker.getRegistrations !== "function") return;

  var SAFE_SW_PATH = "/finsight-sw.js";
  var host = location.hostname;
  var isLocal = host === "localhost" || host === "127.0.0.1" || host === "[::1]";
  var reloadFlag = "finsight-sw-cleanup-reloaded";

  function pathnameOf(url) {
    try { return new URL(url, location.href).pathname; } catch (e) { return ""; }
  }

  function scriptURL(reg) {
    return (reg.active && reg.active.scriptURL)
      || (reg.waiting && reg.waiting.scriptURL)
      || (reg.installing && reg.installing.scriptURL)
      || "";
  }

  function isSafe(reg) {
    return pathnameOf(scriptURL(reg)) === SAFE_SW_PATH;
  }

  function controllerSafe() {
    if (!navigator.serviceWorker.controller) return true;
    return pathnameOf(navigator.serviceWorker.controller.scriptURL) === SAFE_SW_PATH;
  }

  function disablePreload(reg) {
    try {
      if (reg.navigationPreload && typeof reg.navigationPreload.disable === "function") {
        return reg.navigationPreload.disable().catch(function () {});
      }
    } catch (e) {}
    return Promise.resolve();
  }

  navigator.serviceWorker.getRegistrations().then(function (regs) {
    regs = regs || [];
    var hadForeign = !!navigator.serviceWorker.controller && !controllerSafe();
    var hadAny = regs.length > 0 || !!navigator.serviceWorker.controller;
    var tasks = regs.map(function (reg) {
      return disablePreload(reg).then(function () {
        if (isSafe(reg)) return;
        if (!isLocal) {
          var path = pathnameOf(scriptURL(reg));
          var stale = path === "/sw.js"
            || path === "/service-worker.js"
            || path === "/serviceworker.js"
            || /workbox|serwist|next-pwa|ngsw/i.test(scriptURL(reg));
          if (!stale) return;
        }
        try { return reg.unregister(); } catch (e) {}
      });
    });

    return Promise.all(tasks).then(function () {
      if (!hadAny && !isLocal) return null;
      return navigator.serviceWorker.register(SAFE_SW_PATH, {
        scope: "/",
        updateViaCache: "none"
      }).then(function (reg) {
        return disablePreload(reg).then(function () { return reg; });
      }).catch(function () { return null; });
    }).then(function () {
      if (!isLocal) return;
      if (!hadForeign && controllerSafe()) {
        try { sessionStorage.removeItem(reloadFlag); } catch (e) {}
        return;
      }
      if (!hadForeign && !hadAny) {
        try { sessionStorage.removeItem(reloadFlag); } catch (e) {}
        return;
      }
      try {
        if (sessionStorage.getItem(reloadFlag) === "1") return;
        sessionStorage.setItem(reloadFlag, "1");
        location.reload();
      } catch (e) {}
    });
  }).catch(function () {});
})();`
