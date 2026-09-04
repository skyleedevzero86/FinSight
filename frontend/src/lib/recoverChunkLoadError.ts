const RELOAD_KEY = "finsight-chunk-load-reloaded"
const COOLDOWN_MS = 20_000

declare global {
  interface Window {
    __finsightChunkLoadRecovery?: boolean
  }
}

function warnDev(message: string, err?: unknown) {
  if (process.env.NODE_ENV !== "development") return
  if (err === undefined) {
    console.warn(message)
    return
  }
  console.warn(message, err)
}

export function isChunkLoadError(reason: unknown): boolean {
  if (reason == null) return false
  if (typeof reason === "string") {
    return /ChunkLoadError|Loading chunk .+ failed/i.test(reason)
  }
  if (typeof reason !== "object") return false
  const err = reason as { name?: unknown; message?: unknown; type?: unknown }
  if (err.name === "ChunkLoadError") return true
  const message = String(err.message ?? "")
  if (/ChunkLoadError|Loading chunk .+ failed/i.test(message)) return true
  // webpack script load failures sometimes surface as Event
  if (err.type === "error" && message.includes("/_next/static/chunks/")) return true
  return false
}

function reloadOnce(): void {
  try {
    const raw = sessionStorage.getItem(RELOAD_KEY)
    const last = raw ? Number(raw) : 0
    if (Number.isFinite(last) && Date.now() - last < COOLDOWN_MS) {
      warnDev("청크 로드 복구를 이미 시도했습니다. 잠시 후 수동 새로고침해 주세요.")
      return
    }
    sessionStorage.setItem(RELOAD_KEY, String(Date.now()))
  } catch {
    return
  }
  warnDev("Next.js 청크 로드에 실패했습니다. 페이지를 새로고침합니다.")
  window.location.reload()
}

export function clearChunkLoadRecoveryFlag(): void {
  if (typeof window === "undefined") return
  try {
    const raw = sessionStorage.getItem(RELOAD_KEY)
    const last = raw ? Number(raw) : 0
    if (!Number.isFinite(last) || Date.now() - last >= COOLDOWN_MS) {
      sessionStorage.removeItem(RELOAD_KEY)
    }
  } catch {
  }
}

export function installChunkLoadRecovery(): void {
  if (typeof window === "undefined") return
  if (window.__finsightChunkLoadRecovery) return
  window.__finsightChunkLoadRecovery = true

  window.addEventListener("unhandledrejection", (event) => {
    if (!isChunkLoadError(event.reason)) return
    event.preventDefault()
    reloadOnce()
  })

  window.addEventListener(
    "error",
    (event) => {
      const target = event.target
      if (target instanceof HTMLScriptElement) {
        const src = target.src || ""
        if (src.includes("/_next/static/chunks/")) {
          reloadOnce()
          return
        }
      }
      if (isChunkLoadError(event.error ?? event.message)) {
        reloadOnce()
      }
    },
    true,
  )
}

/** layout <head>에서 React보다 먼저 설치 — HMR ChunkLoadError 선제 복구 */
export const CHUNK_LOAD_RECOVERY_SCRIPT = `(function () {
  if (typeof window === "undefined") return;
  if (window.__finsightChunkLoadRecovery) return;
  window.__finsightChunkLoadRecovery = true;

  var RELOAD_KEY = "finsight-chunk-load-reloaded";
  var COOLDOWN_MS = 20000;

  function isChunkErr(reason) {
    if (reason == null) return false;
    if (typeof reason === "string") {
      return /ChunkLoadError|Loading chunk .+ failed/i.test(reason);
    }
    if (typeof reason !== "object") return false;
    if (reason.name === "ChunkLoadError") return true;
    var message = String(reason.message || "");
    return /ChunkLoadError|Loading chunk .+ failed/i.test(message);
  }

  function reloadOnce() {
    try {
      var raw = sessionStorage.getItem(RELOAD_KEY);
      var last = raw ? Number(raw) : 0;
      if (isFinite(last) && Date.now() - last < COOLDOWN_MS) return;
      sessionStorage.setItem(RELOAD_KEY, String(Date.now()));
    } catch (e) {
      return;
    }
    try {
      if (typeof console !== "undefined" && console.warn) {
        console.warn("Next.js 청크 로드에 실패했습니다. 페이지를 새로고침합니다.");
      }
    } catch (e) {}
    location.reload();
  }

  window.addEventListener("unhandledrejection", function (event) {
    if (!isChunkErr(event.reason)) return;
    try { event.preventDefault(); } catch (e) {}
    reloadOnce();
  });

  window.addEventListener("error", function (event) {
    var target = event.target;
    if (target && target.tagName === "SCRIPT") {
      var src = target.src || "";
      if (src.indexOf("/_next/static/chunks/") !== -1) {
        reloadOnce();
        return;
      }
    }
    if (isChunkErr(event.error || event.message)) reloadOnce();
  }, true);
})();`
