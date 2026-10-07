import { readFile } from "node:fs/promises"
import path from "node:path"
import { publicNewsImageUrl } from "@/lib/newsImage"

const MAX_BYTES = 5_000_000
const MAX_HOPS = 3

export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("url") ?? ""
  const target = publicNewsImageUrl(raw)
  if (!target) return logoResponse()
  const image = await loadRemoteImage(target, 0)
  return image ?? logoResponse()
}

async function loadRemoteImage(target: URL, hop: number): Promise<Response | null> {
  if (hop >= MAX_HOPS) return null
  let upstream: Response
  try {
    upstream = await fetch(target, {
      redirect: "manual",
      headers: { Accept: "image/avif,image/webp,image/*,*/*;q=0.8" },
    })
  } catch {
    console.warn("뉴스 이미지 연결 실패", target.hostname)
    return null
  }
  if (upstream.status >= 300 && upstream.status < 400) {
    return followImageRedirect(target, upstream, hop)
  }
  if (!upstream.ok) {
    console.warn("뉴스 이미지 응답 실패", target.hostname, upstream.status)
    return null
  }
  const type = (upstream.headers.get("content-type") ?? "").split(";")[0].trim()
  if (!type.startsWith("image/")) return null
  const bytes = await upstream.arrayBuffer()
  if (bytes.byteLength === 0 || bytes.byteLength > MAX_BYTES) return null
  return new Response(bytes, {
    headers: {
      "Content-Type": type,
      "Cache-Control": "public, max-age=86400",
    },
  })
}

function followImageRedirect(current: URL, upstream: Response, hop: number): Promise<Response | null> {
  const location = upstream.headers.get("location")
  if (!location) return Promise.resolve(null)
  const next = publicNewsImageUrl(new URL(location, current).toString())
  if (!next) return Promise.resolve(null)
  return loadRemoteImage(next, hop + 1)
}

async function logoResponse(): Promise<Response> {
  const file = await readFile(path.join(process.cwd(), "public", "finsight-logo.png"))
  return new Response(file, {
    headers: {
      "Content-Type": "image/png",
      "Cache-Control": "public, max-age=300",
    },
  })
}
