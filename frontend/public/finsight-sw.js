self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting())
})

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      try {
        if (self.registration.navigationPreload) {
          await self.registration.navigationPreload.disable()
        }
      } catch (_) {
      }
      await self.clients.claim()
    })(),
  )
})

self.addEventListener("fetch", (event) => {
  if (event.request.mode !== "navigate") {
    return
  }

  const preloadResponsePromise = event.preloadResponse
  event.respondWith(
    (async () => {
      try {
        const preloadResponse = await preloadResponsePromise
        if (preloadResponse) {
          return preloadResponse
        }
      } catch (_) {
      }
      return fetch(event.request)
    })(),
  )
})
