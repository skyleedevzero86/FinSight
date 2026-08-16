export const NEXT_HMR_BFCACHE_GUARD_FLAG = "__finsightNextHmrBfcacheGuard"

declare global {
  interface Window {
    __finsightNextHmrBfcacheGuard?: boolean
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

export const NEXT_HMR_BFCACHE_GUARD_SCRIPT = `(function () {
  if (typeof window === "undefined" || typeof WebSocket === "undefined") return;
  if (window.__finsightNextHmrBfcacheGuard) return;
  window.__finsightNextHmrBfcacheGuard = true;

  var Original = window.WebSocket;
  var hmrSockets = new Set();

  function warnHmr(message, err) {
    if (typeof console === "undefined" || typeof console.warn !== "function") return;
    if (err === undefined) {
      console.warn(message);
      return;
    }
    console.warn(message, err);
  }

  function isNextHmr(url) {
    try {
      return String(url).indexOf("/_next/webpack-hmr") !== -1;
    } catch (err) {
      warnHmr("HMR 웹소켓 URL을 확인하는 중 오류가 발생했습니다. 해당 소켓은 HMR로 처리하지 않습니다.", err);
      return false;
    }
  }

  function closeHmrSockets() {
    hmrSockets.forEach(function (ws) {
      try {
        if (ws.readyState === Original.OPEN || ws.readyState === Original.CONNECTING) {
          ws.close();
        }
      } catch (err) {
        warnHmr("Next.js HMR 웹소켓을 닫는 중 오류가 발생했습니다. BFCache 정리는 계속 진행합니다.", err);
      }
    });
    try {
      hmrSockets.clear();
    } catch (err) {
      warnHmr("HMR 웹소켓 목록을 비우는 중 오류가 발생했습니다.", err);
    }
  }

  class GuardedWebSocket extends Original {
    constructor(url, protocols) {
      if (protocols === undefined) {
        super(url);
      } else {
        super(url, protocols);
      }
      if (isNextHmr(url)) {
        var self = this;
        hmrSockets.add(self);
        self.addEventListener("close", function () {
          hmrSockets.delete(self);
        });
      }
    }
  }

  window.WebSocket = GuardedWebSocket;
  window.addEventListener("pagehide", closeHmrSockets);
  document.addEventListener("freeze", closeHmrSockets);
})();`

function isNextHmrUrl(url: string | URL): boolean {
  try {
    return String(url).includes("/_next/webpack-hmr")
  } catch (err) {
    warnDev(
      "HMR 웹소켓 URL을 확인하는 중 오류가 발생했습니다. 해당 소켓은 HMR로 처리하지 않습니다.",
      err,
    )
    return false
  }
}

export function installNextHmrBfcacheGuard(): void {
  if (typeof window === "undefined" || typeof WebSocket === "undefined") return
  if (process.env.NODE_ENV !== "development") return
  if (window.__finsightNextHmrBfcacheGuard) return

  window.__finsightNextHmrBfcacheGuard = true

  const Original = window.WebSocket
  const hmrSockets = new Set<WebSocket>()

  function closeHmrSockets() {
    hmrSockets.forEach((ws) => {
      try {
        if (ws.readyState === Original.OPEN || ws.readyState === Original.CONNECTING) {
          ws.close()
        }
      } catch (err) {
        warnDev(
          "Next.js HMR 웹소켓을 닫는 중 오류가 발생했습니다. BFCache 정리는 계속 진행합니다.",
          err,
        )
      }
    })
    try {
      hmrSockets.clear()
    } catch (err) {
      warnDev("HMR 웹소켓 목록을 비우는 중 오류가 발생했습니다.", err)
    }
  }

  class GuardedWebSocket extends Original {
    constructor(url: string | URL, protocols?: string | string[]) {
      if (protocols === undefined) {
        super(url)
      } else {
        super(url, protocols)
      }
      if (isNextHmrUrl(url)) {
        hmrSockets.add(this)
        this.addEventListener("close", () => {
          hmrSockets.delete(this)
        })
      }
    }
  }

  window.WebSocket = GuardedWebSocket
  window.addEventListener("pagehide", closeHmrSockets)
  document.addEventListener("freeze", closeHmrSockets)
}
